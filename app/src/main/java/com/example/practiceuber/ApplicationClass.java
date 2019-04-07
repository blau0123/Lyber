package com.example.practiceuber;

import android.app.Application;

import java.util.ArrayList;

public class ApplicationClass extends Application {
    public static ArrayList<Ride> rideEstims;

    /*
    Creates a rides arraylist that will be used for holding contents of recyclerview list
     */
    @Override
    public void onCreate() {
        super.onCreate();

        rideEstims = new ArrayList<>();
        // so there's one card in the recyclerview so it is visible before any real data entered
        rideEstims.add(new Ride());
    }
}
