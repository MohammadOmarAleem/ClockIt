package com.example.clockit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterFragment extends Fragment {

    private EditText emailField, nameField, usernameField, passwordField, confirmPasswordField;
    private EditText studentIdField, cardUidField; // Fields for Student ID and Card UID
    private RadioGroup accountTypeRadioGroup;
    private Button signUpButton, logInButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;  // Firebase Realtime Database

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Initialize fields and buttons
        emailField = view.findViewById(R.id.et_email);
        nameField = view.findViewById(R.id.et_fullname); // Name field
        usernameField = view.findViewById(R.id.et_username); // Username field
        passwordField = view.findViewById(R.id.et_password);
        confirmPasswordField = view.findViewById(R.id.et_confirm_password);
        studentIdField = view.findViewById(R.id.et_student_id); // Student ID field
        cardUidField = view.findViewById(R.id.et_card_uid); // Card UID field
        accountTypeRadioGroup = view.findViewById(R.id.accountTypeRadioGroup);
        signUpButton = view.findViewById(R.id.btn_login);
        logInButton = view.findViewById(R.id.logInButton);

        // Initialize FirebaseAuth instance
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Handle sign-up logic
        signUpButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String name = nameField.getText().toString().trim(); // Capture name
            String username = usernameField.getText().toString().trim(); // Capture username
            String password = passwordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();
            String studentId = studentIdField.getText().toString().trim();
            String cardUid = cardUidField.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(name) || TextUtils.isEmpty(username) ||
                    TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) ||
                    TextUtils.isEmpty(studentId) || TextUtils.isEmpty(cardUid)) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the selected account type
            int selectedId = accountTypeRadioGroup.getCheckedRadioButtonId();
            RadioButton selectedRadioButton = view.findViewById(selectedId);
            String accountType = selectedRadioButton.getText().toString();

            // Create a user with Firebase Authentication
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();

                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            User userData = new User(name, username, email, accountType, studentId, cardUid);

                            // Store user data in Firebase Realtime Database under the user's unique ID
                            databaseReference.child(user.getUid()).setValue(userData)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(getContext(), "User data saved successfully!", Toast.LENGTH_SHORT).show();
                                            // Redirect to login fragment
                                            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                                            transaction.replace(android.R.id.content, new LoginFragment());
                                            transaction.addToBackStack(null);
                                            transaction.commit();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(getContext(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Redirect to login on button click
        logInButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, new LoginFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }
}
