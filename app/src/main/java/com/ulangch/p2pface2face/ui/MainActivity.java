package com.ulangch.p2pface2face.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.ulangch.p2pface2face.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openP2pAddFriendActivity(View v) {
        startActivity(new Intent(this, P2pAddFriendActivity.class));
    }
}
