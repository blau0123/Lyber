package com.example.practiceuber;

public class LyftRide {
    private String minEstim, maxEstim;
    private String rideType;

    public LyftRide(){
        minEstim = "0";
        maxEstim = "0";
        rideType = "None";
    }

    public LyftRide(String min, String max, String ride){
        minEstim = min;
        maxEstim = max;
        rideType = ride;
    }

    public String getMaxEstim() {
        return maxEstim;
    }

    public String getMinEstim() {
        return minEstim;
    }

    public String getRideType() {
        return rideType;
    }
}
