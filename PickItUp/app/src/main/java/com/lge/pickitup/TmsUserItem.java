package com.lge.pickitup;

import java.util.ArrayList;

public class TmsUserItem {
    private static final String usertype_admin = "admin";
    private static final String usertype_courier = "courier";
    private static final String usertype_consignor = "consignor";

    public String uid = "";
    public String username = "";
    public String usertype = "";
    public String parentId = "";
    public String brand = "";
    public ArrayList<TmsUserItem> children;
    public ArrayList<String> brands;

    public TmsUserItem() { /* empty */}

    public TmsUserItem(String uid, String username, String usertype, String parentId, String brand) {
        this.uid = uid;
        this.username = username;
        this.usertype = usertype;
        this.parentId = parentId;
        this.brand = brand;
        this.children = new ArrayList<>();
        this.brands = new ArrayList<>();
    }

    public void addChild(TmsUserItem acc) {
        children.add(acc);
    }

    public void removeChild(TmsUserItem acc) {
        children.remove(acc);
    }

    public boolean hasChild(TmsUserItem acc) {
        if (acc.equals(this)) return true;

        for (TmsUserItem child : this.children) {
            if (child.hasChild(acc)) return true;
        }
        return false;
    }

    public void addBrand(String brand) {
        brands.add(brand);
    }

    public void removeBrand(String brand) {
        brands.remove(brand);
    }

    public boolean hasBrand(String brand) {
        return brands.contains(brand);
    }
}
