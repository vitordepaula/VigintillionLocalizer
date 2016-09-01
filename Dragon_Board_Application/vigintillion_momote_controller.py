#!/usr/local/bin/python

import sys
from pymongo import MongoClient
from uuid import getnode as get_mac
from geojson import Point
import json
import geocoder
import socket
import SimpleHTTPServer
import SocketServer
from SocketServer import ThreadingMixIn
from SimpleHTTPServer import SimpleHTTPRequestHandler
from BaseHTTPServer import HTTPServer
import calendar
import time
from datetime import datetime, date, timedelta
import threading
from flask import Flask
import re

from twisted.internet.defer import Deferred
from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor
from twisted.internet import task
from twisted.python import log
import txthings.coap as coap
import txthings.resource as resource
from ipaddress import ip_address

client = '' 
currentPort = ''
payload = ''

ips = ['aaaa::212:4b00:804:d603', 'aaaa::212:4b00:804:d383', 'aaaa::212:4b00:804:e084']
MAP_PORT = { ips[0]: 9001, ips[1]:9002, ips[2]:9003 }
MAP_LOC = { ips[0]: Point((-22.8305, -43.2192)), ips[1]: Point((-22.8305, -43.2202)), ips[2]: Point((-22.8315, -43.2192)) }
sensorData = { ips[0]: [], ips[1]: [], ips[2]: [] }

ROUTES = [('/', '/id')]

endpoint = resource.Endpoint(None)
protocol = coap.Coap(endpoint)

def MakeHandlerClass(idx):
	class CustomHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
		def do_GET(self):
			print("Replying for " + str(self.idx))
			if None != re.search('/beacon/*', self.path):
				beaconID = self.path.split('=')[-1]
				print('beaconID = ' + beaconID)
				if sensorData[self.idx].has_key(beaconID):
					self.send_response(200)
					self.send_header('Content-Type', 'application/json')
					self.end_headers()
					self.wfile.write(sensorData[self.idx])
				else:
					self.send_response(404)
					self.send_header('Content-Type', 'application/json')
					self.end_headers()
			else:
				self.send_response(404, 'Not found: record does not exist')
				self.send_header('Content-Type', 'application/json')
				self.end_headers()
			return
		def __init__(self, request, client_addr, server):
			print("Initializing handler with " + str(idx))
			self.idx = idx
			SimpleHTTPServer.SimpleHTTPRequestHandler.__init__(self, request, client_addr, server)
	return CustomHandler

class ThreadingSimpleServer(ThreadingMixIn, HTTPServer):
	pass

class Agent():	
    def __init__(self, protocol):
        self.protocol = protocol  
        self.currentIp = ''
        reactor.callLater(1, self.requestPutResource)    
        reactor.callLater(2, self.startGetRequestCoap)         

    def startGetRequestCoap(self):
		l = task.LoopingCall(self.requestGetResource)
		l.start(3)

    def requestGetResource(self):
		print('Request CoAP GET')
		for x in ips:
			currentIp = x
			request = coap.Message(code=coap.GET)
			request.opt.uri_path = ('ble',)
			request.opt.observe = 0
			request.remote = (ip_address(x), coap.COAP_PORT)
			d = protocol.request(request, observeCallback=self.printLaterResponse)
			d.addCallback(self.processGetResponse)
			d.addErrback(self.noResponse)
		pass
        
    def requestPutResource(self):
		for x in ips:
			payload = 'utm='+str(calendar.timegm(time.gmtime()))
			request = coap.Message(code=coap.PUT, payload = payload)
			request.opt.uri_path = ('uTimer',)
			request.opt.content_format = coap.media_types_rev['text/plain']
			request.remote = (ip_address(x), coap.COAP_PORT)
			d = protocol.request(request)
			#d.addCallback(self.printPutResponse)		

    def processGetResponse(self, response):
        print 'Result: ' + response.payload
        sensorData[currentIp] = response.payload
	for entry in sensorData[currentIp]:
		entry["loc"] = MAP_LOC[currentIp]
        print 'Processed for IP ' + currentIp + ': ' + sensorData[currentIp]
        #reactor.stop()

    def printLaterResponse(self, response):
        print 'Observe result: ' + response.payload
        payload[currentIp] = response.payload

    def noResponse(self, failure):
        print 'Failed to fetch resource:'
        print failure
        #reactor.stop()
        
	#def printPutResponse(self, response):
		#print coap.responses[response.code]
		#print response.payload
        
 
def createHTTPServer(ip, port):
	print(port)
	#server = ThreadingSimpleServer((ip,port), Handler)
	#server.handle_request()
	server = SocketServer.TCPServer((ip, port), MakeHandlerClass(port))
	thread = threading.Thread(target=server.serve_forever)
	thread.start()
	#httpd = SocketServer.TCPServer((ip, port), Handler)
	#httpd.serve_forever()

def mongoClientUpdate(scanner):
	client = MongoClient("mongodb://vps.de.paula.nom.br/vigintillion")
	db = client.vigintillion
	db['sensors.ble'].update({"id":scanner["id"]}, scanner, upsert=True)

def scannerToJSON(loc, id, port):
	data = {}
	data['loc'] = loc
	data['id'] = id
	data['ip'] = '10.0.9.173'
	data['port'] = port
	data['timestamp'] = calendar.timegm(time.gmtime())
	jsonData = json.dumps(data)
	return data
    
def startCoapListen():
	log.startLogging(sys.stdout)
	client = Agent(protocol)  
	reactor.listenUDP(0, protocol, interface='::0')
	reactor.run()

if __name__ == '__main__':
	print(len(ips))
	for x in ips:
		currentIp = x
		mongoClientUpdate(scannerToJSON(MAP_LOC[x], x, MAP_PORT[x]))
		print('momote IP: ' + x)
		createHTTPServer('', MAP_PORT[x])
	startCoapListen()
