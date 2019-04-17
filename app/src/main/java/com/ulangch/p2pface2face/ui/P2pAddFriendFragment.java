package com.ulangch.p2pface2face.ui;

import android.Manifest;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ulangch.p2pface2face.R;
import com.ulangch.p2pface2face.adapter.P2pDeviceAdapter;
import com.ulangch.p2pface2face.model.P2pDevice;
import com.ulangch.p2pface2face.p2p.P2pManager;
import com.ulangch.p2pface2face.utils.HardwareUtils;
import com.ulangch.p2pface2face.utils.PermissionUtils;

import java.util.Arrays;
import java.util.List;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class P2pAddFriendFragment extends Fragment implements P2pManager.IP2pCallback{

    private static final String TAG = "P2pAddFriendFragment";

    private static final int REQUEST_PERMISSION_CODE = 0xf001;

    private RecyclerView mRecyclerView;
    private P2pDeviceAdapter mAdapter;

    private TextView mName;
    private TextView mAddress;

    public static P2pAddFriendFragment newInstance() {
        return new P2pAddFriendFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_p2p_add_friend, container,  false);
        mName = root.findViewById(R.id.name);
        mAddress = root.findViewById(R.id.address);
        mRecyclerView = root.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        DividerItemDecoration decor = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        decor.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider_recycler_view));
        mRecyclerView.addItemDecoration(decor);
        mAdapter = new P2pDeviceAdapter();
        mRecyclerView.setAdapter(mAdapter);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        P2pManager.of(getActivity()).addP2pCallback(this, this);
        if (PermissionUtils.checkPermissions(getActivity(), P2pManager.P2P_PERMISSIONS)) {
            setCurrentDevice(P2pManager.of(getActivity()).getThisDevice());
            pollP2pDevice();
        } else {
            PermissionUtils.requestPermissions(this, P2pManager.P2P_PERMISSIONS, REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult: " + Arrays.toString(permissions) + ", " + Arrays.toString(grantResults));
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (PermissionUtils.checkPermissions(getActivity(), P2pManager.P2P_PERMISSIONS)) {
                pollP2pDevice();
            } else {
                Toast.makeText(getContext(), "需要开启WiFi权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setCurrentDevice(P2pDevice device) {
        if (device == null) {
            mName.setText("正在获取中...");
        } else {
            mName.setText(device.getName());
            mAddress.setText(device.getAddress());
        }
    }

    private void pollP2pDevice() {
        P2pManager.of(getActivity()).pollP2pDevices(getActivity());
    }

    @Override
    public void onThisDeviceChanged(P2pDevice device) {
        Log.i(TAG, "onThisDeviceChanged: " + device);
        setCurrentDevice(device);
    }

    @Override
    public void onP2pChanged(List<P2pDevice> list) {
        Log.i(TAG, "onP2pChanged: " + list);
        mAdapter.setData(list);
    }

    @Override
    public void onP2pStateChanged(boolean enabled, int error) {
        Log.i(TAG, "onP2pStateChanged: " + enabled + ", " + error);
        if (!enabled) {
            if (error == P2pManager.ERROR_WIFI_DISABLED) {
                Toast.makeText(getContext(), "请先开启WiFi后再重试", Toast.LENGTH_LONG).show();
            } else if (error == P2pManager.ERROR_UNKNOWN) {
                Toast.makeText(getContext(), "未知异常，无法开启P2p", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onP2pDiscoverStateChanged(boolean discover, int error) {
        Log.i(TAG, "onP2pDiscoverStateChanged: " + discover + ", " + error);
        if (!discover) {
            if (error == P2pManager.NO_ERROR) {
                // 达到扫描间隔时间，继续扫描
                // pollP2pDevice();
            } else if (error == P2pManager.ERROR_UNKNOWN) {
                Toast.makeText(getContext(), "未知异常，无法扫描P2p", Toast.LENGTH_LONG).show();
            } else if (error == P2pManager.ERROR_START_DISCOVER_TIMEOUT) {
                Toast.makeText(getContext(), "开启扫描超时", Toast.LENGTH_LONG).show();
            }
        }
    }
}
