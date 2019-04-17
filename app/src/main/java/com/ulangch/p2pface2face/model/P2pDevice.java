package com.ulangch.p2pface2face.model;

import android.net.wifi.p2p.WifiP2pDevice;
import android.support.annotation.NonNull;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class P2pDevice {
    String address;
    String name;

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static P2pDevice from(@NonNull WifiP2pDevice device) {
        P2pDevice p2pDevice = new P2pDevice();
        p2pDevice.address = device.deviceAddress;
        p2pDevice.name = device.deviceName;
        return p2pDevice;
    }

    @Override
    public String toString() {
        return "P2pUser{address=" + address + ", name=" + name + "} ";
    }
}
