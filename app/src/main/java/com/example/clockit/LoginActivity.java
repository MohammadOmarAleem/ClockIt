package com.example.clockit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class LoginActivity extends Fragment {
    private Button userButton, adminButton, forgotPasswordButton, signUpButton;

    public LoginActivity() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_login, container, false);

        // Initialize buttons from the layout
        userButton = view.findViewById(R.id.userButton);
        adminButton = view.findViewById(R.id.adminButton);
        forgotPasswordButton = view.findViewById(R.id.forgotPasswordButton);
        signUpButton = view.findViewById(R.id.signUpButton);

        // Set onClick listeners for buttons
        userButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUserSelected();
            }
        });

        adminButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAdminSelected();
            }
        });

        // Handle "Forgot Password" button click

        // Handle "Forgot Password" button click
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ForgotPassActivity
                Intent intent = new Intent(getActivity(), ForgotPassActivity.class);
                startActivity(intent);
            }
        });

        // Handle "Sign Up" button click
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to RegisterActivity
                RegisterActivity registerFragment = new RegisterActivity();
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, registerFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return view;
    }

    // Method for when User button is clicked
    public void onUserSelected() {
        userButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.black));
        userButton.setTextColor(getResources().getColor(android.R.color.white));
        adminButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.light_gray));
        adminButton.setTextColor(getResources().getColor(R.color.gray));
    }

    // Method for when Admin button is clicked
    public void onAdminSelected() {
        adminButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.black));
        adminButton.setTextColor(getResources().getColor(android.R.color.white));
        userButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.light_gray));
        userButton.setTextColor(getResources().getColor(R.color.gray));
    }

}
