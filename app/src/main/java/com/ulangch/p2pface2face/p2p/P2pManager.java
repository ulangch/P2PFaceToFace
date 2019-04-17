package com.ulangch.p2pface2face.p2p;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;

import com.ulangch.p2pface2face.model.MutableLiveData;
import com.ulangch.p2pface2face.model.P2pDevice;
import com.ulangch.p2pface2face.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 * Activity内的单例
 * TODO: 改成状态机模式
 *
 * 关闭WiFi:
 * 04-17 13:24:35.598 14125 14125 I P2pManager: onReceive: android.net.wifi.p2p.PEERS_CHANGED-
 * 04-17 13:24:35.602 14125 14125 I P2pManager: onReceive: android.net.wifi.p2p.STATE_CHANGED-false
 * 04-17 13:24:35.672 14125 14125 I P2pManager: onReceive: android.net.wifi.WIFI_STATE_CHANGED-false
 * 04-17 13:24:36.012 14125 14125 I P2pManager: onReceive: android.net.wifi.WIFI_STATE_CHANGED-false
 *
 * 开启WiFi:
 * 04-17 13:25:09.477 14125 14125 I P2pManager: onReceive: android.net.wifi.WIFI_STATE_CHANGED-false
 * 04-17 13:25:09.828 14125 14125 I P2pManager: onReceive: android.net.wifi.WIFI_STATE_CHANGED-true
 * 04-17 13:25:09.885 14125 14125 I P2pManager: onReceive: android.net.wifi.p2p.STATE_CHANGED-true
 * 04-17 13:25:09.890 14125 14125 I P2pManager: onReceive: android.net.wifi.p2p.THIS_DEVICE_CHANGED-Device: 小米手机
 */
public class P2pManager extends ViewModel implements
        WifiP2pManager.ActionListener, WifiP2pManager.ChannelListener {

    private static final String TAG = "P2pManager";

    private Context mContext;
    private WifiManager mWifiManager;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    private IntentFilter mP2pFilter;
    private BroadcastReceiver mP2pReceiver;
    private IntentFilter mWifiFilter;
    private BroadcastReceiver mWifiReceiver;

    private Handler mTimeoutHandler;

    private boolean mInitialized;
    private boolean mWifiEnabled;
    private boolean mNeedDiscover;

    private int mRetryDiscoverTimes;

    private MutableLiveData<P2pDevice> mThisDevice = new MutableLiveData<>();
    private MutableLiveData<List<P2pDevice>> mP2pDevices = new MutableLiveData<>();
    private MutableLiveData<Pair<Boolean, Integer>> mP2pState = new MutableLiveData<>();
    private MutableLiveData<Pair<Boolean, Integer>> mP2pDiscoverState = new MutableLiveData<>();

    public static final int NO_ERROR = 0;
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_NO_PERMISSION = -2;
    public static final int ERROR_WIFI_DISABLED = -3;
    public static final int ERROR_START_DISCOVER_TIMEOUT = -4;
    public static final int ERROR_MANAGER_NOT_SETUP = -5;

    private static final int MAX_WAIT_WIFI_TIME_MILLS = 5 * 1000; // 5s
    private static final int MAX_WAIT_DISCOVER_TIME_MILLS = 3 * 1000; // 3s
    private static final int MAX_RETRY_DISCOVER_TIMES_1 = 2; // discover失败时，最大重试次数（不stop discover）
    private static final int MAX_RETRY_DISCOVER_TIMES_2 = 4; // discover失败时，最大重试次数（stop discover）

    public static final String[] P2P_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
    };

    public interface IP2pCallback {
        void onThisDeviceChanged(P2pDevice device);
        void onP2pChanged(List<P2pDevice> list);
        void onP2pStateChanged(boolean enabled, int error);
        void onP2pDiscoverStateChanged(boolean discover, int error);
    }

    private class P2pBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                boolean enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,
                        WifiP2pManager.WIFI_P2P_STATE_DISABLED)
                        == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
                Log.i(TAG, "onReceive: " + action + "-" + enabled);
                boolean originEnabled = isP2pEnabled();
                setP2pState(enabled, mWifiEnabled ? NO_ERROR : ERROR_WIFI_DISABLED);
                if (!originEnabled && enabled && !isP2pDiscovering() && mNeedDiscover) {
                    startDiscover();
                }
            } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
                boolean started =  intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,
                        WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED)
                        == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED;
                Log.i(TAG, "onReceive: " + action + "-" + intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,
                        WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED));
                setP2pDiscoverState(started, mWifiEnabled ? NO_ERROR : ERROR_WIFI_DISABLED);
                if (!started && mNeedDiscover) {
                    startDiscover();
                } else if (started) {
                    mRetryDiscoverTimes = 0;
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                WifiP2pDeviceList devices = intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                Log.i(TAG, "onReceive: " + action + "-" + devices);
                setP2pDevices(devices);
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice p2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                Log.i(TAG, "onReceive: " + action + "-" + p2pDevice);
                setThisDevice(p2pDevice);
            }
        }
    }

    private class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                mWifiEnabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED)
                        == WifiManager.WIFI_STATE_ENABLED;
                Log.i(TAG, "onReceive: " + action + "-" + mWifiEnabled);
            }
        }
    }

    public static P2pManager of(FragmentActivity context) {
        P2pManager p2pManager = ViewModelProviders.of(context).get(P2pManager.class);
        p2pManager.setupP2pManager(context);
        return p2pManager;
    }


    public void addP2pCallback(LifecycleOwner lifecycleOwner, final IP2pCallback callback) {
        mThisDevice.observe(lifecycleOwner, new Observer<P2pDevice>() {
            @Override public void onChanged(@Nullable P2pDevice p2pDevice) {
                callback.onThisDeviceChanged(p2pDevice);
            }
        });
        mP2pDevices.observe(lifecycleOwner, new Observer<List<P2pDevice>>() {
            @Override public void onChanged(@Nullable List<P2pDevice> p2pDevices) {
                callback.onP2pChanged(p2pDevices);
            }
        });
        mP2pState.observe(lifecycleOwner, new Observer<Pair<Boolean, Integer>>() {
            @Override public void onChanged(@Nullable Pair<Boolean, Integer> pair) {
                callback.onP2pStateChanged(pair.first, pair.second);
            }
        });
        mP2pDiscoverState.observe(lifecycleOwner, new Observer<Pair<Boolean, Integer>>() {
            @Override public void onChanged(@Nullable Pair<Boolean, Integer> pair) {
                callback.onP2pDiscoverStateChanged(pair.first, pair.second);
            }
        });
    }

    public void pollP2pDevices() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler().post(new Runnable() {
                @Override public void run() {
                    pollP2pDevices();
                }
            });
        } else {
            startDiscover();
        }
    }

    public P2pDevice getThisDevice() {
        return mThisDevice.getValue();
    }

    private void setThisDevice(WifiP2pDevice device) {
        if (device != null) {
            mThisDevice.setValue(P2pDevice.from(device));
        } else {
            mThisDevice.setValue(null);
        }
    }

    public List<P2pDevice> getP2pDevices() {
        return mP2pDevices.getValue();
    }

    private void setP2pDevices(WifiP2pDeviceList devices) {
        List<P2pDevice> p2pDevices = new ArrayList<>();
        for (WifiP2pDevice device : devices.getDeviceList()) {
            if (device != null) {
                P2pDevice p2pDevice = P2pDevice.from(device);
                p2pDevices.add(p2pDevice);
            }
        }
        mP2pDevices.setValue(p2pDevices);
    }

    private void setP2pState(boolean enabled, int error) {
        mP2pState.setValue(new Pair<>(enabled, error));
    }

    private boolean isP2pEnabled() {
        Pair<Boolean, Integer> p2pState = mP2pState.getValue();
        return p2pState != null && p2pState.first;
    }

    private void setP2pDiscoverState(boolean discover, int error) {
        mP2pDiscoverState.setValue(new Pair<>(discover, error));
    }

    private boolean isP2pDiscovering() {
        Pair<Boolean, Integer> discoverState = mP2pDiscoverState.getValue();
        return discoverState != null && discoverState.first;
    }

    private void setupP2pManager(Context context) {
        if (mInitialized) {
            Log.i(TAG, "P2pManager already setup");
            return;
        }
        mContext = context;
        mTimeoutHandler = new Handler();
        registerWifiReceiver();
        registerP2pReceiver();
        Context appContext = mContext.getApplicationContext();
        mWifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            Log.e(TAG, "Fail to get WifiManager");
            releaseP2pManager();
            return;
        }
        mWifiP2pManager = (WifiP2pManager) appContext.getSystemService(Context.WIFI_P2P_SERVICE);
        if (mWifiP2pManager == null) {
            Log.e(TAG, "Fail to get WifiP2pManager");
            releaseP2pManager();
            return;
        }
        mChannel = mWifiP2pManager.initialize(appContext, Looper.getMainLooper(), this);
        if (mChannel == null) {
            Log.e(TAG, "Fail to setup connection with WifiP2pService");
            releaseP2pManager();
            return;
        }
        mRetryDiscoverTimes = 0;
        mInitialized = true;
    }

    private void releaseP2pManager() {
        Log.i(TAG, "releaseP2pManager");
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mContext.unregisterReceiver(mWifiReceiver);
        mContext.unregisterReceiver(mP2pReceiver);
        mContext = null;
        mWifiManager = null;
        mWifiP2pManager = null;
        mChannel = null;
        mP2pFilter = null;
        mP2pReceiver = null;
        mWifiFilter = null;
        mWifiReceiver = null;
        mTimeoutHandler = null;
        mInitialized = false;
        mWifiEnabled = false;
        mNeedDiscover = false;
        mInitialized = false;
        mRetryDiscoverTimes = 0;
    }

    private void registerP2pReceiver() {
        if (mP2pFilter == null && mP2pReceiver == null) {
            mP2pFilter = new IntentFilter();
            mP2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mP2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mP2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            mP2pFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            mP2pFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            mP2pReceiver = new P2pBroadcastReceiver();
            mContext.registerReceiver(mP2pReceiver, mP2pFilter);
        } else {
            Log.e(TAG, "P2p Receiver already registered");
        }
    }

    private void registerWifiReceiver() {
        if (mWifiFilter == null && mWifiReceiver == null) {
            mWifiFilter = new IntentFilter();
            mWifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            mWifiReceiver = new WifiBroadcastReceiver();
            mContext.registerReceiver(mWifiReceiver, mWifiFilter);
        } else {
            Log.e(TAG, "WiFi Receiver already registered");
        }
    }

    private boolean checkPermission() {
        if (!PermissionUtils.checkPermissions((Activity) mContext, P2P_PERMISSIONS)) {
            return false;
        }
        return true;
    }

    private boolean checkWifiState() {
        Log.i(TAG, "checkWifiState");
        if (mWifiManager.isWifiEnabled()) {
            mWifiEnabled = true;
            return true;
        }
        mWifiEnabled = false;
        // 国内很多厂商会hold住setWifiEnabled的Binder调用
        // 在主线程中调用可能会产生ANR
        new Thread(new Runnable() {
            @Override public void run() {
                mWifiManager.setWifiEnabled(true);
            }
        }).start();
        mTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mWifiEnabled) {
                    setP2pState(false, ERROR_WIFI_DISABLED);
                }
            }
        }, MAX_WAIT_WIFI_TIME_MILLS);

        return false;
    }

    private void startDiscover() {
        Log.i(TAG, "startDiscover");
        if (!mInitialized) {
            Log.e(TAG, "P2pManager has not been initialized");
            setP2pState(false, ERROR_MANAGER_NOT_SETUP);
            return;
        }
        mNeedDiscover = true;
        if (isP2pDiscovering()) {
            Log.i(TAG, "Being discovering now");
            setP2pDiscoverState(true, NO_ERROR);
        } else if (!checkPermission()) {
            Log.e(TAG, "No WiFi permission");
            setP2pDiscoverState(false, ERROR_NO_PERMISSION);
        } else if (!checkWifiState()) {
            // WiFi开启出现未知异常
            // 需要等待WiFi开启
            Log.e(TAG, "Need to wait WiFi");
        } else {
            Log.i(TAG, "Real startDiscover");
            mWifiP2pManager.discoverPeers(mChannel, this);
            mTimeoutHandler.postDelayed(retryDiscoverTask(), MAX_WAIT_DISCOVER_TIME_MILLS);
        }
    }

    private Runnable retryDiscoverTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (isP2pDiscovering() || !mNeedDiscover) {
                    return;
                }
                mRetryDiscoverTimes++;
                Log.i(TAG, "Retry to discover for: " + mRetryDiscoverTimes + " times");
                if (mRetryDiscoverTimes <= MAX_RETRY_DISCOVER_TIMES_1) {
                    startDiscover();
                } else if (mRetryDiscoverTimes <= MAX_RETRY_DISCOVER_TIMES_2) {
                    stopDiscover(false);
                    startDiscover();
                } else {
                    setP2pDiscoverState(false, ERROR_START_DISCOVER_TIMEOUT);
                    Log.e(TAG, "Fail to discover for max times");
                }
            }
        };
        return runnable;
    }

    private void stopDiscover(boolean release) {
        Log.i(TAG, "stopDiscover");
        mNeedDiscover = false;
        if (isP2pDiscovering()) {
            mWifiP2pManager.stopPeerDiscovery(mChannel, null);
        }
        if (release) {
            releaseP2pManager();
        }
    }

    @Override
    public void onChannelDisconnected() {
        Log.i(TAG, "WifiP2p channel disconnected");
        if (mInitialized && mNeedDiscover) {
            mWifiP2pManager.initialize(mContext.getApplicationContext(), Looper.getMainLooper(), this);
        }
    }

    @Override
    public void onSuccess() {
        Log.i(TAG, "Success");
        // setP2pDiscoverState(true, NO_ERROR);
        // 等广播时再设置
    }

    @Override
    public void onFailure(int error) {
        Log.e(TAG, "Fail" + error);
    }

    @Override
    protected void onCleared() {
        Log.i(TAG, "onCleared");
        stopDiscover(true);
        super.onCleared();
    }
}
