package com.example.clockit;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class StudentAttendanceLogFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private TableLayout attendanceTable;
    private Handler handler;
    private Runnable periodicTask;

    private DatabaseReference classDatabase;
    private List<ClassModel> classList; // To store classes fetched from Firebase

    public StudentAttendanceLogFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_log, container, false);
        setHasOptionsMenu(true);

        // Toolbar setup
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setHomeButtonEnabled(true);

        }

        // Drawer layout setup
        drawerLayout = view.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(activity, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Navigation view setup
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            handleMenuItemClick(item.getItemId());
            return true;
        });

        attendanceTable = view.findViewById(R.id.attendanceTable);
        handler = new Handler();

        // Add table header once

        // Initialize Firebase class database
        classDatabase = FirebaseDatabase.getInstance().getReference("classes");
        classList = new ArrayList<>();
        fetchClassesFromFirebase(); // Fetch class data from Firebase

        // Remove the initial call to startPeriodicFetching(null);
        // It will be called after classes are fetched
        // startPeriodicFetching(null);

        return view;
    }



    private void fetchClassesFromFirebase() {
        classDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classList.clear();
                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    ClassModel classModel = classSnapshot.getValue(ClassModel.class);
                    if (classModel != null) {
                        classList.add(classModel);
                        // Add logging here
                        Log.d("FetchClasses", "Fetched class: " + classModel.getClassName());
                    }
                }
                // Log the total number of classes fetched
                Log.d("FetchClasses", "Total classes fetched: " + classList.size());

                // Start fetching attendance data after classes are fetched
                startPeriodicFetching(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch class data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPeriodicFetching(String date) {
        stopPeriodicFetching();
        periodicTask = new Runnable() {
            @Override
            public void run() {
                fetchAttendanceLog(date);
                handler.postDelayed(this, 5000); // Fetch every 5 seconds
            }
        };
        handler.post(periodicTask);
    }

    private void stopPeriodicFetching() {
        if (handler != null && periodicTask != null) {
            handler.removeCallbacks(periodicTask);
        }
    }

    private void fetchAttendanceLog(String dateFilter) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentUID = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(studentUID);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String cardUid = snapshot.child("cardUid").getValue(String.class);
                    if (cardUid != null) {
                        fetchAttendanceDataFromSheet(cardUid, dateFilter);
                    } else {
                        Toast.makeText(getContext(), "Card UID not found for the user", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "User data not found in Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAttendanceDataFromSheet(String cardUid, String dateFilter) {
        String url = "https://script.google.com/macros/s/AKfycbwRY8bc53awrHD3ES32-Er1pVc2eR-KcEIlAR9AfeaPstwG23Z69HnNQK8mzDVlSCMT3A/exec";

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
            try {
                // Remove data rows, keep header
                attendanceTable.removeAllViews();

                for (int i = 0; i < response.length(); i++) {
                    JSONObject jsonObject = response.getJSONObject(i);
                    String uid = jsonObject.optString("UID", "N/A");
                    String date = jsonObject.optString("Date", "N/A");

                    if (uid.equals(cardUid) && (dateFilter == null || dateFilter.equals(date))) {
                        String timeIn = jsonObject.optString("TimeIn", "N/A");
                        String timeOut = jsonObject.optString("TimeOut", "N/A");
                        String matchedClass = matchClassWithTime(timeIn, timeOut, date); // Pass attendance date
                        addAttendanceRow(jsonObject, matchedClass);
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Error processing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, error -> Toast.makeText(getContext(), "Failed to fetch attendance data: " + error.getMessage(), Toast.LENGTH_SHORT).show());
        requestQueue.add(jsonArrayRequest);
    }

    // Updated TimeComponents class to handle 24-hour format
    private static class TimeComponents {
        int hours;
        int minutes;

        TimeComponents(String timeStr, String format) throws ParseException {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
            Date date = sdf.parse(timeStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            this.hours = calendar.get(Calendar.HOUR_OF_DAY);
            this.minutes = calendar.get(Calendar.MINUTE);
        }

        int toMinutesSinceMidnight() {
            return hours * 60 + minutes;
        }
    }

    private String matchClassWithTime(String timeIn, String timeOut, String attendanceDate) {
        // Check if classList is empty
        if (classList.isEmpty()) {
            Log.e("AttendanceLog", "classList is empty. Classes may not have been fetched yet.");
            return "No Matching Class";
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

        try {
            // Parse the attendance date to get day of week
            Date attendanceDateObj = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(attendanceDate);
            String attendanceDay = dayFormat.format(attendanceDateObj).trim();

            // Parse attendance times in "HH:mm:ss" format
            TimeComponents checkInTime = new TimeComponents(timeIn, "HH:mm:ss");
            TimeComponents checkOutTime = new TimeComponents(timeOut, "HH:mm:ss");

            // Convert to minutes for easier comparison
            int checkInMinutes = checkInTime.toMinutesSinceMidnight();
            int checkOutMinutes = checkOutTime.toMinutesSinceMidnight();

            // Debug logging
            Log.d("AttendanceLog", String.format("Checking attendance for day: %s, Time: %s - %s",
                    attendanceDay, timeIn, timeOut));
            Log.d("AttendanceLog", String.format("Minutes since midnight: Check-in: %d, Check-out: %d",
                    checkInMinutes, checkOutMinutes));

            // Define acceptable windows (15 minutes before/after)
            final int ACCEPTABLE_WINDOW = 15;

            for (ClassModel classModel : classList) {
                try {
                    // Log class details
                    Log.d("MatchingClass", "Attempting to match class: " + classModel.getClassName());
                    Log.d("MatchingClass", "Class days: " + classModel.getDays());
                    Log.d("MatchingClass", "Class start time: " + classModel.getStartTime());
                    Log.d("MatchingClass", "Class end time: " + classModel.getEndTime());

                    // Parse class times with the correct format
                    TimeComponents classStartTime = new TimeComponents(classModel.getStartTime(), "hh:mm a");
                    TimeComponents classEndTime = new TimeComponents(classModel.getEndTime(), "hh:mm a");

                    int classStartMinutes = classStartTime.toMinutesSinceMidnight();
                    int classEndMinutes = classEndTime.toMinutesSinceMidnight();

                    // Debug logging for class times
                    Log.d("AttendanceLog", String.format("Class: %s, Days: %s, Time: %s - %s",
                            classModel.getClassName(),
                            classModel.getDays().toString(),
                            classModel.getStartTime(),
                            classModel.getEndTime()));

                    // Ensure day names are in the same format
                    List<String> classDays = new ArrayList<>();
                    for (String day : classModel.getDays()) {
                        classDays.add(day.trim());
                    }

                    // Check if the class is scheduled for this day
                    if (classDays.contains(attendanceDay)) {
                        // Check if check-in is within acceptable window of class start time
                        boolean validCheckIn = Math.abs(checkInMinutes - classStartMinutes) <= ACCEPTABLE_WINDOW;

                        // Check if check-out is within acceptable window of class end time
                        boolean validCheckOut = Math.abs(checkOutMinutes - classEndMinutes) <= ACCEPTABLE_WINDOW;

                        Log.d("AttendanceLog", String.format("Class %s: Valid check-in: %b, Valid check-out: %b",
                                classModel.getClassName(), validCheckIn, validCheckOut));

                        if (validCheckIn && validCheckOut) {
                            return classModel.getClassName();
                        }
                    }
                } catch (ParseException e) {
                    Log.e("AttendanceLog", "Error parsing class times: " + e.getMessage());
                    continue; // Skip this class if times can't be parsed
                }
            }
        } catch (ParseException e) {
            Log.e("AttendanceLog", "Error parsing attendance data: " + e.getMessage());
        }

        return "No Matching Class";
    }

    private void addAttendanceRow(JSONObject jsonObject, String matchedClass) throws JSONException {
        TableRow newRow = new TableRow(getContext());
        newRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView dateView = createTextView(jsonObject.optString("Date", "N/A"));
        TextView timeInView = createTextView(jsonObject.optString("TimeIn", "N/A"));

        String timeOut = jsonObject.optString("TimeOut", "N/A");
        if (timeOut.isEmpty() || timeOut.equals("19:00:00") || timeOut.equals("Invalid Time")) {
            timeOut = "N/A";
        }
        TextView timeOutView = createTextView(timeOut);

        TextView classView = createTextView(matchedClass);

        newRow.addView(dateView);
        newRow.addView(timeInView);
        newRow.addView(timeOutView);
        newRow.addView(classView);

        attendanceTable.addView(newRow);
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(8, 8, 8, 8);
        return textView;
    }

    private TextView createHeaderTextView(String text) {
        TextView textView = createTextView(text);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        return textView;
    }

    private void handleMenuItemClick(int itemId) {
        drawerLayout.closeDrawer(GravityCompat.START);
        if (itemId == R.id.nav_home) {
            stopPeriodicFetching();
            loadFragment(new UserFragment());
        } else if (itemId == R.id.nav_student_help) {
            stopPeriodicFetching();
            loadFragment(new StudentHelpFragment());
        } else if (itemId == R.id.student_profile) {
            stopPeriodicFetching();
            loadFragment(new StudentProfile());
        } else if (itemId == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            stopPeriodicFetching();
            Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            loadFragment(new LoginFragment());
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
        stopPeriodicFetching();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.student_filter_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_all) {
            startPeriodicFetching(null);
            return true;
        } else if (item.getItemId() == R.id.show_day) {
            showDatePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat utcFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String utcDate = utcFormat.format(new Date(selection));
            startPeriodicFetching(utcDate);
        });

        datePicker.show(requireActivity().getSupportFragmentManager(), "DATE_PICKER");
    }
}
