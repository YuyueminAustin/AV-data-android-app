package com.msra.avliveness.utils;

public class DeviceInfo {
    private String name;

    private String id;

    public DeviceInfo(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getID() { return id;}

    @Override
    public String toString() {
        return name;
    }
}
