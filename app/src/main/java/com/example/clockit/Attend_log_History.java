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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;

public class Attend_log_History extends Fragment {

    private TableLayout attendanceTable;
    private Spinner classSpinner;
    private EditText dateInput;
    private Button filterButton;

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

        attendanceTable = view.findViewById(R.id.attendanceTable);
        classSpinner = view.findViewById(R.id.classSpinner);
        dateInput = view.findViewById(R.id.dateInput);
        filterButton = view.findViewById(R.id.filterButton);

        fetchClassesFromFirebase();
        filterButton.setOnClickListener(v -> fetchAttendanceFromGoogleSheets());
        return view;
    }

    private void fetchClassesFromFirebase() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fetching classes...");
        progressDialog.show();

        firebaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();
                List<String> classes = new ArrayList<>();
                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    String className = classSnapshot.child("className").getValue(String.class);
                    if (className != null) {
                        classes.add(className);
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, classes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                classSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(getContext(), "Error fetching classes.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAttendanceFromGoogleSheets() {
        String selectedClass = classSpinner.getSelectedItem() != null ? classSpinner.getSelectedItem().toString() : "";
        selectedDate = dateInput.getText().toString().trim();

        if (selectedClass.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(getContext(), "Please select a class and enter a date.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fetching attendance...");
        progressDialog.show();

        firebaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();
                String startTime = null, endTime = null;

                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    String className = classSnapshot.child("className").getValue(String.class);
                    if (className != null && className.equals(selectedClass)) {
                        startTime = convertTo24HourFormat(classSnapshot.child("startTime").getValue(String.class));
                        endTime = convertTo24HourFormat(classSnapshot.child("endTime").getValue(String.class));
                        break;
                    }
                }

                if (startTime == null || endTime == null) {
                    Toast.makeText(getContext(), "Class timings not available.", Toast.LENGTH_SHORT).show();
                } else {
                    filterAttendance(selectedClass, selectedDate, startTime, endTime);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(getContext(), "Error fetching class timings.", Toast.LENGTH_SHORT).show();
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

        // Add header row
        TableRow headerRow = new TableRow(getContext());
        headerRow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        addTextViewToRow(headerRow, "Name");
        addTextViewToRow(headerRow, "Student ID");
        addTextViewToRow(headerRow, "Check-In");
        addTextViewToRow(headerRow, "Check-Out");
        attendanceTable.addView(headerRow);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject jsonObject = response.getJSONObject(i);
                String name = jsonObject.optString("Name", "");
                String cardUID = jsonObject.optString("UID", "");
                String checkIn = jsonObject.optString("TimeIn", "");
                String checkOut = jsonObject.optString("TimeOut", "");
                String date = jsonObject.optString("Date", "");

                if (date.equals(selectedDate) && timeFormat.parse(checkIn).compareTo(timeFormat.parse(startTime)) >= 0 &&
                        timeFormat.parse(checkOut).compareTo(timeFormat.parse(endTime)) <= 0) {

                    usersReference.orderByChild("cardUid").equalTo(cardUID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                Student student = userSnapshot.getValue(Student.class);
                                if (student != null) {
                                    TableRow newRow = new TableRow(getContext());
                                    addTextViewToRow(newRow, name);
                                    addTextViewToRow(newRow, student.getStudentId());  // Replaced ID with Student ID
                                    addTextViewToRow(newRow, checkIn);
                                    addTextViewToRow(newRow, checkOut);
                                    attendanceTable.addView(newRow);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Firebase error fetching student ID: " + error.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error filtering attendance: " + e.getMessage());
            }
        }
    }

    private String convertTo24HourFormat(String time) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("h:mm a"); // Input format (e.g., "3:30 AM")
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss"); // Output format (e.g., "03:30:00")
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
