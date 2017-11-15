package com.android.taal_rider.historyRecyclerView;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.taal_rider.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolders> {

    private List<HistoryObject> mItemList;
    private Context mContext;

    public HistoryAdapter(List<HistoryObject> itemList, Context context) {
        this.mItemList = itemList;
        this.mContext = context;
    }

    @Override
    public HistoryViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutView.setLayoutParams(lp);
        HistoryViewHolders rcv = new HistoryViewHolders(layoutView);

        return rcv;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolders holder, final int position) {
        holder.mRideId.setText(mItemList.get(position).getRideId());
        if (mItemList.get(position).getTime() != null) {
            holder.mTime.setText(mItemList.get(position).getTime());
        }
    }

    @Override
    public int getItemCount() {
        return this.mItemList.size();
    }

}