package br.inatel.hackathon.vigintillionlocalizer.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by lucas on 27/08/2016.
 */
public class Beacon {
    private String id; // MAC address
    private int rssi;
    private long timestamp; // unix time (epoch)
    private LatLng location;

    public String getId() {
        return id;
    }

    public Beacon setId(String id) {
        this.id = id;
        return this;
    }

    public int getRssi() { return rssi; }

    public Beacon setRssi(int rssi) {
        this.rssi = rssi;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Beacon setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LatLng getLocation() {
        return location;
    }

    public Beacon setLocation(LatLng location) {
        this.location = location;
        return this;
    }
}
