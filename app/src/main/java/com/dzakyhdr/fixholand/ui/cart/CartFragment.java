package com.dzakyhdr.fixholand.ui.cart;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProviders;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import com.dzakyhdr.fixholand.Adapter.MyCartAdapter;
import com.dzakyhdr.fixholand.Callback.ILoadTimeFromFirebaseListener;
import com.dzakyhdr.fixholand.Common.Common;
import com.dzakyhdr.fixholand.Common.MySwiperHelper;
import com.dzakyhdr.fixholand.Database.CartDataSource;
import com.dzakyhdr.fixholand.Database.CartDatabase;
import com.dzakyhdr.fixholand.Database.CartItem;
import com.dzakyhdr.fixholand.Database.LocalCartDataSource;
import com.dzakyhdr.fixholand.EventBus.CounterCartEvent;
import com.dzakyhdr.fixholand.EventBus.HideFABCart;
import com.dzakyhdr.fixholand.EventBus.MenuItemBack;
import com.dzakyhdr.fixholand.EventBus.UpdateItemInCart;
import com.dzakyhdr.fixholand.HomeActivity;
import com.dzakyhdr.fixholand.Model.FCMRespone;
import com.dzakyhdr.fixholand.Model.FCMSendData;
import com.dzakyhdr.fixholand.Model.OrderModel;
import com.dzakyhdr.fixholand.R;
import com.dzakyhdr.fixholand.remote.ICloudFunctions;
import com.dzakyhdr.fixholand.remote.IFCMService;
import com.dzakyhdr.fixholand.remote.RetrofitFCMClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CartFragment extends Fragment implements ILoadTimeFromFirebaseListener {

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placesFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;

    ILoadTimeFromFirebaseListener listener;
    IFCMService ifcmService;
    ICloudFunctions cloudFunctions;

    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    private CartViewModel cartViewModel;
    private Unbinder unbinder;
    private MyCartAdapter adapter;

    @OnClick(R.id.btn_place_holder)
    void onPlaceOrderClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("One More Step");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);
        EditText edt_comment = view.findViewById(R.id.edt_comment);
        TextView txt_address = view.findViewById(R.id.txt_address_detail);
        RadioButton rdi_other_address = view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_home = view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_ship_to_this = view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = view.findViewById(R.id.rdi_braintree);

        places_fragment = (AutocompleteSupportFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placesFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected = place;
                txt_address.setText(place.getAddress());
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(getContext(), ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //data
        txt_address.setText(Common.currentUser.getAddress()); // alamat detail

        //Event
        rdi_home.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                txt_address.setText(Common.currentUser.getAddress());
                txt_address.setVisibility(View.VISIBLE);
                places_fragment.setHint(Common.currentUser.getAddress());
            }
        });
        rdi_other_address.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                txt_address.setVisibility(View.VISIBLE);
            }
        });
        rdi_ship_to_this.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(e -> {

                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            txt_address.setVisibility(View.GONE);

                        }).addOnCompleteListener(task -> {
                    String coordinates = new StringBuilder()
                            .append(task.getResult().getLatitude())
                            .append("/")
                            .append(task.getResult().getLongitude()).toString();

                    Single<String> singleAddress = Single.just(getAddressFromLatLng(task.getResult().getLatitude(),
                            task.getResult().getLongitude()));

                    Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>() {
                        @Override
                        public void onSuccess(String s) {
                            txt_address.setText(s);
                            txt_address.setVisibility(View.VISIBLE);
                            places_fragment.setHint(s);

                        }

                        @Override
                        public void onError(Throwable e) {
                            txt_address.setText(e.getMessage());
                            txt_address.setVisibility(View.VISIBLE);

                        }
                    });


                });
            }
        });

        builder.setView(view);
        builder.setNegativeButton("NO", (dialog, which) -> {
            dialog.dismiss();

        }).setPositiveButton("YES", (dialog, which) -> {
//            Toast.makeText(getContext(), "Implement late !", Toast.LENGTH_SHORT).show();
            if (rdi_cod.isChecked())
                paymentCOD(txt_address.getText().toString(), edt_comment.getText().toString());
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void paymentCOD(String address, String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems -> {
                    //when we have all cart items, we will get total price
                    cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Double>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Double totalPrice) {
                                    double finalPrice = totalPrice; // for discount
                                    OrderModel order = new OrderModel();
                                    order.setUserId(Common.currentUser.getUid());
                                    order.setUserName(Common.currentUser.getName());
                                    order.setUserPhone(Common.currentUser.getPhone());
                                    order.setShippingAddress(address);
                                    order.setComment(comment);

                                    if (currentLocation != null) {
                                        order.setLat(currentLocation.getLatitude());
                                        order.setLng(currentLocation.getLongitude());
                                    } else {
                                        order.setLat(-0.1f);
                                        order.setLng(-0.1f);
                                    }

                                    order.setCartItemList(cartItems);
                                    order.setTotalPayment(totalPrice);
                                    order.setDiscount(0);
                                    order.setFinalPayment(finalPrice);
                                    order.setCod(true);
                                    order.setTransactionId("Cash On Delivery");

                                    //Submit this order object to firebase
                                    syncLocalTimeWithGlobaltiem(order);

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (!e.getMessage().contains("Query returned empty result set"))
                                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });

                }, throwable -> {
                    Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void syncLocalTimeWithGlobaltiem(OrderModel order) {
        final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long offset = dataSnapshot.getValue(Long.class);
                long estimatedServerTimeMS = System.currentTimeMillis() + offset;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                Date resultDate = new Date(estimatedServerTimeMS);
                Log.d("TEST_DATE", "" + sdf.format(resultDate));

                listener.onLoadTimeSuccess(order, estimatedServerTimeMS);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onLoadTimeFailed(databaseError.getMessage());
            }
        });
    }

    public void writeOrderToFirebase(OrderModel order) {
        FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                .child(Common.createOrderNumber()) // Create order number with only digit
                .setValue(order)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                }).addOnCompleteListener(task -> {
            //write success
            cartDataSource.clearCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            Map<String, String> notiData = new HashMap<>();
                            notiData.put(Common.NOTI_TITLE, "New Order");
                            notiData.put(Common.NOTI_CONTENT, "Kamu Mempunyai order dari" + Common.currentUser.getPhone());

                            FCMSendData sendData = new FCMSendData(Common.createTopicOrder(), notiData);

                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(fcmRespone -> {
                                        Toast.makeText(getContext(), "OrderModel Placed Successfully", Toast.LENGTH_SHORT).show();
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                    }, throwable -> {
                                        Toast.makeText(getContext(), "Pesanan Terkirim, tapi gagal mengirim notification", Toast.LENGTH_SHORT).show();
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                    }));

//                            //clean sukses
//                            Toast.makeText(getContext(), "Order Success", Toast.LENGTH_SHORT).show();
//                            EventBus.getDefault().postSticky(new CounterCartEvent(true));

                        }

                        @Override
                        public void onError(Throwable e) {

                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });

        });
    }

    private String getAddressFromLatLng(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result = "";
        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            } else
                result = "address not found";
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        cartViewModel =
                ViewModelProviders.of(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        listener = this;
        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(getViewLifecycleOwner(), cartItems -> {
            if (cartItems == null || cartItems.isEmpty()) {
                recycler_cart.setVisibility(View.GONE);
                group_place_holder.setVisibility(View.GONE);
                txt_empty_cart.setVisibility(View.VISIBLE);
            } else {
                recycler_cart.setVisibility(View.VISIBLE);
                group_place_holder.setVisibility(View.VISIBLE);
                txt_empty_cart.setVisibility(View.GONE);

                adapter = new MyCartAdapter(getContext(), cartItems);
                recycler_cart.setAdapter(adapter);
            }

        });
        unbinder = ButterKnife.bind(this, root);
        initViews();
        initLocation();
        return root;

    }

    private void initLocation() {
        buildLocationRequest();
        buildLocationCallback();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    private void initViews() {
        initPlaceClient();
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());
        setHasOptionsMenu(true);

        EventBus.getDefault().postSticky(new HideFABCart(true));
        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwiperHelper mySwiperHelper = new MySwiperHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            cartDataSource.deleteCartItems(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {
                                            adapter.notifyItemRemoved(pos);
                                            sumAllItemInCart(); //update total price
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true)); //update FAB
                                            Toast.makeText(getContext(), "Deleted Item from cart Success", Toast.LENGTH_SHORT).show();

                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                                        }
                                    });
                        }));
            }
        };

        sumAllItemInCart();
    }

    private void initPlaceClient() {
        Places.initialize(getContext(),getString(R.string.google_maps_key));
        placesClient = Places.createClient(getContext());
    }

    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aDouble) {
                        txt_total_price.setText(new StringBuilder("Total: RP").append(aDouble));

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!e.getMessage().contains("Query Returned empty"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false); //hide home menu already inflate
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart) {
            cartDataSource.clearCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            Toast.makeText(getContext(), "Clear Cart Success", Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        calculateTotalPrice();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().postSticky(new HideFABCart(false));
        cartViewModel.onStop();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onStop();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event) {
        if (event.getCartItem() != null) {
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState); //fix erro recycler after update

                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), "[UPDATE CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
        }
    }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: RP")
                                .append(Common.formatPrize(price)));

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!e.getMessage().contains("Query returned empty result set"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    @Override
    public void onLoadTimeSuccess(OrderModel order, long estimateTimeInMs) {
        order.setCreateDate(estimateTimeInMs);
        order.setOrderStatus(0);
        writeOrderToFirebase(order);

    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }
}
