package com.lge.pickitup;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class AssignParcelsUtil {
    private String date;

    // <courier name, list of parcels> pair
    private HashMap<String, LinkedList<TmsParcelItem>> mParcelItems;
    private HashSet<TmsCourierItem> mCourierItems;

    public AssignParcelsUtil(String date, HashMap<String, LinkedList<TmsParcelItem>> parcels, HashSet<TmsCourierItem> couriers) {
        this.date = date;
        this.mCourierItems = couriers;
        this.mParcelItems = parcels;
    }

    public void assignCourier(ArrayList<TmsParcelItem> checkedParcels, TmsCourierItem checkedCourier) {

    }

    private LinkedList<TmsParcelItem> removeParcelsFromList(ArrayList<TmsParcelItem> parcelsToRemove) {
        LinkedList<TmsParcelItem> ret = new LinkedList<>();

    }

    private LinkedList<TmsParcelItem> attachParcelsToCourier(LinkedList<TmsParcelItem> parcelsToAttach, String courierName) {
        LinkedList<TmsParcelItem> tmp = new LinkedList<>();
        return tmp;
    }

    private void setToFirebaseDB() {

    }
}
