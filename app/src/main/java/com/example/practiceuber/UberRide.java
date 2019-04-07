package com.example.practiceuber;

public class UberRide {
    private String minEstim, maxEstim;
    private String rideType;

    public UberRide(){
        minEstim = "0";
        maxEstim = "0";
        rideType = "None";
    }

    public UberRide(String min, String max, String ride){
        minEstim = min;
        maxEstim = max;
        rideType = ride;
    }

    public UberRide(UberRide prevRide){
        minEstim = prevRide.getMinEstim();
        maxEstim = prevRide.getMaxEstim();
        rideType = prevRide.getRideType();
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
