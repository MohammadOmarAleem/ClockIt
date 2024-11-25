package com.example.clockit;

import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class StudentProfile extends Fragment {

    private DrawerLayout drawerLayout;
    private TextView welcomeMessage, dateTime, studentIdTextView;
    private TextView student_Name,student_cardUID,student_id,student_email;
    private Button clockStatusButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabase;

    private Handler handler;
    private Runnable updateTask;

    public StudentProfile() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.student_profile, container, false);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        userDatabase = FirebaseDatabase.getInstance().getReference("Users");
        student_Name = view.findViewById(R.id.student_Name);
        student_cardUID = view.findViewById(R.id.student_cardUID);
        student_id = view.findViewById(R.id.student_id);
        student_email = view.findViewById(R.id.student_email);

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
            } else if (itemId == R.id.student_profile) {

                loadFragment(new StudentProfile());
            } else if (itemId == R.id.attend_log) {
                loadFragment(new StudentAttendanceLogFragment());
            } else if (itemId == R.id.nav_logout) {
                loadFragment(new LoginFragment());
                Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        fetchAndDisplayUserDetails();
        return view;
    }

    private void fetchAndDisplayUserDetails() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String studentId = snapshot.child("studentId").getValue(String.class);
                        String cardUID = snapshot.child("cardUid").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);


                        student_Name.setText(name);
                       student_id.setText(studentId);
                       student_email.setText(email);
                       student_cardUID.setText(cardUID);

                    } else {
                        Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
        }
    }



    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }




}
