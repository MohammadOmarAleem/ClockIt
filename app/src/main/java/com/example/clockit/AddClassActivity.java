package com.example.clockit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class AddClassActivity extends Fragment {

    private EditText etClassName, etClassCode, etClassDescription, etTeacherName, etClassStartTime, etClassEndTime, etClassRoom;
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    private Button btnSubmitClass;
    private DatabaseReference classDatabase;
    private FirebaseAuth firebaseAuth;

    // Navigation drawer components
    private DrawerLayout drawerLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_add_class, container, false);

        // Set up the toolbar and navigation drawer
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        drawerLayout = view.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                requireActivity(), drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        // Initialize Firebase authentication and database reference
        firebaseAuth = FirebaseAuth.getInstance();
        classDatabase = FirebaseDatabase.getInstance().getReference("classes");

        // Initialize UI elements
        etClassName = view.findViewById(R.id.etClassName);
        etClassCode = view.findViewById(R.id.etClassCode);
        etClassDescription = view.findViewById(R.id.etClassDescription);
        etTeacherName = view.findViewById(R.id.etTeacherName);
        etClassStartTime = view.findViewById(R.id.etClassStartTime);
        etClassEndTime = view.findViewById(R.id.etClassEndTime);
        etClassRoom = view.findViewById(R.id.etClassRoom);

        // Initialize checkboxes for days of the week
        cbMonday = view.findViewById(R.id.cbMonday);
        cbTuesday = view.findViewById(R.id.cbTuesday);
        cbWednesday = view.findViewById(R.id.cbWednesday);
        cbThursday = view.findViewById(R.id.cbThursday);
        cbFriday = view.findViewById(R.id.cbFriday);
        cbSaturday = view.findViewById(R.id.cbSaturday);
        cbSunday = view.findViewById(R.id.cbSunday);

        btnSubmitClass = view.findViewById(R.id.btnSubmitClass);

        // Set the button listener
        btnSubmitClass.setOnClickListener(this::addClassToFirebase);

        return view;
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();

        if (id == R.id.nav_home) {
            transaction.replace(R.id.fragment_container, new AdminFragment()).commit();
        } else if (id == R.id.nav_students) {
            transaction.replace(R.id.fragment_container, new StudentsFragment()).commit();
        } else if (id == R.id.nav_announcements) {
            transaction.replace(R.id.fragment_container, new AnnouncementsFragment()).commit();
        } else if (id == R.id.nav_card_assign) {
            transaction.replace(R.id.fragment_container, new CardAssignFragment()).commit();
        } else if (id == R.id.nav_add_classes) {
            transaction.replace(R.id.fragment_container, new AddClassActivity()).commit();
        } else if (id == R.id.attend_log) {
            transaction.replace(R.id.fragment_container, new Attend_log_History()).commit();
        } else if (id == R.id.nav_help) {
            transaction.replace(R.id.fragment_container, new HelpFragment()).commit();
        } else if (id == R.id.nav_edit_classes) {
            transaction.replace(R.id.fragment_container, new EditClassActivity()).commit();
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            transaction.replace(R.id.fragment_container, new LoginFragment()).commit();
            Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getActivity(), "All fields and at least one day are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for overlapping classes
        classDatabase.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean overlapFound = false;

                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    ClassData existingClass = snapshot.getValue(ClassData.class);
                    if (existingClass == null) continue;

                    // Check for overlapping days and times
                    for (String day : classDays) {
                        if (existingClass.getDays().contains(day) &&
                                classStartTime.equals(existingClass.getStartTime()) &&
                                classEndTime.equals(existingClass.getEndTime())) {
                            overlapFound = true;
                            break;
                        }
                    }

                    if (overlapFound) break;
                }

                if (overlapFound) {
                    Toast.makeText(getActivity(), "Overlapping class time detected. Please choose a different time or day.", Toast.LENGTH_SHORT).show();
                } else {
                    // Save the class to Firebase
                    String classId = classDatabase.push().getKey();
                    ClassData newClass = new ClassData(classId, className, classCode, classDescription, teacherName, classStartTime, classEndTime, classRoom, adminId, classDays);

                    if (classId != null) {
                        classDatabase.child(classId).setValue(newClass)
                                .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Class added successfully", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to add class", Toast.LENGTH_SHORT).show());
                    }
                }
            } else {
                Toast.makeText(getActivity(), "Error checking for overlapping classes. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
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
}