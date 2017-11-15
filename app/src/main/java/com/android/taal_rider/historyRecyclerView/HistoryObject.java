package com.android.taal_rider.historyRecyclerView;

public class HistoryObject {

    private String mRideId;
    private String mTime;

    public HistoryObject(String rideId, String time) {
        this.mRideId = rideId;
        this.mTime = time;
    }

    public String getRideId() {
        return mRideId;
    }

    public void setRideId(String rideId) {
        this.mRideId = rideId;
    }

    public String getTime() {
        return mTime;
    }

    public void setTime(String time) {
        this.mTime = time;
    }
}
