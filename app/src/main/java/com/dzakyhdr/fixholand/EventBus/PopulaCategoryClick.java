package com.dzakyhdr.fixholand.EventBus;

import com.dzakyhdr.fixholand.Model.PopularCategoryModel;

public class PopulaCategoryClick {
    private PopularCategoryModel popularCategoryModel;

    public PopulaCategoryClick(PopularCategoryModel popularCategoryModel) {
        this.popularCategoryModel = popularCategoryModel;
    }

    public PopularCategoryModel getPopularCategoryModel() {
        return popularCategoryModel;
    }

    public void setPopularCategoryModel(PopularCategoryModel popularCategoryModel) {
        this.popularCategoryModel = popularCategoryModel;
    }
}
