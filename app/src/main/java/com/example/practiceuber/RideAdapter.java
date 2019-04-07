package com.example.practiceuber;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.uber.sdk.android.rides.RideParameters;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.ViewHolder>{
    private ArrayList<Ride> rideEstims;
    ItemClicked activity;
    private Context ctx;
    int position;

    /*
   Interface to be implemented by activites that want to use
   this list (makes the list clickable)
    */
    public interface ItemClicked{
        void onItemClicked(int index);
    }

    public RideAdapter(Context context, ArrayList<Ride> list){
        rideEstims = list;
        activity = (ItemClicked) context;
        ctx = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvLyftEstim, tvUberEstim;
        TextView tvLyftTitle, tvUberTitle;
        Button btnGoToLyft, btnGoToUber;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            tvLyftEstim = itemView.findViewById(R.id.tvLyftEstim);
            tvUberEstim = itemView.findViewById(R.id.tvUberEstim);
            tvLyftTitle = itemView.findViewById(R.id.tvLyftTitle);
            tvUberTitle = itemView.findViewById(R.id.tvUberTitle);

            btnGoToLyft = itemView.findViewById(R.id.btnGoToLyft);
            btnGoToUber = itemView.findViewById(R.id.btnGoToUber);

            /*
            If user wants to go to the Uber app, send them to method in Main
             */
            btnGoToUber.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   if (ctx instanceof MainActivity){
                       ((MainActivity)ctx).goToUberApp();
                   }
                }
            });

            /*
            If user wants to go to Lyft app, send them to method in Main
             */
            btnGoToLyft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ctx instanceof MainActivity){
                        ((MainActivity)ctx).goToLyftApp();
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public RideAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rides_row, viewGroup, false);
        return new RideAdapter.ViewHolder(view);
    }

    /*
    Sets what the contents of the list item should show
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.itemView.setTag(rideEstims.get(i));
        viewHolder.tvUberTitle.setText("Uber".toUpperCase());
        viewHolder.tvLyftTitle.setText("Lyft".toUpperCase());

        if (rideEstims.get(i).getLyftMin().equals("0") && rideEstims.get(i).getLyftMax().equals("0")){
            viewHolder.tvLyftEstim.setText("Not Available".toUpperCase());
            viewHolder.tvLyftTitle.setText("Lyft".toUpperCase());
        }
        else {
            viewHolder.tvLyftTitle.setText(rideEstims.get(i).getLyftRideType());
            String lyftEstims =
                    "\nMinimum: " + rideEstims.get(i).getLyftMin() +
                            "$\nMaximum: " + rideEstims.get(i).getLyftMax() + "$";
            viewHolder.tvLyftEstim.setText(lyftEstims);
        }

        if (rideEstims.get(i).getUberMin().equals("0") && rideEstims.get(i).getUberMax().equals("0")){
            viewHolder.tvUberEstim.setText("Not Available".toUpperCase());
            viewHolder.tvUberTitle.setText("Uber".toUpperCase());
        }
        else {
            viewHolder.tvUberTitle.setText(rideEstims.get(i).getUberRideType());
            String uberEstims =
                    "\nMinimum: " + rideEstims.get(i).getUberMin() +
                            "$\nMaximum: " + rideEstims.get(i).getUberMax() + "$";
            viewHolder.tvUberEstim.setText(uberEstims);
        }

    }

    @Override
    public int getItemCount(){
        return rideEstims.size();
    }
}
