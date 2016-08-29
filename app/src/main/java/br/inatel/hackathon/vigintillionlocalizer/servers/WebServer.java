package br.inatel.hackathon.vigintillionlocalizer.servers;

/**
 * Created by vitor on 28/08/16.
 */

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import br.inatel.hackathon.vigintillionlocalizer.database.DB;
import br.inatel.hackathon.vigintillionlocalizer.fragments.BluetoothScannerFragment;
import br.inatel.hackathon.vigintillionlocalizer.model.Beacon;
import fi.iki.elonen.NanoHTTPD;

/**
 * HTTP Server
 */
public class WebServer extends NanoHTTPD {

    private static final String TAG = WebServer.class.getSimpleName();
    private DB mDb;

    public WebServer(DB db) {
        super(BluetoothScannerFragment.HTTP_SERVER_PORT);
        mDb = db;
    }

    @Override
    public void start() throws IOException {
        Log.d(TAG, "HTTP: server starting");
        super.start();
    }

    @Override
    public void stop() {
        Log.d(TAG, "HTTP: server stopping");
        super.stop();
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getMethod().equals(Method.GET)) {
            //TODO: validate URI path
            List<String> id_list = session.getParameters().get("id");
            if (id_list != null && id_list.size() > 1)
                Log.w(TAG, "Ignoring multiple queries for ID fields");
            else if (id_list == null || id_list.isEmpty()) {
                Log.w(TAG, "HTTP: ID not supplied for query");
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                        "text/plain", "Bad request.");
            }
            String id = id_list.get(0);
            try {
                Beacon beacon = mDb.detected_search(id);
                if (beacon == null) {
                    Log.d(TAG, "HTTP: ID " + id + " not found");
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                            "text/plain", "Not found.");
                }
                String answer = new JSONObject()
                        .put("timestamp", beacon.getTimestamp())
                        .put("lat", beacon.getLocation().latitude)
                        .put("lon", beacon.getLocation().longitude)
                        .put("signal", beacon.getRssi())
                        .toString();
                //Log.d(TAG, "Replying to query of tag " + id + ": " + answer);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                        "application/json", answer);
            } catch (JSONException je) {
                Log.e(TAG, "HTTP: Internal error for id " + id);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        "text/plain", "Internal server error.");
            }
        } else {
            Log.w(TAG, "HTTP: method not allowed");
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                    "text/plain", "Not allowed.");
        }
    }
}
