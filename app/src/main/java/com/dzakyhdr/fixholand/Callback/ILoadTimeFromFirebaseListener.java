package com.dzakyhdr.fixholand.Callback;

import com.dzakyhdr.fixholand.Model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSuccess(OrderModel order, long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
