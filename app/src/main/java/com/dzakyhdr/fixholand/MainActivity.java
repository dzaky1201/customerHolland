package com.dzakyhdr.fixholand;

import com.dzakyhdr.fixholand.Common.Common;
import com.dzakyhdr.fixholand.Model.UserModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;
import io.reactivex.disposables.CompositeDisposable;


public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private DatabaseReference userRef;
    private List<AuthUI.IdpConfig> providers;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placesFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        Places.initialize(this,getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());
        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCE);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();
        listener = firebaseAuth -> {

            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                checkUserFromFirebase(user);
                            } else {
                                phoneLogin();
                            }

                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Toast.makeText(MainActivity.this, "You must enable this permission to use app", Toast.LENGTH_SHORT).show();

                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                        }
                    }).check();

        };
    }

    private void checkUserFromFirebase(FirebaseUser user) {
        dialog.show();
        userRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Toast.makeText(MainActivity.this, "You already register", Toast.LENGTH_SHORT).show();

                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
                            goToHomeActivity(userModel);
                            finish();
                        } else {
                            showRegisterDialog(user);
                        }

                        dialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill Information");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        EditText edt_name = itemView.findViewById(R.id.edt_name);
        TextView txt_address_detail = itemView.findViewById(R.id.txt_address_detail);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        places_fragment = (AutocompleteSupportFragment)getSupportFragmentManager().findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placesFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected = place;
                txt_address_detail.setText(place.getAddress());
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // set data
        edt_phone.setText(user.getPhoneNumber());


        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setPositiveButton("Register", (dialog, which) -> {

            if (placeSelected != null) {
                if (TextUtils.isEmpty(edt_name.getText().toString())) {
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }
                UserModel userModel = new UserModel();
                userModel.setUid(user.getUid());
                userModel.setName(edt_name.getText().toString());
                userModel.setAddress(txt_address_detail.getText().toString());
                userModel.setPhone(edt_phone.getText().toString());
                userModel.setLat(placeSelected.getLatLng().latitude);
                userModel.setLng(placeSelected.getLatLng().longitude);

                userRef.child(user.getUid()).setValue(userModel)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Congretulation ! Register succesfull", Toast.LENGTH_SHORT).show();
                                    goToHomeActivity(userModel);
                                }
                            }
                        });
            }
            else
            {
                Toast.makeText(this, "Please Select Address", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(itemView);

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(places_fragment);
            fragmentTransaction.commit();
        });
        dialog.show();
    }

    private void goToHomeActivity(UserModel userModel) {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Common.currentUser = userModel;
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();

                }).addOnCompleteListener(task -> {
                     Common.currentUser = userModel;
                     Common.updateToken(MainActivity.this,task.getResult().getToken());
                     startActivity(new Intent(MainActivity.this, HomeActivity.class));
                     finish();

        });

    }

    private void phoneLogin() {
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(), APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, "Failed to sign in", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
