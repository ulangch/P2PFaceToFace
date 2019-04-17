package com.ulangch.p2pface2face.ui;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.ulangch.p2pface2face.R;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class P2pAddFriendActivity extends AppCompatActivity {

    private static final String TAG = "P2pAddFriendActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2p_add_friend);
        FragmentManager fm = getSupportFragmentManager();
        P2pAddFriendFragment fragment = (P2pAddFriendFragment) fm.findFragmentById(R.id.root_p2p_add_friend);
        if (fragment == null) {
            fragment = P2pAddFriendFragment.newInstance();
            fm.beginTransaction().add(R.id.root_p2p_add_friend, fragment).commit();
        }
    }

}
