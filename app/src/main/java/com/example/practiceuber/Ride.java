package com.example.practiceuber;

public class Ride {
    // index 0 = min, index 1 = max
    private String[] lyftEstimates;
    private String[] uberEstimates;
    private String rideType;

    public Ride(){
        lyftEstimates = new String[2];
        uberEstimates = new String[2];
        rideType = "lyft/uberx".toUpperCase();
    }

    public Ride(String[] lyft, String[] uber, String ride){
        lyftEstimates = new String[2];
        uberEstimates = new String[2];

        // deep copy for lyft estimates
        for (int i = 0; i < lyftEstimates.length; i++){
            lyftEstimates[i] = lyft[i];
        }

        // deep copy for uber estimates
        for (int i = 0; i < uberEstimates.length; i++){
            uberEstimates[i] = uber[i];
        }

        rideType = ride;
    }

    public Ride(String[] lyft, String ride){
        lyftEstimates = new String[2];

        // deep copy for lyft estimates
        for (int i = 0; i < lyftEstimates.length; i++){
            lyftEstimates[i] = lyft[i];
        }

        rideType = ride;
    }

    public String getUberMin(){
        return uberEstimates[0];
    }

    public String getUberMax(){
        return uberEstimates[1];
    }

    public String getLyftMin(){
        return lyftEstimates[0];
    }

    public String getLyftMax(){
        return lyftEstimates[1];
    }

    public String getRideType(){
        return rideType;
    }

}
