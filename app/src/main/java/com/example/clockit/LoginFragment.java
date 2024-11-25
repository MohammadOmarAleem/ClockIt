package com.example.clockit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {
    private Button userButton, adminButton, forgotPasswordButton, signUpButton, loginButton;
    private EditText emailField, passwordField;
    private CheckBox rememberMeCheckBox;
    private boolean isAdminSelected = false;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Initialize buttons and fields from the layout
        userButton = view.findViewById(R.id.userButton);
        adminButton = view.findViewById(R.id.adminButton);
        forgotPasswordButton = view.findViewById(R.id.forgotPasswordButton);
        signUpButton = view.findViewById(R.id.signUpButton);
        loginButton = view.findViewById(R.id.loginButton);
        emailField = view.findViewById(R.id.usernameField);
        passwordField = view.findViewById(R.id.passwordField);
        rememberMeCheckBox = view.findViewById(R.id.rememberMe);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Set onClick listeners for buttons
        userButton.setOnClickListener(v -> onUserSelected());
        adminButton.setOnClickListener(v -> onAdminSelected());

        forgotPasswordButton.setOnClickListener(v -> {
            ForgotPassFragment forgotPassFragment = new ForgotPassFragment();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, forgotPassFragment);  // Use fragment_container
            transaction.addToBackStack(null);
            transaction.commit();
        });

        signUpButton.setOnClickListener(v -> {
            RegisterFragment registerFragment = new RegisterFragment();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, registerFragment);  // Use fragment_container
            transaction.addToBackStack(null);
            transaction.commit();
        });

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter your email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                            if (currentUser != null) {
                                boolean rememberMe = rememberMeCheckBox.isChecked();
                                saveLoginState(rememberMe);
                                checkAccountType(currentUser.getUid());
                            }
                        } else {
                            Toast.makeText(getContext(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        return view;
    }

    private void saveLoginState(boolean rememberMe) {
        SharedPreferences prefs = getActivity().getSharedPreferences("ClockItPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("rememberMe", rememberMe);
        editor.apply();
    }

    public void onUserSelected() {
        isAdminSelected = false;
        userButton.setBackgroundColor(getResources().getColor(android.R.color.black));
        adminButton.setBackgroundColor(getResources().getColor(R.color.light_gray));
    }

    public void onAdminSelected() {
        isAdminSelected = true;
        adminButton.setBackgroundColor(getResources().getColor(android.R.color.black));
        userButton.setBackgroundColor(getResources().getColor(R.color.light_gray));
    }

    private void checkAccountType(String userId) {
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String accountType = dataSnapshot.child("accountType").getValue(String.class);
                    if (isAdminSelected && "Admin".equals(accountType)) {
                        saveAccountTypeToPreferences("Admin");
                        redirectToFragment(new AdminFragment());
                    } else if (!isAdminSelected && "User".equals(accountType)) {
                        saveAccountTypeToPreferences("User");
                        redirectToFragment(new UserFragment());
                    } else {
                        Toast.makeText(getContext(), "Account type mismatch. Please select the correct account type.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Account type not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAccountTypeToPreferences(String accountType) {
        SharedPreferences prefs = getContext().getSharedPreferences("ClockItPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("accountType", accountType);
        editor.apply();
    }

    private void redirectToFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);  // Use the new fragment_container ID
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
