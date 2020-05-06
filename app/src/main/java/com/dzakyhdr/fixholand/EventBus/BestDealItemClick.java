package com.dzakyhdr.fixholand.EventBus;

import com.dzakyhdr.fixholand.Model.BestDealModel;

public class BestDealItemClick {

    private BestDealModel bestDealModel;

    public BestDealItemClick(BestDealModel bestDealModel) {
        this.bestDealModel = bestDealModel;
    }

    public BestDealModel getBestDealModel() {
        return bestDealModel;
    }

    public void setBestDealModel(BestDealModel bestDealModel) {
        this.bestDealModel = bestDealModel;
    }
}
