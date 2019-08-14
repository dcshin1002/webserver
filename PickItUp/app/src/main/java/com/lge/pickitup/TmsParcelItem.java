package com.lge.pickitup;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class TmsParcelItem implements Comparable<TmsParcelItem>{
    public static final String STATUS_COLLECTED = "collected";    // Submitted to courier service initially
    public static final String STATUS_GEOCODED = "geocoded";      // Converted to geocode from address facade
    public static final String STATUS_DELIVERED = "delivered";    // Delivered

    public static final String UNSET = "";

    public static final String KEY_ID = "id";
    public static final String KEY_TRACKING_NUM = "trackingNum";
    public static final String KEY_PACKAGE_TYPE = "packageType";
    public static final String KEY_DATE = "date";
    // 보내는 사람 정보
    public static final String KEY_CONSIGNOR_NAME = "consignorName";
    public static final String KEY_CONSIGNOR_CONTACT = "consignorContact";
    // 받는 사람 정보
    public static final String KEY_CONSIGNEE_NAME = "consigneeName";
    public static final String KEY_CONSIGNEE_ADDR = "consigneeAddr";
    public static final String KEY_CONSIGNEE_CONTACT = "consigneeContact";
    // 받는 사람 주소의 위경도 정보
    public static final String KEY_CONSIGNEE_LONGITUDE = "consigneeLongitude";
    public static final String KEY_CONSIGNEE_LATITUDE = "consigneeLatitude";
    // 배달 기사 정보
    public static final String KEY_COURIER_NAME = "courierName";
    public static final String KEY_COURIER_CONTACT = "courierContact";
    public static final String KEY_REMARK = "remark";
    public static final String KEY_DELIVERY_NOTE = "deliveryNote";
    public static final String KEY_REGIONAL_CODE = "regionalCode";
    public static final String KEY_SECTOR_ID = "sectorId";
    public static final String KEY_ORDER_ID = "orderInRoute";
    public static final String KEY_STATUS = "status";

    public String id;
    public String trackingNum = UNSET;
    public String packageType = UNSET;
    public String date = UNSET;

    // Information about Consignor (Sender)
    public String consignorName = UNSET;
    public String consignorContact = UNSET;

    // Information about Consignee (Receiver)
    public String consigneeName = UNSET;
    public String consigneeAddr = UNSET;
    public String consigneeContact = UNSET;
    public String consigneeLongitude = UNSET;
    public String consigneeLatitude = UNSET;

    // Information about courier (driver or individual to transfer)
    public String courierName = UNSET;
    public String courierContact = UNSET;

    // Delivery note (memo)
    public String remark = UNSET;
    public String deliveryNote = UNSET;

    // Information to process parcel
    public String regionalCode = UNSET;
    public int sectorId = -1;
    public int orderInRoute = -1;
    public String status = STATUS_COLLECTED;

    public TmsParcelItem() {
        // Default constructor required for calls to DataSnapshot.getValue(TmsParcelItem.class)
    }

    public TmsParcelItem(String id, String trackingNum, String packageType, String date,
                         String consignorName, String consignorContact,
                         String consigneeName, String consigneeAddr, String consigneeContact,
                         String remark, String deliveryNote) {
        this.id = id;
        this.trackingNum = trackingNum;

        if (packageType != null) {
            this.packageType = packageType;
        }

        this.date = date;
        this.consignorName = consignorName;

        if (consignorContact != null) {
            this.consignorContact = consignorContact;
        }

        this.consigneeName = consigneeName;
        this.consigneeAddr = consigneeAddr;

        if (consigneeContact != null) {
            this.consigneeContact = consigneeContact;
        }
        if (remark != null) {
            this.remark = remark;
        }
        if (deliveryNote != null) {
            this.deliveryNote = deliveryNote;
        }
    }

    void setGeocode (String longitude, String latitude) {
        this.consigneeLatitude = latitude;
        this.consigneeLongitude = longitude;
    }

    void setSectorId (int id) {
        this.sectorId = id;
    }

    void setOrderId (int id) {
        this.orderInRoute = id;
    }

    void setCourier(String name, String contact) {
        if (name != null)
            this.courierName = name;
        if (contact != null)
            this.courierContact = contact;
    }

    void setStatus (String newStatus) {
        if (this.status.equals(newStatus)) {
            return;
        } else {
            //Todo: Need to check newStatus is valid or not
            this.status = newStatus;
        }
    }

    @Override
    public int compareTo(TmsParcelItem s) {
        if (this.orderInRoute < s.orderInRoute) {
            return -1;
        } else if (this.orderInRoute > s.orderInRoute) {
            return 1;
        }
        return 0;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(KEY_ID, id);
        result.put(KEY_TRACKING_NUM, trackingNum);
        result.put(KEY_PACKAGE_TYPE, packageType);
        result.put(KEY_DATE, date);
        result.put(KEY_CONSIGNOR_NAME, consignorName);
        result.put(KEY_CONSIGNOR_CONTACT, consignorContact);
        result.put(KEY_CONSIGNEE_NAME, consigneeName);
        result.put(KEY_CONSIGNEE_ADDR, consigneeAddr);
        result.put(KEY_CONSIGNEE_CONTACT, consigneeContact);
        result.put(KEY_CONSIGNEE_LONGITUDE, consigneeLongitude);
        result.put(KEY_CONSIGNEE_LATITUDE, consigneeLatitude);
        result.put(KEY_COURIER_NAME, courierName);
        result.put(KEY_COURIER_CONTACT, courierContact);
        result.put(KEY_REMARK, remark);
        result.put(KEY_DELIVERY_NOTE, deliveryNote);
        result.put(KEY_REGIONAL_CODE, regionalCode);
        result.put(KEY_SECTOR_ID, sectorId);
        result.put(KEY_ORDER_ID, orderInRoute);
        result.put(KEY_STATUS, status);
        return result;
    }
}
