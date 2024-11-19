package com.example.clockit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.auth.FirebaseAuth;

public class HelpFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private EditText firstNameInput, lastNameInput, emailInput, orgInput, descriptionInput;
    private Spinner subjectSpinner;
    private Button submitButton;

    public HelpFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);

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
                loadFragment(new AdminFragment());
            } else if (itemId == R.id.nav_students) { // Use StudentsFragment instead
                loadFragment(new StudentsFragment());
            } else if (itemId == R.id.nav_announcements) {
                loadFragment(new AnnouncementsFragment());
            } else if (itemId == R.id.nav_card_assign) {
                loadFragment(new CardAssignFragment());
            } else if (itemId == R.id.nav_add_classes) {
                Intent intent = new Intent(getActivity(), AddClassActivity.class);
                startActivity(intent);
            } else if (itemId == R.id.nav_help) {
                loadFragment(new HelpFragment());
            } else if (itemId == R.id.nav_logout) { // Handle logout
                FirebaseAuth.getInstance().signOut();
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
        subjectSpinner = view.findViewById(R.id.subjectSpinner);  // Subject dropdown (Spinner)
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
        String emailSubject = "Support Request: " + selectedSubject;
        String emailBody = "First Name: " + firstName + "\n"
                + "Last Name: " + lastName + "\n"
                + "Email: " + email + "\n"
                + "Organization: " + organization + "\n"
                + "Subject: " + selectedSubject + "\n\n"
                + "Description:\n" + description;

        // Create email intent
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822"); // Set MIME type to email
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
        if (getActivity() != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}
