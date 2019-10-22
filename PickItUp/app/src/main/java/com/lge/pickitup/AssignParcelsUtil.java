package com.lge.pickitup;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/*
        // courierName, listParcel
        HashMap<String, LinkedList<TmsParcelItem>> parcelItems;


     */
public class AssignParcelsUtil {
    private String date;
    private HashMap<String, LinkedList<TmsParcelItem>> mParcelItems;
    private HashSet<TmsCourierItem> mCourierItems;

    public AssignParcelsUtil(String date, HashSet<TmsCourierItem> couriers) {
        this.date = date;
        this.mCourierItems = couriers;
        getFromFirebaseDB();
    }

    private void getFromFirebaseDB() {
        // get from firebaseDB -> mParcelItems, mCourierItems by given date

    }

    public void setToFirebaseDB() {
    }

    public LinkedList<TmsParcelItem> removeParcelsFromList(ArrayList<TmsParcelItem> parcelsToRemove) {
    }

    public LinkedList<TmsParcelItem> attachParcelsToCourier(LinkedList<TmsParcelItem> parcelsToAttach, String courierName) {
    }
}
