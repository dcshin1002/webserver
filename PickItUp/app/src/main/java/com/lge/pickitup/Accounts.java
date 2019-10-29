package com.lge.pickitup;

import java.util.ArrayList;

public class Accounts {
    public String id = "";
    public String name = "";
    public boolean isManager = false;
    public ArrayList<Accounts> children;
    public ArrayList<String> brands;

    public Accounts() { /* empty */}

    public Accounts(String id, String name, boolean manager) {
        this.id = id;
        this.name = name;
        this.isManager = manager;
    }

    public void addChild(Accounts acc) {
        children.add(acc);
    }

    public void removeChild(Accounts acc) {
        children.remove(acc);
    }

    public boolean hasChild(Accounts acc) {
        if (acc.equals(this)) return true;

        for (Accounts child : this.children) {
            if (acc.hasChild(child)) return true;
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
