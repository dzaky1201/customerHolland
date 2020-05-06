package com.dzakyhdr.fixholand.ui.foodlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProviders;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;

import com.dzakyhdr.fixholand.Adapter.MyFoodListAdapter;
import com.dzakyhdr.fixholand.Common.Common;
import com.dzakyhdr.fixholand.EventBus.MenuItemBack;
import com.dzakyhdr.fixholand.Model.CategoryModel;
import com.dzakyhdr.fixholand.Model.FoodModel;
import com.dzakyhdr.fixholand.R;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class FoodListFragment extends Fragment {

    private FoodListViewModel foodListViewModel;
    Unbinder unbinder;
    @BindView(R.id.recycler_food_list)
    RecyclerView recycler_food_list;

    LayoutAnimationController layoutAnimationController;
    MyFoodListAdapter adapter;

    public static FoodListFragment newInstance() {
        return new FoodListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        foodListViewModel =
                ViewModelProviders.of(this).get(FoodListViewModel.class);
        View root = inflater.inflate(R.layout.fragment_food_list, container, false);
        unbinder = ButterKnife.bind(this,root);
        initViews();
        foodListViewModel.getMutableLiveDataFoodList().observe(getViewLifecycleOwner(), foodModels -> {
            if (foodModels != null)
            {
                adapter = new MyFoodListAdapter(getContext(), foodModels);
                recycler_food_list.setAdapter(adapter);
                recycler_food_list.setLayoutAnimation(layoutAnimationController);
            }

        });
        return root;
    }

    private void initViews() {
        ((AppCompatActivity)getActivity())
                .getSupportActionBar()
                .setTitle(Common.categorySelected.getName());
        recycler_food_list.setHasFixedSize(true);
        recycler_food_list.setLayoutManager(new LinearLayoutManager(getContext()));

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);

        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        //event
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                startSearch(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        //clear text ketika menekan close
        ImageView closeButton = (ImageView)searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(view -> {
            EditText ed = (EditText)searchView.findViewById(R.id.search_src_text);
            //clear text
            ed.setText("");
            //clear query
            searchView.setQuery("",false);
            //Collapse the action view
            searchView.onActionViewCollapsed();
            //Collapse the search widget
            menuItem.collapseActionView();
            //restore result to original
            foodListViewModel.getMutableLiveDataFoodList();

        });
    }

    private void startSearch(String s) {
        List<FoodModel> resultList = new ArrayList<>();
        for (int i = 0; i < Common.categorySelected.getFoods().size(); i++)
        {
            FoodModel foodModel = Common.categorySelected.getFoods().get(i);
            if (foodModel.getName().toLowerCase().contains(s))
                resultList.add(foodModel);


        }
        foodListViewModel.getMutableLiveDataFoodList().setValue(resultList);
    }
    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }

}
