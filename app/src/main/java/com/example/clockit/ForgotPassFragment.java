package com.example.clockit;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassFragment extends Fragment {
    private EditText emailField;
    private Button forgotPasswordButton, signUpButton;
    private FirebaseAuth firebaseAuth;  // FirebaseAuth instance

    public ForgotPassFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_forgotpass, container, false);

        // Initialize the fields and buttons from the layout
        emailField = view.findViewById(R.id.emailField);
        forgotPasswordButton = view.findViewById(R.id.forgotPasswordButton);
        signUpButton = view.findViewById(R.id.signUpButton);

        // Initialize FirebaseAuth instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Handle "Forgot Password" button click
        forgotPasswordButton.setOnClickListener(v -> handleForgotPassword());

        // Handle "Sign Up" button click
        signUpButton.setOnClickListener(v -> {
            // Replace the current fragment with the RegisterFragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, new RegisterFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    // Method to handle the "Forgot Password" logic
    private void handleForgotPassword() {
        String email = emailField.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
        } else {
            // Send password reset email using FirebaseAuth
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Reset link sent to " + email, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to send reset email", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
