package com.example.clockit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class AddClassActivity extends AppCompatActivity {

    private EditText etClassName, etClassCode, etClassDescription, etTeacherName, etClassStartTime, etClassEndTime, etClassRoom;
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    private Button btnSubmitClass;
    private DatabaseReference classDatabase;
    private FirebaseAuth firebaseAuth;

    // Navigation drawer components
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);

        // Set up the toolbar and navigation drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        // Initialize Firebase authentication and database reference
        firebaseAuth = FirebaseAuth.getInstance();
        classDatabase = FirebaseDatabase.getInstance().getReference("classes");

        // Initialize UI elements
        etClassName = findViewById(R.id.etClassName);
        etClassCode = findViewById(R.id.etClassCode);
        etClassDescription = findViewById(R.id.etClassDescription);
        etTeacherName = findViewById(R.id.etTeacherName);
        etClassStartTime = findViewById(R.id.etClassStartTime);
        etClassEndTime = findViewById(R.id.etClassEndTime);
        etClassRoom = findViewById(R.id.etClassRoom);

        // Initialize checkboxes for days of the week
        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        btnSubmitClass = findViewById(R.id.btnSubmitClass);

        // Set the button listener
        btnSubmitClass.setOnClickListener(this::addClassToFirebase);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (id == R.id.nav_home) {
            transaction.replace(R.id.fragment_container, new AdminFragment()).commit();
        } else if (id == R.id.nav_students) {
            transaction.replace(R.id.fragment_container, new StudentsFragment()).commit();
        } else if (id == R.id.nav_announcements) {
            transaction.replace(R.id.fragment_container, new AnnouncementsFragment()).commit();
        } else if (id == R.id.nav_card_assign) {
            transaction.replace(R.id.fragment_container, new CardAssignFragment()).commit();
        } else if (id == R.id.nav_help) {
            transaction.replace(R.id.fragment_container, new HelpFragment()).commit();
        } else if (id == R.id.nav_logout) {
            firebaseAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            // Redirect to LoginFragment
            transaction.replace(R.id.fragment_container, new LoginFragment()).commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void addClassToFirebase(View view) {
        // Get input values
        String className = etClassName.getText().toString().trim();
        String classCode = etClassCode.getText().toString().trim();
        String classDescription = etClassDescription.getText().toString().trim();
        String teacherName = etTeacherName.getText().toString().trim();
        String classStartTime = etClassStartTime.getText().toString().trim();
        String classEndTime = etClassEndTime.getText().toString().trim();
        String classRoom = etClassRoom.getText().toString().trim();
        String adminId = firebaseAuth.getCurrentUser().getUid(); // Get current admin's ID
        List<String> classDays = getSelectedDays(); // Get the selected days

        // Validate input
        if (TextUtils.isEmpty(className) || TextUtils.isEmpty(classCode) ||
                TextUtils.isEmpty(classDescription) || TextUtils.isEmpty(teacherName) ||
                TextUtils.isEmpty(classStartTime) || TextUtils.isEmpty(classEndTime) ||
                TextUtils.isEmpty(classRoom) || classDays.isEmpty()) {
            Toast.makeText(this, "All fields and at least one day are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a unique class ID
        String classId = classDatabase.push().getKey();

        // Create a new ClassData object
        ClassData newClass = new ClassData(classId, className, classCode, classDescription, teacherName, classStartTime, classEndTime, classRoom, adminId, classDays);

        // Store the class data in Firebase
        if (classId != null) {
            classDatabase.child(classId).setValue(newClass)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Class added successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add class", Toast.LENGTH_SHORT).show());
        }
    }

    private List<String> getSelectedDays() {
        List<String> selectedDays = new ArrayList<>();
        if (cbMonday.isChecked()) selectedDays.add("Monday");
        if (cbTuesday.isChecked()) selectedDays.add("Tuesday");
        if (cbWednesday.isChecked()) selectedDays.add("Wednesday");
        if (cbThursday.isChecked()) selectedDays.add("Thursday");
        if (cbFriday.isChecked()) selectedDays.add("Friday");
        if (cbSaturday.isChecked()) selectedDays.add("Saturday");
        if (cbSunday.isChecked()) selectedDays.add("Sunday");
        return selectedDays;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
