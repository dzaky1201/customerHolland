package com.dzakyhdr.fixholand.ui.foodlist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dzakyhdr.fixholand.Common.Common;
import com.dzakyhdr.fixholand.Model.FoodModel;

import java.util.List;

import butterknife.Unbinder;

public class FoodListViewModel extends ViewModel {
    private MutableLiveData<List<FoodModel>> mutableLiveDataFoodList;
    Unbinder unbinder;

    public FoodListViewModel() {

    }

    public MutableLiveData<List<FoodModel>> getMutableLiveDataFoodList() {
        if (mutableLiveDataFoodList == null)
            mutableLiveDataFoodList = new MutableLiveData<>();
        mutableLiveDataFoodList.setValue(Common.categorySelected.getFoods());
        return mutableLiveDataFoodList;
    }
}
