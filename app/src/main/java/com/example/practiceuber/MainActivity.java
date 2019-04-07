package com.example.practiceuber;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
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
import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.rides.client.ServerTokenSession;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.model.PriceEstimate;
import com.uber.sdk.rides.client.model.PriceEstimatesResponse;
import com.uber.sdk.rides.client.model.Product;
import com.uber.sdk.rides.client.model.ProductsResponse;
import com.uber.sdk.rides.client.model.TimeEstimatesResponse;
import com.uber.sdk.rides.client.services.RidesService;

import java.io.IOException;
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
    // variables for lyft
    private LyftPublicApi lyftPublicApi;
    private ApiConfig apiConfig;
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
                // clear the recyclerview so that last search won't be in the list
                clearRecyclerView();

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
                        getPriceEstimateLyft();
                        getPriceEstimateUber();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Nani", Toast.LENGTH_SHORT).show();
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
            setStartAddressAsCurrent();
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
                    setStartAddressAsCurrent();
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
            addr = addresses.get(0);
        } catch(IOException e){
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

    class loadEstimations

    /*
    Using the Lyft API, calculate the estimated price for each ride type
     */
    private void getPriceEstimateLyft(){
        Call<CostEstimateResponse> costEstimateCall = lyftPublicApi.getCosts(startLat, startLong,
                RideTypeEnum.ALL.toString(), endLat, endLong);

        costEstimateCall.enqueue(new Callback<CostEstimateResponse>() {
            @Override
            public void onResponse(Call<CostEstimateResponse> call, Response<CostEstimateResponse> response) {
                CostEstimateResponse result = response.body();
                for (CostEstimate costEstimate : result.cost_estimates){
                    // retrieving the estimated cost and ride type
                    String[] priceEstims = {String.valueOf(costEstimate.estimated_cost_cents_min / 100),
                            String.valueOf(costEstimate.estimated_cost_cents_max / 100)};
                    String rideType = costEstimate.ride_type;

                    // adding to the ArrayList representing the recyclerview items
                    ApplicationClass.rideEstims.add(new Ride(priceEstims, rideType));
                }
                myAdapter = new RideAdapter(MainActivity.this, ApplicationClass.rideEstims);
                rv.setAdapter(myAdapter);
            }

            @Override
            public void onFailure(Call<CostEstimateResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed call", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
    Using the Uber API, calculates the price estimates and the ride type
     */
    private void getPriceEstimateUber() {
        try {

            Response<ProductsResponse> response = service.getProducts((float)startLat, (float)startLong).execute();
            ProductsResponse products = response.body();
            List<Product> productIdList = products.getProducts();

            System.out.println(productIdList);
            /*
            Response<PriceEstimatesResponse> estims = service.getPriceEstimates((float)startLat, (float)startLong, (float)endLat, (float)endLong).execute();
            PriceEstimatesResponse result = estims.body();
            for (PriceEstimate price : result.getPrices()){
                System.out.println(price.getEstimate());
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
   Method for when item in recyclerView is clicked
    */
    @Override
    public void onItemClicked(int index) {
    }
}
