package com.example.clockit;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Set the layout with fragment_container

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        SharedPreferences prefs = getSharedPreferences("ClockItPrefs", MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("rememberMe", false);

        if (currentUser != null && rememberMe) {
            String accountType = prefs.getString("accountType", null);
            if (accountType != null) {
                loadFragmentBasedOnAccountType(accountType);
            } else {
                fetchAccountType(currentUser.getUid());
            }
        } else {
            clearLoginState();
            loadFragment(new LoginFragment());
        }
    }

    public void fetchAccountType(String userId) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String accountType = dataSnapshot.child("accountType").getValue(String.class);

                    SharedPreferences.Editor editor = getSharedPreferences("ClockItPrefs", MODE_PRIVATE).edit();
                    editor.putString("accountType", accountType);
                    editor.apply();

                    loadFragmentBasedOnAccountType(accountType);
                } else {
                    clearLoginState();
                    loadFragment(new LoginFragment());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                clearLoginState();
                loadFragment(new LoginFragment());
            }
        });
    }

    private void loadFragmentBasedOnAccountType(String accountType) {
        Fragment fragment;
        if ("Admin".equals(accountType)) {
            fragment = new AdminFragment();
        } else if ("User".equals(accountType)) {
            fragment = new UserFragment();
        } else {
            fragment = new LoginFragment();
        }
        loadFragment(fragment);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);  // Use the new fragment_container ID
        transaction.commit();
    }

    private void clearLoginState() {
        SharedPreferences.Editor editor = getSharedPreferences("ClockItPrefs", MODE_PRIVATE).edit();
        editor.remove("rememberMe");
        editor.remove("accountType");
        editor.apply();
    }
}