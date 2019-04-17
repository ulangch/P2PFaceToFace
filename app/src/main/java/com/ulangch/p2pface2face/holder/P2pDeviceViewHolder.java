package com.ulangch.p2pface2face.holder;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ulangch.p2pface2face.R;
import com.ulangch.p2pface2face.model.P2pDevice;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class P2pDeviceViewHolder extends RecyclerView.ViewHolder {

    private TextView mName;
    private TextView mAddress;

    public P2pDeviceViewHolder(View itemView) {
        super(itemView);
        mName = itemView.findViewById(R.id.name);
        mAddress = itemView.findViewById(R.id.address);

    }

    public static P2pDeviceViewHolder create(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_p2p_device, parent, false);
        return new P2pDeviceViewHolder(itemView);
    }

    public void bind(P2pDevice p2pDevice) {
        mName.setText(p2pDevice.getName());
        mAddress.setText(p2pDevice.getAddress());
    }
}
