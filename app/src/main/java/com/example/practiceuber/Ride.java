package com.example.practiceuber;

import java.util.ArrayList;

public class Ride {
    private UberRide uberRide;
    private LyftRide lyftRide;

    public Ride(){
        uberRide = new UberRide();
        lyftRide = new LyftRide();
    }

    public Ride(UberRide uber, LyftRide lyft){
        uberRide = uber;
        lyftRide = lyft;
    }

    public String getUberRideType(){
        return uberRide.getRideType().toUpperCase();
    }

    public String getLyftRideType(){
        return lyftRide.getRideType().toUpperCase();
    }

    public String getLyftMax(){
        return lyftRide.getMaxEstim();
    }

    public String getLyftMin(){
        return lyftRide.getMinEstim();
    }

    public String getUberMin(){
        return uberRide.getMinEstim();
    }

    public String getUberMax(){
        return uberRide.getMaxEstim();
    }
}
