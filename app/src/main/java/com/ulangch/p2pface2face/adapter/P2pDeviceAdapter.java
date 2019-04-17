package com.ulangch.p2pface2face.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.ulangch.p2pface2face.holder.P2pDeviceViewHolder;
import com.ulangch.p2pface2face.model.P2pDevice;

import java.util.ArrayList;
import java.util.List;


/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class P2pDeviceAdapter extends RecyclerView.Adapter {

    private List<P2pDevice> mP2pDevices;

    public void setData(List<P2pDevice> devices) {
        if (mP2pDevices == null) {
            mP2pDevices = new ArrayList<>();
        }
        mP2pDevices.clear();
        mP2pDevices.addAll(devices);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return P2pDeviceViewHolder.create(viewGroup);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        ((P2pDeviceViewHolder) viewHolder).bind(mP2pDevices.get(i));
    }

    @Override
    public int getItemCount() {
        return mP2pDevices != null ? mP2pDevices.size() : 0;
    }
}
