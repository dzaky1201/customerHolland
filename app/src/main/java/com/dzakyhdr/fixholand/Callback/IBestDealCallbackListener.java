package com.dzakyhdr.fixholand.Callback;


import com.dzakyhdr.fixholand.Model.BestDealModel;

import java.util.List;

public interface IBestDealCallbackListener {
    void onBestDealLoadSuccess(List<BestDealModel> bestDealModels);
    void onBestDealLoadFailed(String message);
}
