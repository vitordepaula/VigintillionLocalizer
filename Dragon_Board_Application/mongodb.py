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
import time
from datetime import datetime, date, timedelta
import threading
from flask import Flask

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

MAP_IPV6 = {'aaaa::212:4b00:804:d500':9001, 'aaaa::212:4b00:804:d383':9002, 'aaaa::212:4b00:804:e084':9003}
payload = {'aaaa::212:4b00:804:d500':'','aaaa::212:4b00:804:d383':'', 'aaaa::212:4b00:804:e084':''}
ips = ['aaaa::212:4b00:804:d500', 'aaaa::212:4b00:804:d383'		, 'aaaa::212:4b00:804:e084']
ROUTES = [('/', '/id')]

endpoint = resource.Endpoint(None)
protocol = coap.Coap(endpoint)

class Handler(SimpleHTTPServer.SimpleHTTPRequestHandler):
	
	def do_GET(self):
		# Construct a server response.
		self.send_response(200)
		self.send_header('Content-type','text/plain')
		self.end_headers()

		mac = str(self.path)
		mac = mac.split('=')
		print(mac)
		if len(mac) > 1:
			for key,value in payload.iteritems():
				print(value)
				if mac[1] not in value:
					continue
				else:
					self.wfile.write(value)
		return		
	
	def translate_path(self, path):
		root = os.getcw()
		for patt, rootDir in ROUTES:
			if path.startswith(patt):
				path = path[len(patt):]
				root = rootDir
				break
		return os.path.join(root,path)


        
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
		print('REQUESTGET!!!!!!!!!!')
		for x in ips:
			currentIp = x
			request = coap.Message(code=coap.GET)
			request.opt.uri_path = ('ble',) #Request HTTP que vem do Mongo
			request.opt.observe = 0
			request.remote = (ip_address(x), coap.COAP_PORT)
			d = protocol.request(request, observeCallback=self.printLaterResponse)
			d.addCallback(self.printGetResponse)
			d.addErrback(self.noResponse)
		pass
        
    def requestPutResource(self):
		for x in ips:
			payload = 'utm='+str(toTimestamp(datetime.utcnow()))
			request = coap.Message(code=coap.PUT, payload = payload)
			request.opt.uri_path = ('uTimer',)
			request.opt.content_format = coap.media_types_rev['text/plain']
			request.remote = (ip_address(x), coap.COAP_PORT)
			d = protocol.request(request)
			#d.addCallback(self.printPutResponse)		

    def printGetResponse(self, response):
        print 'First result: ' + response.payload
        payload[currentIp] = response.payload
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
	server = SocketServer.TCPServer((ip, port), Handler)
	thread = threading.Thread(target=server.serve_forever)
	thread.start()
	#httpd = SocketServer.TCPServer((ip, port), Handler)
	#httpd.serve_forever()

def mongoClientInsert(scanner):
    client = MongoClient("mongodb://vps.de.paula.nom.br/vigintillion")
    db = client.vigintillion
    db['ble-sensors'].insert_one(scanner)


def scannersToJSON(loc, mac, ip, date):
    data = {}
    data['loc'] = loc
    data['id'] = mac
    data['ip'] = ip
    data['timestamp'] = str(toTimestamp(datetime.utcnow()))
    jsonData = json.dumps(data)
    return data
    
    
    
def toTimestamp(dt, epoch=datetime(1970,1,1)):
	td = dt-epoch
	return (td.microseconds + (td.seconds + td.days*86400) *10**6) / 10**6


def location():
	g = geocoder.freegeoip('131.221.243.1')
	return Point((-22.8305, -43.2192))

def startCoapListen():
    log.startLogging(sys.stdout)

    client = Agent(protocol)  
          
    #reactor.listenUDP(61616, protocol)#, interface="::")
    reactor.listenUDP(0, protocol, interface='::0')
    reactor.run()
    
    

if __name__ == '__main__':
	print(location())
	mongoClientInsert(scannersToJSON(location(),'11:22:33:44:55:66', '192.168.1.10', '28-08-2016-01-59-43'))	
	print(len(ips))
	for x in ips:
		currentIp = x
		print('valor de x:' + x)
		createHTTPServer('', MAP_IPV6[x])
	startCoapListen()

