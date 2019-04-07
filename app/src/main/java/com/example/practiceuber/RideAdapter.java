package com.example.practiceuber;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.ViewHolder>{
    private ArrayList<Ride> rideEstims;
    ItemClicked activity;
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
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvLyftEstim, tvUberEstim;
        Button btnGoToLyft, btnGoToUber;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            tvLyftEstim = itemView.findViewById(R.id.tvLyftEstim);
            tvUberEstim = itemView.findViewById(R.id.tvUberEstim);
            btnGoToLyft = itemView.findViewById(R.id.btnGoToLyft);
            btnGoToUber = itemView.findViewById(R.id.btnGoToUber);
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
        String lyftEstims = rideEstims.get(i).getRideType() +
                "\nMinimum: " + rideEstims.get(i).getLyftMin() +
                "$\nMaximum: " + rideEstims.get(i).getLyftMax() + "$";
        viewHolder.tvLyftEstim.setText(lyftEstims);
    }

    @Override
    public int getItemCount(){
        return rideEstims.size();
    }
}
