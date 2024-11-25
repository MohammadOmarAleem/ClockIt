package com.example.clockit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

public class StudentHelpFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private EditText firstNameInput, lastNameInput, emailInput, orgInput, descriptionInput;
    private Spinner subjectSpinner;
    private Button submitButton;

    public StudentHelpFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_help, container, false);

        // Set up Toolbar and DrawerLayout
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        drawerLayout = view.findViewById(R.id.drawer_layout);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
        }

        // Set up drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                activity, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set up NavigationView
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new UserFragment());
            } else if (itemId == R.id.nav_student_help) {
                loadFragment(new StudentHelpFragment());
            }else if (itemId == R.id.student_profile) {

                loadFragment(new StudentProfile());
            }
            else if (itemId == R.id.attend_log) {
                loadFragment(new StudentAttendanceLogFragment());
            } else if (itemId == R.id.nav_logout) {
                loadFragment(new LoginFragment());
                Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        // Initialize UI elements for the help form
        firstNameInput = view.findViewById(R.id.firstNameInput);
        lastNameInput = view.findViewById(R.id.lastNameInput);
        emailInput = view.findViewById(R.id.emailInput);
        orgInput = view.findViewById(R.id.orgInput);
        subjectSpinner = view.findViewById(R.id.subjectSpinner);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        submitButton = view.findViewById(R.id.submitButton);

        // Set up click listener for the Submit button
        submitButton.setOnClickListener(v -> handleSubmit());

        return view;
    }

    private void handleSubmit() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String organization = orgInput.getText().toString().trim();
        String selectedSubject = subjectSpinner.getSelectedItem().toString();
        String description = descriptionInput.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(firstName)) {
            Toast.makeText(getContext(), "Please enter your first name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            Toast.makeText(getContext(), "Please enter your last name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            Toast.makeText(getContext(), "Please describe your issue", Toast.LENGTH_SHORT).show();
            return;
        }

        // Compose email subject and body
        String emailSubject = "Student Support Request: " + selectedSubject;
        String emailBody = "First Name: " + firstName + "\n"
                + "Last Name: " + lastName + "\n"
                + "Email: " + email + "\n"
                + "Organization: " + organization + "\n"
                + "Subject: " + selectedSubject + "\n\n"
                + "Description:\n" + description;

        // Create email intent
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(android.net.Uri.parse("mailto:")); // Only email apps should handle this
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"clockit390@gmail.com"}); // Replace with your support email
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "No email client installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
