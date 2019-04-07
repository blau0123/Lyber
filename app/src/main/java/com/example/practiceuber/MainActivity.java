package com.example.practiceuber;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
// import uber api and sdk things
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.lyft.lyftbutton.RideTypeEnum;
import com.lyft.networking.ApiConfig;
import com.lyft.networking.LyftApiFactory;
import com.lyft.networking.apiObjects.CostEstimate;
import com.lyft.networking.apiObjects.CostEstimateResponse;
import com.lyft.networking.apis.LyftPublicApi;
import com.uber.sdk.android.core.Deeplink;
import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.android.rides.RideParameters;
import com.uber.sdk.rides.client.ServerTokenSession;
import com.uber.sdk.rides.client.Session;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.model.PriceEstimate;
import com.uber.sdk.rides.client.model.PriceEstimatesResponse;
import com.uber.sdk.rides.client.model.Product;
import com.uber.sdk.rides.client.model.ProductsResponse;
import com.uber.sdk.rides.client.model.RideEstimate;
import com.uber.sdk.rides.client.model.RideRequestParameters;
import com.uber.sdk.rides.client.model.TimeEstimatesResponse;
import com.uber.sdk.rides.client.services.RidesService;

import org.apache.log4j.chainsaw.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements RideAdapter.ItemClicked{
    EditText etStartAddr, etEndAddr;
    Button btnSubmit;
    // private variables involved with finding estimates
    private FusedLocationProviderClient fusedLocationClient;
    private double startLong, startLat, endLong, endLat;
    private RidesService service;
    private ArrayList<UberRide> uberRides;
    // variables for lyft
    private LyftPublicApi lyftPublicApi;
    private ApiConfig apiConfig;
    private ArrayList<LyftRide> lyftRides;
    //recyclerview to show list of rides
    RecyclerView rv;
    RecyclerView.Adapter myAdapter;
    RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etStartAddr = findViewById(R.id.etStartAddr);
        etEndAddr = findViewById(R.id.etEndAddr);
        btnSubmit = findViewById(R.id.btnSubmit);

        // setting up recyclerview
        rv = findViewById(R.id.rvRideList);
        rv.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        myAdapter = new RideAdapter(this, ApplicationClass.rideEstims);
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(myAdapter);

        /*
        When the submit button is clicked, calculate the price estimates
         */
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // hides the keyboard on click
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                } catch(Exception e){

                }

                // clear the recyclerview so that last search won't be in the list
                clearRecyclerView();
                if (!etStartAddr.getText().toString().trim().toUpperCase().equals("CURRENT LOCATION")) {
                    startLong = 0;
                    startLat = 0;
                }

                // check if start and end locations were inputted
                if ( etStartAddr.getText() != null && etEndAddr.getText() != null &&
                !etStartAddr.getText().toString().trim().equals("") &&
                !etEndAddr.getText().toString().trim().equals("")){
                    // get lat and long of each
                    // if startLong and startLat > 0, then current location is being used
                    if (startLong == 0 && startLat == 0){
                        // get lat and long of start loc because not from current oc
                        Address startLoc = getLocationFromAddress(MainActivity.this,
                                etStartAddr.getText().toString().trim());
                        startLat = startLoc.getLatitude();
                        startLong = startLoc.getLongitude();
                    }

                    // get lat and long of end loc
                    Address endLoc = getLocationFromAddress(MainActivity.this,
                            etEndAddr.getText().toString().trim());
                    if (endLoc != null) {
                        endLat = endLoc.getLatitude();
                        endLong = endLoc.getLongitude();
                        // call the asynctask to load the estimations
                        new MainActivity.loadEstimations().execute();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Be more specific, please :)", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // set location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            // permission is not granted, so ask for permission -- will call onRequestPermissionsResult
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else{
            // permission granted
            if (!etStartAddr.getText().toString().trim().toUpperCase().equals("CURRENT LOCATION")) {
                setStartAddressAsCurrent();
            }
        }

        setUpUber();
        setUpLyft();
    }


    /*
    Clears the RecyclerView of all elements
     */
    public void clearRecyclerView(){
        ApplicationClass.rideEstims.clear();
    }

    /*
    Called when user responds to a permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults){
        switch(requestCode){
            case 1:{
                // if request cancelled, result arrays empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permission granted, so set start addr as current location
                    if (!etStartAddr.getText().toString().trim().toUpperCase().equals("CURRENT LOCATION")) {
                        setStartAddressAsCurrent();
                    }
                }
                else{
                    // permission denied, don't set the start addr as anything!
                }
            }
        }
    }

    /*
    If location permissions are granted, this method is called.
    Sets the start longitude and latitude to the user's current location.
     */
    private void setStartAddressAsCurrent(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // get last location to put into start addr
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                startLong = location.getLongitude();
                                startLat = location.getLatitude();
                                etStartAddr.setText("Current Location");
                            }
                            else{
                                Toast.makeText(MainActivity.this,
                                        "Unable to get current location",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (SecurityException e){
            e.printStackTrace();
        }
    }

    /*
    For start address (if not current location) and end address, get the latitude
    and longitude of the given address to input into Uber
     */
    private Address getLocationFromAddress(Context context, String address){
        Geocoder coder = new Geocoder(context);
        List<Address> addresses;
        Address addr = null;
        try{
            addresses = coder.getFromLocationName(address, 5);
            if (addresses == null){
                return null;
            }
            System.out.println(addresses);
            addr = addresses.get(0);
        } catch(Exception e){
            e.printStackTrace();
        }
        return addr;
    }

    /*
    Set up the Uber API for obtaining price estimates and type of ride
     */
    private void setUpUber(){
        SessionConfiguration config = new SessionConfiguration.Builder()
                .setClientId("yPM13cwignlOH8w-8Ag09Ue0cW-dkFyN")
                .setServerToken("4kt8rc31h1lOMVccszXdRocg1vn9p383L9GLkiAC")
                //.setRedirectUri("12345")
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)
                .build();
        UberSdk.initialize(config);

        ServerTokenSession session = new ServerTokenSession(config);
        service = UberRidesApi.with(session).build().createService();
    }

    /*
    Set up the Lyft API for obtaining price estimates and type of ride
     */
    private void setUpLyft(){
        apiConfig = new ApiConfig.Builder()
                .setClientId("AGBKKrEDbx8D")
                .setClientToken("LR1yADOoyJChNY/Tf78p6+P8MT3fXcUJ5vl2u689oDjEwgAx4S7wjzonLEJrDvUNwbSCP2Wogy4oPt3vByoPY1TDEcKTi2eLgv1R+/pDf4MtpbqXSz5gxlI=")
                .build();
        lyftPublicApi = new LyftApiFactory(apiConfig).getLyftPublicApi();
    }

    /*
    Loads the Lyft and Uber price estimations in the background
     */
    public class loadEstimations extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            lyftRides = getPriceEstimateLyft();
            uberRides = getPriceEstimateUber();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // organize uber list so it is in same order as lyft
            orderLyftAndUber();

            //create lyber and add to recyclerview list
            ArrayList<Ride> rides = createLyber();
            ApplicationClass.rideEstims.addAll(rides);
            if (ApplicationClass.rideEstims.size() != 0){
                myAdapter = new RideAdapter(MainActivity.this, ApplicationClass.rideEstims);
                rv.setAdapter(myAdapter);
            }
        }
    }

    /*
    Combines ArrayList of uber and lyft into one Ride arraylist
     */
    public ArrayList<Ride> createLyber(){
        ArrayList<Ride> rides = new ArrayList<>();
        int index = 0;
        // add to Ride until one of them runs out
        for (; index < Math.min(lyftRides.size(), uberRides.size()); index++){
            rides.add(new Ride(uberRides.get(index), lyftRides.get(index)));
        }

        // if lyft has more entries left, then uber has none. add rest of lyft to rides
        if (index < lyftRides.size()){
            //System.out.println("index: " + index + ", lyftsize: " + lyftRides.size() + ", ubersize: " + uberRides.size());
            for (; index < lyftRides.size(); index++ ){
                rides.add(new Ride( new UberRide(), lyftRides.get(index)));
            }
        }
        // if not, then lyft has no entries left, so add rest of uber
        else{
            System.out.println("should be here");
            for (; index < uberRides.size(); index++){
                rides.add(new Ride( uberRides.get(index), new LyftRide()));
            }
        }
        return rides;
    }

    /*
    Reorder uberRides ArrayList so it is in the same order as lyftRides. Inefficient.
    e.g. Lyft Line is with UberPool, Lyft with UberX, etc
     */
    public void orderLyftAndUber(){
        ArrayList<UberRide> orderedUber = new ArrayList<>();

        //first is uberpool
        for (int i = 0; i < uberRides.size(); i++){
            if (uberRides.get(i).getRideType().toUpperCase().equals("UberPool".toUpperCase())){
                orderedUber.add(new UberRide(uberRides.get(i)));
                uberRides.remove(i);
                break;
            }
        }

        //second is uberx
        for (int i = 0; i < uberRides.size(); i++){
            if (uberRides.get(i).getRideType().toUpperCase().equals("UberX".toUpperCase())){
                orderedUber.add(new UberRide(uberRides.get(i)));
                uberRides.remove(i);
                break;
            }
        }

        //third is select
        for (int i = 0; i < uberRides.size(); i++){
            if (uberRides.get(i).getRideType().toUpperCase().equals("Select".toUpperCase())){
                orderedUber.add(new UberRide(uberRides.get(i)));
                uberRides.remove(i);
                break;
            }
        }

        //fourth is spanish
        for (int i = 0; i < uberRides.size(); i++){
            if (uberRides.get(i).getRideType().toUpperCase().equals("Español".toUpperCase())){
                orderedUber.add(new UberRide(uberRides.get(i)));
                uberRides.remove(i);
                break;
            }
        }

        //fifth is xl
        for (int i = 0; i < uberRides.size(); i++){
            if (uberRides.get(i).getRideType().toUpperCase().equals("uberxl".toUpperCase())){
                orderedUber.add(new UberRide(uberRides.get(i)));
                uberRides.remove(i);
                break;
            }
        }

        //last is suv
        for (int i = 0; i < uberRides.size(); i++){
            if (uberRides.get(i).getRideType().toUpperCase().equals("ubersuv".toUpperCase())){
                orderedUber.add(new UberRide(uberRides.get(i)));
                uberRides.remove(i);
                break;
            }
        }

        // make the global uber rides arraylist the same as the ordered list
        uberRides.clear();
        uberRides.addAll(orderedUber);
        for ( UberRide r : uberRides ){
            System.out.println(r.getRideType());
        }

    }

    /*
    Using the Lyft API, calculate the estimated price for each ride type
     */
    private ArrayList<LyftRide> getPriceEstimateLyft(){
        final ArrayList<LyftRide> lyftRides = new ArrayList<>();
        Call<CostEstimateResponse> costEstimateCall = lyftPublicApi.getCosts(startLat, startLong,
                RideTypeEnum.ALL.toString(), endLat, endLong);

        costEstimateCall.enqueue(new Callback<CostEstimateResponse>() {
            @Override
            public void onResponse(Call<CostEstimateResponse> call, Response<CostEstimateResponse> response) {
                CostEstimateResponse result = response.body();
                for (CostEstimate costEstimate : result.cost_estimates){
                    // retrieving the estimated cost and ride type
                    String min = String.valueOf(costEstimate.estimated_cost_cents_min / 100);
                    String max = String.valueOf(costEstimate.estimated_cost_cents_max / 100);
                    String rideType = costEstimate.ride_type;

                    // adding to the ArrayList representing the recyclerview items
                    lyftRides.add(new LyftRide(min, max, rideType));
                }
            }

            @Override
            public void onFailure(Call<CostEstimateResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed call", Toast.LENGTH_SHORT).show();
            }
        });
        return lyftRides;
    }

    /*
    Using the Uber API, calculates the price estimates and the ride type
     */
    private ArrayList<UberRide> getPriceEstimateUber() {
        ArrayList<UberRide> uber = new ArrayList<>();
        try {
            Response<PriceEstimatesResponse> priceResponse = service.getPriceEstimates(
                    (float) startLat, (float) startLong, (float) endLat, (float) endLong
            ).execute();

            // if code is 422, then the user entered a start and end 100 miles apart (not allowed!)
            if ( priceResponse.code() != 422 ) {
                PriceEstimatesResponse result = priceResponse.body();
                List<PriceEstimate> prices = result.getPrices();

                for (PriceEstimate estim : prices) {
                    String min = String.valueOf(estim.getLowEstimate());
                    String max = String.valueOf(estim.getHighEstimate());
                    String rideName = estim.getDisplayName();
                    //System.out.println(rideName + " Min: " + min + " Max: " + max);
                    uber.add(new UberRide(min, max, rideName));
                }
            }
            else{
                // send message to user not to have locations over 100 miles away, send Toast on main thread
                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "You can't have a travel of over 100 miles!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uber;
    }

    /*
   Method for when item in recyclerView is clicked
    */
    @Override
    public void onItemClicked(int index) {
    }

    /*
    Called from adapter when the user wants to go to Uber through button click
     */
    public void goToUberApp(){
       if (isPackageInstalled(this, "com.ubercab")){
           openLink(this, "uber://?action=setPickup&pickup[latitude]=" + startLat +
                   "&pickup[longitude]=" + startLong + "&dropoff[latitude]=" +
                   endLat + "&dropoff[longitude]=" + endLong);
       }
       else{
           Toast.makeText(this, "Uber not installed!", Toast.LENGTH_SHORT).show();
       }
    }

    public void goToLyftApp(){
        if (isPackageInstalled(this, "me.lyft.android")){
            openLink(this, "lyft://ridetype?id=lyft&pickup[latitude]=" + startLat +
                    "&pickup[longitude]=" + startLong + "&destination[latitude]=" +
                    endLat + "&destination[longitude]=" + endLong);
        }
        else{
            Toast.makeText(this, "Lyft not installed!", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    Send user to given activity
     */
    static void openLink(Activity activity, String link){
        Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
        playStoreIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        playStoreIntent.setData(Uri.parse(link));
        activity.startActivity(playStoreIntent);
    }

    /*
    Helper method to check if the Lyft/Uber package is already installed. If so, take
    user to the native app. If not, take them to the app store.
     */
    static boolean isPackageInstalled(Context context, String packageId){
        PackageManager pm = context.getPackageManager();
        try{
            pm.getPackageInfo(packageId, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e){
            //ignored if name not found -- will return false after
        }
        return false;
    }
}
