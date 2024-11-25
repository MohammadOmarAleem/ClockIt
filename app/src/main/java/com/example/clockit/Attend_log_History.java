package com.example.clockit;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import androidx.annotation.NonNull;
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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class Attend_log_History extends Fragment {

    private TableLayout attendanceTable, absentTable;
    private Spinner classSpinner;
    private EditText dateInput;
    private Button filterButton;
    private DrawerLayout drawerLayout;
    private DatabaseReference firebaseReference, usersReference;

    private static final String TAG = "Attend_log_History";
    private static final String GOOGLE_SHEET_URL = "https://script.google.com/macros/s/AKfycbzTNcTq6jIsro4B0Rz6CtHuEs-XGDw7C6S1wRgCIxgtXG3yIIpfZOPHNk8eW1OxQ9ddZw/exec";

    private String selectedDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase references
        firebaseReference = FirebaseDatabase.getInstance().getReference("classes");
        usersReference = FirebaseDatabase.getInstance().getReference("Users");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_log_attend, container, false);

        // Initialize UI elements
        attendanceTable = view.findViewById(R.id.attendanceTable);
        absentTable = view.findViewById(R.id.absentTable);
        classSpinner = view.findViewById(R.id.classSpinner);
        dateInput = view.findViewById(R.id.dateInput);
        filterButton = view.findViewById(R.id.filterButton);

        // Toolbar setup
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        // Drawer layout setup
        drawerLayout = view.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                requireActivity(), drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Navigation View setup
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                transaction.replace(R.id.fragment_container, new AdminFragment()).commit();
            } else if (itemId == R.id.nav_students) {
                transaction.replace(R.id.fragment_container, new StudentsFragment()).commit();
            } else if (itemId == R.id.nav_announcements) {
                transaction.replace(R.id.fragment_container, new AnnouncementsFragment()).commit();
            } else if (itemId == R.id.nav_card_assign) {
                transaction.replace(R.id.fragment_container, new CardAssignFragment()).commit();
            } else if (itemId == R.id.nav_add_classes) {
                transaction.replace(R.id.fragment_container, new AddClassActivity()).commit();
            } else if (itemId == R.id.nav_help) {
                transaction.replace(R.id.fragment_container, new HelpFragment()).commit();
            } else if (itemId == R.id.attend_log) {
                transaction.replace(R.id.fragment_container, new Attend_log_History()).commit();
            } else if (itemId == R.id.nav_edit_classes) {
                transaction.replace(R.id.fragment_container, new EditClassActivity()).commit();
            } else if (itemId == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                transaction.replace(R.id.fragment_container, new LoginFragment()).commit();
                Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Fetch classes from Firebase
        fetchClassesFromFirebase();

        // Add Date Picker to dateInput
        dateInput.setOnClickListener(v -> showDatePicker());

        // Set up filter button to fetch attendance
        filterButton.setOnClickListener(v -> fetchAttendanceFromGoogleSheets());

        return view;
    }
    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Default date selection
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Format the selected date and set it in the date input field
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Use UTC time zone for consistent results

            String formattedDate = dateFormat.format(new Date(selection));
            dateInput.setText(formattedDate); // Populate the date input field
        });

        datePicker.show(requireActivity().getSupportFragmentManager(), "DATE_PICKER");
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null); // Optional: add to back stack if needed
        transaction.commit();
    }
    private void fetchClassesFromFirebase() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fetching classes...");
        progressDialog.show();

        String currentProfessorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentProfessorId == null) {
            progressDialog.dismiss();
            Toast.makeText(getContext(), "Professor ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query Firebase for classes where adminId matches the logged-in professor's UID
        firebaseReference.orderByChild("adminId").equalTo(currentProfessorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();
                List<String> classes = new ArrayList<>();

                // Iterate over the results and add class names to the list
                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    String className = classSnapshot.child("className").getValue(String.class);
                    if (className != null) {
                        classes.add(className);
                    }
                }

                // Check if the professor has any classes
                if (classes.isEmpty()) {
                    Toast.makeText(getContext(), "No classes found for this professor.", Toast.LENGTH_SHORT).show();
                }

                // Populate the spinner with the list of classes
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, classes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                classSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error fetching classes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void fetchAttendanceFromGoogleSheets() {
        String selectedClass = classSpinner.getSelectedItem() != null ? classSpinner.getSelectedItem().toString() : "";
        selectedDate = dateInput.getText().toString().trim();

        // Check if class and date are selected
        if (selectedClass.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(getContext(), "Please select a class and a date.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog while fetching data
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fetching attendance...");
        progressDialog.show();

        // Query Firebase for class data
        firebaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();

                // Initialize start and end time variables
                String startTime = null, endTime = null;

                // Iterate through classes to find the selected one
                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    String className = classSnapshot.child("className").getValue(String.class);
                    if (className != null && className.equals(selectedClass)) {
                        startTime = convertTo24HourFormat(classSnapshot.child("startTime").getValue(String.class));
                        endTime = convertTo24HourFormat(classSnapshot.child("endTime").getValue(String.class));
                        break;
                    }
                }

                // Check if timings are available
                if (startTime == null || endTime == null) {
                    Toast.makeText(getContext(), "Class timings not available for the selected class.", Toast.LENGTH_SHORT).show();
                } else {
                    // Call filterAttendance with retrieved timings
                    filterAttendance(selectedClass, selectedDate, startTime, endTime);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(getContext(), "Error fetching class timings from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void filterAttendance(String selectedClass, String selectedDate, String startTime, String endTime) {
        try {
            String encodedClass = URLEncoder.encode(selectedClass, "UTF-8");
            String encodedDate = URLEncoder.encode(selectedDate, "UTF-8");
            String attendanceUrl = GOOGLE_SHEET_URL + "?class=" + encodedClass + "&date=" + encodedDate;

            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Filtering attendance...");
            progressDialog.show();

            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, attendanceUrl, null,
                    response -> {
                        progressDialog.dismiss();
                        filterAttendanceByTime(response, startTime, endTime);
                    },
                    error -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error Response: " + error.getMessage());
                        Toast.makeText(getContext(), "Error fetching attendance.", Toast.LENGTH_SHORT).show();
                    });

            jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(
                    5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(jsonArrayRequest);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Encoding error: " + e.getMessage());
            Toast.makeText(getContext(), "Error encoding parameters.", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterAttendanceByTime(JSONArray response, String startTime, String endTime) {
        attendanceTable.removeAllViews(); // Clear previous data
        absentTable.removeAllViews(); // Clear absent table

        // Add header rows
        addHeaderRow(attendanceTable, "Name", "Student ID", "Check-In", "Check-Out");
        addHeaderRow(absentTable, "Name", "Student ID");

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Set<String> presentUIDs = new HashSet<>();

        // Populate attendance table and collect present UIDs
        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject jsonObject = response.getJSONObject(i);
                String cardUID = jsonObject.optString("UID", "");
                String checkIn = jsonObject.optString("TimeIn", "");
                String checkOut = jsonObject.optString("TimeOut", "");
                String date = jsonObject.optString("Date", "");

                if (date.equals(selectedDate) && timeFormat.parse(checkIn).compareTo(timeFormat.parse(startTime)) >= 0 &&
                        timeFormat.parse(checkOut).compareTo(timeFormat.parse(endTime)) <= 0) {
                    presentUIDs.add(cardUID);
                    addRowToAttendanceTable(jsonObject);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing attendance data: " + e.getMessage());
            }
        }

        // Populate absent table by comparing Firebase UIDs
        usersReference.orderByChild("accountType").equalTo("User").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Student student = userSnapshot.getValue(Student.class);
                    if (student != null && student.getStudentId() != null && !presentUIDs.contains(student.getStudentId())) {
                        addRowToAbsentTable(student);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching users: " + error.getMessage());
            }
        });
    }

    private void addRowToAttendanceTable(JSONObject jsonObject) {
        TableRow row = new TableRow(getContext());
        addTextViewToRow(row, jsonObject.optString("Name", ""));
        addTextViewToRow(row, jsonObject.optString("UID", ""));
        addTextViewToRow(row, jsonObject.optString("TimeIn", ""));
        addTextViewToRow(row, jsonObject.optString("TimeOut", ""));
        attendanceTable.addView(row);
    }

    private void addRowToAbsentTable(Student student) {
        TableRow row = new TableRow(getContext());
        addTextViewToRow(row, student.getName());
        addTextViewToRow(row, student.getStudentId());
        absentTable.addView(row);
    }

    private void addHeaderRow(TableLayout table, String... headers) {
        TableRow headerRow = new TableRow(getContext());
        headerRow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        for (String header : headers) {
            addTextViewToRow(headerRow, header);
        }
        table.addView(headerRow);
    }

    private String convertTo24HourFormat(String time) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("h:mm a");
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss");
            return outputFormat.format(inputFormat.parse(time));
        } catch (Exception e) {
            Log.e(TAG, "Error converting time format: " + e.getMessage());
            return null;
        }
    }

    private void addTextViewToRow(TableRow row, String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setPadding(16, 16, 16, 16);
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        row.addView(textView);
    }
}