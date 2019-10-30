package com.lge.pickitup;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class TmsUserItem {
    public static final String usertype_admin = "admin";
    public static final String usertype_courier = "courier";
    public static final String usertype_consignor = "consignor";

    public String uid = "";
    public String username = "";
    public String usertype = "";
    public String parentId = "";
    public String brand = "";
    public ArrayList<TmsUserItem> children = new ArrayList<>();

    public TmsUserItem() { /* empty */}

    public TmsUserItem(String uid, String username, String usertype, String parentId, String brand) {
        this.uid = uid;
        this.username = username;
        this.usertype = usertype;
        this.parentId = parentId;
        this.brand = brand;
    }

    public void addChild(TmsUserItem acc) {
        children.add(acc);
    }

    public void removeChild(TmsUserItem acc) {
        children.remove(acc);
    }

    public boolean hasChild() {
        return !children.isEmpty();
    }

    public boolean hasChild(String name) {
        if (name.equals(this.username)) return true;

        for (TmsUserItem child : this.children) {
            if (child.hasChild(name)) return true;
        }
        return false;
    }

    public boolean hasChild(TmsUserItem acc) {
        if (acc.uid.equals(this.uid)) return true;

        for (TmsUserItem child : this.children) {
            if (child.hasChild(acc)) return true;
        }
        return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        TmsUserItem item = (TmsUserItem)obj;
        return this.uid.equals(item.uid);
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }
}
