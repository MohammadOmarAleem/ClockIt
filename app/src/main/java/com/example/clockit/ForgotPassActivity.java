package com.example.clockit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class ForgotPassActivity extends AppCompatActivity {
    private EditText emailField;
    private Button forgotPasswordButton, signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpass);

        // Initialize the fields and buttons from the layout
        emailField = findViewById(R.id.emailField);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        signUpButton = findViewById(R.id.signUpButton);

        // Handle "Forgot Password" button click
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });

        // Handle "Sign Up" button click
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replace the current fragment with the RegisterFragment
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new RegisterActivity()); // Ensure that R.id.fragment_container exists in your layout
                transaction.addToBackStack(null); // Optional: Adds to back stack for 'back' navigation
                transaction.commit();
            }
        });
    }

    // Method to handle the "Forgot Password" logic
    private void handleForgotPassword() {
        String email = emailField.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(ForgotPassActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
        } else {
            // Logic to send a password reset email
            Toast.makeText(ForgotPassActivity.this, "Password reset link sent to " + email, Toast.LENGTH_SHORT).show();
        }
    }
}