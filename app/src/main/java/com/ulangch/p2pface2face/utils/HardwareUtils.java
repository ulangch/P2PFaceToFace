package com.ulangch.p2pface2face.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class HardwareUtils {

    public static String getMacAddress(Context context) {
        String macAddress = null;
        WifiInfo wifiInfo = getWifiInfo(context);
        if (wifiInfo != null) {
            macAddress = wifiInfo.getMacAddress();
        }

        if ("02:00:00:00:00:00".equals(macAddress) || TextUtils.isEmpty(macAddress)) {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                String name = getNetworkInterfaceName();

                while(true) {
                    NetworkInterface netWork;
                    byte[] by;
                    do {
                        do {
                            if (!interfaces.hasMoreElements()) {
                                return macAddress;
                            }

                            netWork = (NetworkInterface)interfaces.nextElement();
                            by = netWork.getHardwareAddress();
                        } while(by == null);
                    } while(by.length == 0);

                    StringBuilder builder = new StringBuilder();
                    byte[] var8 = by;
                    int var9 = by.length;

                    for(int var10 = 0; var10 < var9; ++var10) {
                        byte b = var8[var10];
                        builder.append(String.format("%02X:", b));
                    }

                    if (builder.length() > 0) {
                        builder.deleteCharAt(builder.length() - 1);
                    }

                    String mac = builder.toString();
                    if (netWork.getName().equals(name)) {
                        macAddress = mac;
                        break;
                    }
                }
            } catch (Exception var12) {
                var12.printStackTrace();
            }
        }

        return macAddress;
    }

    public static WifiInfo getWifiInfo(Context context) {
        if (context == null) {
            return null;
        } else {
            try {
                WifiManager mWifi = (WifiManager)context.getApplicationContext().getSystemService("wifi");
                WifiInfo wifiInfo = mWifi.getConnectionInfo();
                ConnectivityManager connection = (ConnectivityManager)context.getApplicationContext().getSystemService("connectivity");
                if (connection.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED && wifiInfo.getSSID() != null) {
                    return wifiInfo;
                }
            } catch (Exception var4) {
                var4.printStackTrace();
            }

            return null;
        }
    }

    private static String getNetworkInterfaceName() {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", String.class, String.class);
            return (String)method.invoke(clazz, "wifi.interface", "wlan0");
        } catch (Exception var2) {
            var2.printStackTrace();
            return "wlan0";
        }
    }


}
