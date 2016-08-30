package br.inatel.hackathon.vigintillionlocalizer.model;

public class TrackedBeacon {
    public String beacon_id;
    public int color_id;

    public TrackedBeacon(String beacon_id, int color_id) {
        this.beacon_id = beacon_id;
        this.color_id = color_id;
    }
}
