package com.dzakyhdr.fixholand.Callback;

import com.dzakyhdr.fixholand.Model.OrderModel;

import java.util.List;

public interface ILoadOrderCallbackListener {

    void onLoadOrderSuccess(List<OrderModel>orderList);
    void onLoadOrderFailed(String message);

}
