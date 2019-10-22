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
    private TmsCourierItem assignedCourier;
    private HashMap<String, LinkedList<TmsParcelItem>> mParcelItems;
    private HashSet<TmsCourierItem> mCourierItems;

    public AssignParcelsUtil(String date, HashMap<String, LinkedList<TmsParcelItem>> checkedItems,  HashSet<TmsCourierItem> couriers, TmsCourierItem courierToAssign) {
        this.date = date;
        this.mCourierItems = couriers;
        this.mParcelItems = checkedItems;
        this.assignedCourier = courierToAssign;
    }


    public void assignCourier() {

    }

    private LinkedList<TmsParcelItem> removeParcelsFromList(ArrayList<TmsParcelItem> parcelsToRemove) {
        LinkedList<TmsParcelItem> tmp = new LinkedList<>();
        return tmp;
    }

    private LinkedList<TmsParcelItem> attachParcelsToCourier(LinkedList<TmsParcelItem> parcelsToAttach, String courierName) {
        LinkedList<TmsParcelItem> tmp = new LinkedList<>();
        return tmp;
    }
    private void setToFirebaseDB() {

    }

}
