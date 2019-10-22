package com.lge.pickitup;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

public class AssignParcelsUtil {
    private String date;

    // <courier name, list of parcels> pair
    private HashMap<String, LinkedList<TmsParcelItem>> mParcelItems;
    private HashMap<String, TmsCourierItem> mCourierItems;

    public AssignParcelsUtil(String date, HashMap<String, LinkedList<TmsParcelItem>> parcels, HashMap<String, TmsCourierItem> couriers) {
        this.date = date;
        this.mCourierItems = couriers;
        this.mParcelItems = parcels;
    }

    // Args : checked parcels, checked courier to assign these parcels
    public void assignCourier(ArrayList<TmsParcelItem> checkedParcels, TmsCourierItem checkedCourier) {
        LinkedList<TmsParcelItem> tailList = new LinkedList<>();
        for (Map.Entry<String, LinkedList<TmsParcelItem>> itemEntry : mParcelItems.entrySet()) {
            if (itemEntry.getKey().equals(checkedCourier.name)) continue;

            LinkedList<TmsParcelItem> eachList = removeParcelsFromList(itemEntry.getValue(), checkedParcels);
            if (eachList.isEmpty()) continue;

            if (!tailList.isEmpty()) {
                tailList.getLast().nextParcel = eachList.getFirst().id;
            }
            tailList.addAll(eachList);
        }

        attachParcelsToCourier(tailList, checkedCourier.name);
        setToFirebaseDB();
    }

    // remove if list contain checked parcel
    // Return : removed parcels from original list in order
    private LinkedList<TmsParcelItem> removeParcelsFromList(LinkedList<TmsParcelItem> parcelList, ArrayList<TmsParcelItem> parcelsToRemove) {
        LinkedList<TmsParcelItem> ret = new LinkedList<>();
        ListIterator<TmsParcelItem> iter = parcelList.listIterator();
        TmsParcelItem prev, cur;
        if (iter.hasNext()) {
            prev = iter.next();
            // head element is checked
            while (iter.hasNext() && parcelsToRemove.contains(prev)) {
                if (!prev.courierName.isEmpty()) {
                    TmsCourierItem courier = mCourierItems.get(prev.courierName);
                    courier.startparcelid = prev.nextParcel;
                }
                prev.courierName = "";
                prev.sectorId = -1;
                prev.nextParcel = -1;

                if (!ret.isEmpty()) {
                    ret.getLast().nextParcel = prev.id;
                }
                ret.add(prev);

                prev = iter.next();
            }

            while (iter.hasNext()) {
                cur = iter.next();

                if (parcelsToRemove.contains(cur)) {
                    prev.nextParcel = cur.nextParcel;
                    cur.courierName = "";
                    cur.sectorId = -1;
                    cur.nextParcel = -1;

                    if (!ret.isEmpty()) {
                        ret.getLast().nextParcel = cur.id;
                    }
                    ret.add(cur);
                } else {
                    prev = cur;
                }
            }

            TmsCourierItem courier = mCourierItems.get(prev.courierName);
            if (courier != null) {
                courier.endparcelid = prev.id;
            }
        }
        return ret;
    }

    private void attachParcelsToCourier(LinkedList<TmsParcelItem> parcelsToAttach, String courierName) {
        if (parcelsToAttach.isEmpty())
            return;

        int sectorId = mCourierItems.get(courierName).sectorid;
        for (TmsParcelItem item : parcelsToAttach) {
            item.courierName = courierName;
            item.sectorId = sectorId;
        }

        LinkedList<TmsParcelItem> listItems = mParcelItems.get(courierName);
        if (listItems != null) {
            listItems.getLast().nextParcel = parcelsToAttach.getFirst().id;
            TmsCourierItem courier = mCourierItems.get(listItems.getLast().courierName);
            if (courier != null) {
                courier.endparcelid = parcelsToAttach.getLast().id;
            }
        } else {
            listItems = new LinkedList<>();
            TmsCourierItem courier = mCourierItems.get(courierName);
            courier.startparcelid = parcelsToAttach.getFirst().id;
            courier.endparcelid = parcelsToAttach.getLast().id;
        }
        listItems.addAll(parcelsToAttach);
    }

    private void setToFirebaseDB() {
        DatabaseReference parcelref = FirebaseDatabase.getInstance().getReference().child(FirebaseDatabaseConnector.PARCEL_REF_NAME);
        DatabaseReference courierref = FirebaseDatabase.getInstance().getReference().child(FirebaseDatabaseConnector.COURIER_REF_NAME);
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> postValues = null;
        for (Map.Entry<String, LinkedList<TmsParcelItem>> itemEntry : mParcelItems.entrySet()) {
            for (TmsParcelItem item : itemEntry.getValue()) {
                postValues = item.toMap();
                childUpdates.put("/" + date + "/" + item.id, postValues);
            }
        }
        parcelref.updateChildren(childUpdates);
        childUpdates = new HashMap<>();
        postValues = null;
        for (Map.Entry<String, TmsCourierItem> itemEntry : mCourierItems.entrySet()) {
            TmsCourierItem courierItem = itemEntry.getValue();
            postValues = courierItem.toMap();
            childUpdates.put("/" + date + "/" + courierItem.id, postValues);
        }
        courierref.updateChildren(childUpdates);
    }
}
