package br.inatel.hackathon.vigintillionlocalizer.model;

/**
 * Created by lucas on 27/08/2016.
 */
public class Beacon {
    private long id;
    private int rssi;
    private String mac;
    private String date;
    private float longitude;
    private float latitude;
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMac() {  return mac;  }

    public void setMac(String mac) { this.mac = mac; }

    public int getRssi() { return rssi; }

    public void setRssi(int rssi) { this.rssi = rssi; }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
