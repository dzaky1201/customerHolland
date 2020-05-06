package com.dzakyhdr.fixholand.ui.view_orders;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dzakyhdr.fixholand.Adapter.MyOrdersAdapter;
import com.dzakyhdr.fixholand.Callback.ILoadOrderCallbackListener;
import com.dzakyhdr.fixholand.Common.Common;
import com.dzakyhdr.fixholand.Model.OrderModel;
import com.dzakyhdr.fixholand.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class ViewOrdersFragment extends Fragment implements ILoadOrderCallbackListener {

    @BindView(R.id.recycler_order)
    RecyclerView recycler_order;
    AlertDialog dialog;

    private Unbinder unbinder;
    private ViewOrdersViewModel viewOrdersViewModel;
    private ILoadOrderCallbackListener listener;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        viewOrdersViewModel =
                ViewModelProviders.of(this).get(ViewOrdersViewModel.class);
        View root = inflater.inflate(R.layout.fragment_view_orders, container, false);
        unbinder = ButterKnife.bind(this,root);

        iniView(root);
        loadOrdersFromFirebase();

        viewOrdersViewModel.getMutableLiveDataOrderList().observe(getViewLifecycleOwner(),orderList -> {
            MyOrdersAdapter adapter = new MyOrdersAdapter(getContext(),orderList);
            recycler_order.setAdapter(adapter);
        });

        return root;
    }

    private void loadOrdersFromFirebase() {
        List<OrderModel> orderList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                .orderByChild("userId")
                .equalTo(Common.currentUser.getUid())
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot orderSnapShot:dataSnapshot.getChildren())
                        {
                            OrderModel order = orderSnapShot.getValue(OrderModel.class);
                            order.setOrderNumber(orderSnapShot.getKey());
                            orderList.add(order);
                        }

                        listener.onLoadOrderSuccess(orderList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadOrderFailed(databaseError.getMessage());
                    }
                });
    }

    private void iniView(View root) {
        listener = this;
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).build();
        recycler_order.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_order.setLayoutManager(layoutManager);
        recycler_order.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));

    }

    @Override
    public void onLoadOrderSuccess(List<OrderModel> orderList) {
        dialog.dismiss();
        viewOrdersViewModel.setMutableLiveDataOrderList(orderList);

    }

    @Override
    public void onLoadOrderFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

    }
}
