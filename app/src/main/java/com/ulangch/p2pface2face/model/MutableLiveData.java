package com.ulangch.p2pface2face.model;

import android.arch.lifecycle.LiveData;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class MutableLiveData<T> extends LiveData<T> {

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }

    @Override
    public void postValue(T value) {
        super.postValue(value);
    }
}
