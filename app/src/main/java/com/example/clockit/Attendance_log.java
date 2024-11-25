package com.example.clockit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Set;
import java.util.HashSet;


public class Attendance_log extends AppCompatActivity{
    private TableLayout attendanceTable;
    private DatabaseReference usersReference;
    private TableLayout absentTable;

    public Attendance_log(){


    }

    @Override
    protected void onCreate(Bundle bun) {
        super.onCreate(bun);
        setContentView(R.layout.log_attendance);

        // Initialize views
        attendanceTable = findViewById(R.id.attendanceTable);
        absentTable = findViewById(R.id.absentTable);
        usersReference = FirebaseDatabase.getInstance().getReference("Users");

        // Back button functionality
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> {
            finish(); // Closes this activity and goes back to the previous one
        });

        // Fetch course details from intent
        Intent intent = getIntent();
        String courseName = intent.getStringExtra("courseName");

        // Fetch the current date and time
        String currentDateTimeString = DateFormat.format("MM/dd/yyyy hh:mm a", Calendar.getInstance().getTime()).toString();

        // Update UI elements
        TextView courseTitle = findViewById(R.id.courseTitle);
        TextView courseDateTime = findViewById(R.id.courseDateTime);
        courseTitle.setText(courseName != null ? "Course: " + courseName : "Course: Loading...");
        courseDateTime.setText("Date and Time: " + currentDateTimeString);

        // Populate attendance and absent data
        ReadFromSheet();
    }




    // Method to add a manual entry to the attendance log

    public static int convertTimeToMinutes(String timeString) {
        try {
            // Define the time format (12-hour format with AM/PM)
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");

            // Parse the time string to a Date object
            Date date = sdf.parse(timeString);

            // Extract hours and minutes from the Date object
            int hours = date.getHours();   // Returns hours (0-23) in 24-hour format
            int minutes = date.getMinutes(); // Returns minutes (0-59)

            // Convert time to minutes from midnight (hours * 60 + minutes)
            int totalMinutes = (hours * 60) + minutes;

            return totalMinutes;
        } catch (ParseException e) {
            // Handle invalid time format
            System.out.println("Invalid time format: " + e.getMessage());
            return -1;  // Return an error code or handle accordingly
        }
    }
    public static int convertTimeToMinutes2(String timeString) {
        try {
            // Define the time format (24-hour format with hours, minutes, and seconds)
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

            // Parse the time string to a Date object
            Date date = sdf.parse(timeString);

            // Extract hours and minutes from the Date object
            int hours = date.getHours();   // Returns hours (0-23) in 24-hour format
            int minutes = date.getMinutes(); // Returns minutes (0-59)

            // Convert the time to minutes from midnight (hours * 60 + minutes)
            int totalMinutes = (hours * 60) + minutes;

            return totalMinutes;
        } catch (ParseException e) {
            // Handle invalid time format
            System.out.println("Invalid time format: " + e.getMessage());
            return -1;  // Return an error code or handle accordingly
        }
    }
    private void ReadFromSheet() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        Intent intent = getIntent();
        String startTimetext = intent.getStringExtra("startTime");
        String endTimetext = intent.getStringExtra("endTime");

        int startTimeclass = convertTimeToMinutes(startTimetext);
        int endTimeclass = convertTimeToMinutes(endTimetext);

        // Get the current time in minutes
        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.show();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                "https://script.google.com/macros/s/AKfycbzTNcTq6jIsro4B0Rz6CtHuEs-XGDw7C6S1wRgCIxgtXG3yIIpfZOPHNk8eW1OxQ9ddZw/exec",
                null,
                response -> {
                    progressDialog.dismiss();

                    Set<String> presentStudentIds = new HashSet<>();

                    // Populate attendance table
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject jsonObject = response.getJSONObject(i);
                            String uid = jsonObject.optString("UID", "N/A");

                            usersReference.orderByChild("cardUid").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                        Student student = userSnapshot.getValue(Student.class);
                                        if (student != null) {
                                            String studentId = student.getStudentId();
                                            presentStudentIds.add(studentId); // Collect IDs of present students
                                            addAttendanceRow(jsonObject, studentId, currentDate, startTimeclass, endTimeclass);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("Attendance_log", "Firebase error fetching student ID: " + error.getMessage());
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // Populate absent table only if class has ended
                    if (currentMinutes >= endTimeclass) {
                        populateAbsentTable(presentStudentIds);
                    } else {
                        Toast.makeText(this, "Class is still in progress. Absent list will not be populated.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(jsonArrayRequest);
    }


    private void populateAbsentTable(Set<String> presentStudentIds) {
        usersReference.orderByChild("accountType").equalTo("User").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Student student = userSnapshot.getValue(Student.class);
                    if (student != null && student.getStudentId() != null && !presentStudentIds.contains(student.getStudentId())) {
                        addAbsentRow(student);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Attendance_log", "Error fetching absent students: " + error.getMessage());
            }
        });
    }
    private void addAbsentRow(Student student) {
        TableRow row = new TableRow(this);

        TextView nameView = new TextView(this);
        nameView.setText(student.getName());
        nameView.setPadding(8, 8, 8, 8);

        TextView idView = new TextView(this);
        idView.setText(student.getStudentId());
        idView.setPadding(8, 8, 8, 8);

        // Add views to the row
        row.addView(nameView);
        row.addView(idView);

        // Add the row to the absent table
        absentTable.addView(row);
    }



    private void addAttendanceRow(JSONObject jsonObject, String studentId, String currentDate, int startTimeclass, int endTimeclass) {
        String datesheet = jsonObject.optString("Date", "N/A");
        String startTimesheettext = jsonObject.optString("TimeIn", "N/A");
        String endTimesheettext = jsonObject.optString("TimeOut", "N/A");

        int startTimesheet = convertTimeToMinutes2(startTimesheettext);
        int endTimesheet = convertTimeToMinutes2(endTimesheettext);

        // Check if the record meets the specified date and time conditions
        if (datesheet.equals(currentDate) && startTimesheet >= startTimeclass && endTimesheet <= endTimeclass && startTimesheet < endTimeclass && endTimesheet > startTimeclass) {
            TableRow newRow = new TableRow(Attendance_log.this);
            newRow.setPadding(4, 0, 0, 0);

            TextView nameView = new TextView(Attendance_log.this);
            nameView.setText(jsonObject.optString("Name", "N/A"));
            nameView.setPadding(8, 8, 8, 8);

            TextView idView = new TextView(Attendance_log.this);
            idView.setText(studentId);
            idView.setPadding(8, 8, 8, 8);

            TextView timeView = new TextView(Attendance_log.this);
            timeView.setText(jsonObject.optString("TimeIn", "N/A"));
            timeView.setPadding(8, 8, 8, 8);

            TextView timeoutView = new TextView(Attendance_log.this);
            timeoutView.setText(jsonObject.optString("TimeOut", "N/A"));
            timeoutView.setPadding(8, 8, 8, 8);

            // Add all views to the new row
            newRow.addView(nameView);
            newRow.addView(idView);
            newRow.addView(timeView);
            newRow.addView(timeoutView);

            // Finally, add the new row to the table
            attendanceTable.addView(newRow);
        }
    }

    private void addItemtoSheet(String name, String id, String checkin,String currdate){

        final ProgressDialog dialog = ProgressDialog.show(this,"Adding Item","Please wait....");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbzTNcTq6jIsro4B0Rz6CtHuEs-XGDw7C6S1wRgCIxgtXG3yIIpfZOPHNk8eW1OxQ9ddZw/exec", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(),""+response,Toast.LENGTH_SHORT).show();


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.dismiss();
            }
        }){


            @Nullable
            @Override
            protected Map<String, String> getParams()  {

                Map<String , String> parmas = new HashMap<>();

                parmas.put("action","addItem");
                parmas.put("id",id);
                parmas.put("userName",name);
                parmas.put("checkin",checkin);
                parmas.put("currDate",currdate);


                return parmas;
            }
        };

        int timeOut = 50000;

        RetryPolicy retryPolicy = new DefaultRetryPolicy(timeOut,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);

    }





}
