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

public class Attendance_log extends AppCompatActivity{
    private TableLayout attendanceTable;
    private EditText manualEntryName, manualEntryID;
    private Button manualEntryButton, logoutButton;

    public Attendance_log(){


    }

    @Override
    protected void onCreate(Bundle bun) {
        super.onCreate(bun);
        setContentView(R.layout.log_attendance);

        // Initialize views
        attendanceTable = findViewById(R.id.attendanceTable);
        manualEntryName = findViewById(R.id.manualEntryName);
        manualEntryID = findViewById(R.id.manualEntryID);
        manualEntryButton = findViewById(R.id.manualEntryButton);

        // Fetch course details from intent
        Intent intent = getIntent();
        String courseName = intent.getStringExtra("courseName"); // Retrieve the course name
        String currentDateTimeString = DateFormat.format("MM/dd/yyyy hh:mm a", Calendar.getInstance().getTime()).toString();

        // Update UI elements
        TextView courseTitle = findViewById(R.id.courseTitle);
        TextView courseDateTime = findViewById(R.id.courseDateTime);

        // Set the course name and date/time
        courseTitle.setText(courseName != null ? "Course: " + courseName : "Course: Loading...");
        courseDateTime.setText("Date and Time: " + currentDateTimeString);

        // Handle manual entry addition
        manualEntryButton.setOnClickListener(v -> addManualEntry());

        // Populate the attendance table
        ReadFromSheet();
    }




    // Method to add a manual entry to the attendance log
    private void addManualEntry() {
        String name = manualEntryName.getText().toString().trim();
        String id = manualEntryID.getText().toString().trim();

        if (name.isEmpty() || id.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new TableRow
        TableRow newRow = new TableRow(this);
        newRow.setPadding(0,0,0,0);
        // Create new TextViews for each field (Name, ID, Check-in Time)
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setPadding(8, 8, 8, 8);

        TextView idView = new TextView(this);
        idView.setText(id);
        idView.setPadding(8, 8, 8, 8);

        Calendar calendar = Calendar.getInstance();

        // Get hour, minute, and second from the current time
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        // Format the time manually (HH:mm:ss)
        String formattedTime = String.format("%02d:%02d:%02d", hour, minute, second);



        // Get year, month, and day from the current date
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Month is 0-based, so we add 1
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Format the date as "MM/DD/YYYY"
        String formattedDate = String.format("%02d/%02d/%04d", month, day, year);

        TextView timeView = new TextView(this);
        timeView.setText(formattedTime);
        timeView.setPadding(8, 8, 8, 8);

        // Add TextViews to the new row
        newRow.addView(nameView);
        newRow.addView(idView);
        newRow.addView(timeView);
        addItemtoSheet(name,id,formattedTime,formattedDate);
        // Add the new row to the attendance table
        attendanceTable.addView(newRow);

        // Clear the input fields
        manualEntryName.setText("");
        manualEntryID.setText("");
    }
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
    private void ReadFromSheet(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("please wait..");
        progressDialog.show();
        Intent intent = getIntent();
        String classId = intent.getStringExtra("classId");
        String startTimetext = intent.getStringExtra("startTime");
        String endTimetext = intent.getStringExtra("endTime");

        Log.d("Attendance_log", "Received classId: " + classId);
        Log.d("Attendance_log", "Received startTime: " + startTimetext);
        Log.d("Attendance_log", "Received endTime: " + endTimetext);
        int startTimeclass = convertTimeToMinutes(startTimetext);
        int endTimeclass = convertTimeToMinutes(endTimetext);
        // int startTimeclass = Integer.parseInt(startTimetext);
        //int endTimeclass = Integer.parseInt(endTimetext);
        System.out.println(startTimetext);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, "https://script.google.com/macros/s/AKfycbzTNcTq6jIsro4B0Rz6CtHuEs-XGDw7C6S1wRgCIxgtXG3yIIpfZOPHNk8eW1OxQ9ddZw/exec", null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {

                for(int i=0;i<response.length();i++){

                    try{

                        JSONObject jsonObject = response.getJSONObject(i);


                        String datesheet = jsonObject.optString("Date", "N/A");
                        String startTimesheettext =  jsonObject.optString("TimeIn", "N/A");
                        String endTimesheettext =  jsonObject.optString("TimeOut", "N/A");
                        int startTimesheet = convertTimeToMinutes2(startTimesheettext);
                        int endTimesheet = convertTimeToMinutes2(endTimesheettext);


                        if(datesheet.equals(currentDate)  && startTimesheet >= startTimeclass && endTimesheet <= endTimeclass && startTimesheet < endTimeclass && endTimesheet > startTimeclass ) {
                            TableRow newRow = new TableRow(Attendance_log.this);
                            newRow.setPadding(4, 0, 0, 0);
                            // Create new TextViews for each field (Name, ID, Check-in Time)
                            TextView nameView = new TextView(Attendance_log.this);
                            nameView.setText(jsonObject.optString("Name", "N/A"));
                            nameView.setPadding(8, 8, 8, 8);

                            TextView idView = new TextView(Attendance_log.this);
                            idView.setText(jsonObject.optString("UID", "N/A"));
                            idView.setPadding(8, 8, 8, 8);

                            TextView date = new TextView(Attendance_log.this);
                            // date.setText(jsonObject.optString("Date", "N/A"));


                            TextView timeView = new TextView(Attendance_log.this);
                            timeView.setText(jsonObject.optString("TimeIn", "N/A"));
                            timeView.setPadding(8, 8, 8, 8);

                            TextView timeoutView = new TextView(Attendance_log.this);
                            timeoutView.setText(jsonObject.optString("TimeOut", "N/A"));
                            timeoutView.setPadding(8, 8, 8, 8);

                            // Add TextViews to the new row
                            newRow.addView(nameView);
                            newRow.addView(idView);

                            newRow.addView(timeView);
                            newRow.addView(timeoutView);
                            attendanceTable.addView(newRow);
                        }
                        progressDialog.dismiss();
                    }catch(JSONException e){
                        e.printStackTrace();
                        progressDialog.dismiss();

                    }

                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(),"NANIIIIIIIII?",Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonArrayRequest);

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