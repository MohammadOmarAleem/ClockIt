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
import com.android.volley.VolleyError;
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

public class UserFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private TextView welcomeMessage, dateTime, studentIdTextView;
    private Button clockStatusButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabase;

    private Handler handler;
    private Runnable updateTask;

    public UserFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        userDatabase = FirebaseDatabase.getInstance().getReference("Users");

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
        navigationView.setNavigationItemSelectedListener(this::handleNavigation);

        // Initialize UI elements
        welcomeMessage = view.findViewById(R.id.welcomeMessage);
        dateTime = view.findViewById(R.id.dateTime);
        studentIdTextView = view.findViewById(R.id.studentIdTextView);
        clockStatusButton = view.findViewById(R.id.attendanceStatusButton);

        // Initialize Handler
        handler = new Handler();

        // Fetch and display the user's name and student ID
        fetchAndDisplayUserDetails();

        // Update the date and time
        updateDateTime();

        // Start periodic clock status updates
        startPeriodicClockStatusUpdates();

        return view;
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            stopPeriodicUpdates();
            loadFragment(new UserFragment());
        } else if (itemId == R.id.nav_student_help) {
            stopPeriodicUpdates();
            loadFragment(new StudentHelpFragment());
        } else if (itemId == R.id.student_profile) {
            stopPeriodicUpdates();
            loadFragment(new StudentProfile());
        } else if (itemId == R.id.attend_log) {
            stopPeriodicUpdates();
            loadFragment(new StudentAttendanceLogFragment());
        } else if (itemId == R.id.nav_logout) {
            firebaseAuth.signOut();
            stopPeriodicUpdates();
            loadFragment(new LoginFragment());
            Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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

                        // Display name and student ID
                        welcomeMessage.setText(name != null ? "Welcome, " + name + "!" : "Welcome, Student!");
                        studentIdTextView.setText(studentId != null ? "Student ID: " + studentId : "Student ID: N/A");
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

    private void updateDateTime() {
        String currentDateTime = DateFormat.format("EEEE, MMM d, yyyy - h:mm a", Calendar.getInstance().getTime()).toString();
        dateTime.setText(currentDateTime);
    }

    private void checkClockStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        userDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String cardUid = snapshot.child("cardUid").getValue(String.class);
                    if (cardUid != null) {
                        checkAttendanceLog(cardUid);
                    } else {
                        Toast.makeText(getContext(), "Card UID not found for the user.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "User data not found in Firebase.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAttendanceLog(String cardUid) {
        String url = "https://script.google.com/macros/s/AKfycbzTNcTq6jIsro4B0Rz6CtHuEs-XGDw7C6S1wRgCIxgtXG3yIIpfZOPHNk8eW1OxQ9ddZw/exec";

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    String todayDate = DateFormat.format("dd/MM/yyyy", Calendar.getInstance().getTime()).toString();
                    boolean isClockedIn = false;

                    for (int i = response.length() - 1; i >= 0; i--) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        String uid = jsonObject.optString("UID", "");
                        String date = jsonObject.optString("Date", "");

                        if (uid.equals(cardUid) && date.equals(todayDate)) {
                            String timeOut = jsonObject.optString("TimeOut", "");

                            if (timeOut.isEmpty() || timeOut.equals("Invalid Time") || timeOut.equals("88:88:88") || timeOut.equals("19:00:00")) {
                                isClockedIn = true;
                            } else {
                                isClockedIn = false;
                            }
                            break;
                        }
                    }

                    updateClockStatusUI(isClockedIn);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error processing attendance data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, error -> Toast.makeText(getContext(), "Failed to fetch attendance log: " + error.getMessage(), Toast.LENGTH_SHORT).show());

        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonArrayRequest);
    }

    private void updateClockStatusUI(boolean isClockedIn) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isClockedIn) {
                    clockStatusButton.setText("Clocked In");
                    clockStatusButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                } else {
                    clockStatusButton.setText("Clocked Out");
                    clockStatusButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                }
            });
        }
    }

    private void startPeriodicClockStatusUpdates() {
        updateTask = new Runnable() {
            @Override
            public void run() {
                checkClockStatus();
                handler.postDelayed(this, 10000); // Update every 10 seconds
            }
        };
        handler.post(updateTask);
    }

    private void stopPeriodicUpdates() {
        if (handler != null && updateTask != null) {
            handler.removeCallbacks(updateTask);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPeriodicUpdates();
    }
}
