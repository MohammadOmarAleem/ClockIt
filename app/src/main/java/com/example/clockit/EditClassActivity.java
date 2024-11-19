package com.example.clockit;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EditClassActivity extends AppCompatActivity {

    private RecyclerView rvClassList;
    private EditText etClassName, etClassCode, etClassDescription, etClassStartTime, etClassEndTime, etClassRoom;
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    private Button btnUpdateClass, btnDeleteClass;

    private DatabaseReference classDatabase;
    private List<ClassModel> classList;
    private ClassAdapter classAdapter;
    private String selectedClassId;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_class);

        // Set up Toolbar and Drawer
        setupToolbar();

        // Initialize Firebase
        classDatabase = FirebaseDatabase.getInstance().getReference("classes");

        // Initialize Views
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load Classes from Firebase
        loadClasses();

        // Set Button Listeners
        btnUpdateClass.setOnClickListener(v -> updateClass());
        btnDeleteClass.setOnClickListener(v -> deleteClass());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item);
            return true;
        });
    }

    private void initializeViews() {
        rvClassList = findViewById(R.id.rvClassList);
        etClassName = findViewById(R.id.etClassName);
        etClassCode = findViewById(R.id.etClassCode);
        etClassDescription = findViewById(R.id.etClassDescription);
        etClassStartTime = findViewById(R.id.etClassStartTime);
        etClassEndTime = findViewById(R.id.etClassEndTime);
        etClassRoom = findViewById(R.id.etClassRoom);

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        btnUpdateClass = findViewById(R.id.btnUpdateClass);
        btnDeleteClass = findViewById(R.id.btnDeleteClass);
    }

    private void setupRecyclerView() {
        rvClassList.setLayoutManager(new LinearLayoutManager(this));
        classList = new ArrayList<>();
        classAdapter = new ClassAdapter(classList, this::onClassSelected);
        rvClassList.setAdapter(classAdapter);
    }

    private void loadClasses() {
        classDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classList.clear();
                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    ClassModel classModel = classSnapshot.getValue(ClassModel.class);
                    if (classModel != null) {
                        classModel.setClassId(classSnapshot.getKey());
                        classList.add(classModel);
                    }
                }
                classAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditClassActivity.this, "Failed to load classes.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onClassSelected(ClassModel classModel) {
        selectedClassId = classModel.getClassId();
        etClassName.setText(classModel.getClassName());
        etClassCode.setText(classModel.getClassCode());
        etClassDescription.setText(classModel.getClassDescription());
        etClassStartTime.setText(classModel.getStartTime());
        etClassEndTime.setText(classModel.getEndTime());
        etClassRoom.setText(classModel.getRoomNumber());

        cbMonday.setChecked(classModel.getDays().contains("Monday"));
        cbTuesday.setChecked(classModel.getDays().contains("Tuesday"));
        cbWednesday.setChecked(classModel.getDays().contains("Wednesday"));
        cbThursday.setChecked(classModel.getDays().contains("Thursday"));
        cbFriday.setChecked(classModel.getDays().contains("Friday"));
        cbSaturday.setChecked(classModel.getDays().contains("Saturday"));
        cbSunday.setChecked(classModel.getDays().contains("Sunday"));

        findViewById(R.id.layoutEditClass).setVisibility(View.VISIBLE);
    }

    private void updateClass() {
        if (selectedClassId == null) {
            Toast.makeText(this, "Please select a class to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        classDatabase.child(selectedClassId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(EditClassActivity.this, "Class data not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String adminId = snapshot.child("adminId").getValue(String.class);
                if (adminId == null) {
                    Toast.makeText(EditClassActivity.this, "Admin ID not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> updatedDays = getSelectedDays();

                ClassData updatedClass = new ClassData(
                        selectedClassId,
                        etClassName.getText().toString(),
                        etClassCode.getText().toString(),
                        etClassDescription.getText().toString(),
                        snapshot.child("teacherName").getValue(String.class),
                        etClassStartTime.getText().toString(),
                        etClassEndTime.getText().toString(),
                        etClassRoom.getText().toString(),
                        adminId,
                        updatedDays
                );

                classDatabase.child(selectedClassId).setValue(updatedClass)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EditClassActivity.this, "Class updated successfully!", Toast.LENGTH_SHORT).show();
                            redirectToHomePage();
                        })
                        .addOnFailureListener(e -> Toast.makeText(EditClassActivity.this, "Failed to update class.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditClassActivity.this, "Error fetching class data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteClass() {
        if (selectedClassId == null) {
            Toast.makeText(this, "Please select a class to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        classDatabase.child(selectedClassId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Class deleted successfully!", Toast.LENGTH_SHORT).show();
                    redirectToHomePage();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete class.", Toast.LENGTH_SHORT).show());
    }

    private List<String> getSelectedDays() {
        List<String> days = new ArrayList<>();
        if (cbMonday.isChecked()) days.add("Monday");
        if (cbTuesday.isChecked()) days.add("Tuesday");
        if (cbWednesday.isChecked()) days.add("Wednesday");
        if (cbThursday.isChecked()) days.add("Thursday");
        if (cbFriday.isChecked()) days.add("Friday");
        if (cbSaturday.isChecked()) days.add("Saturday");
        if (cbSunday.isChecked()) days.add("Sunday");
        return days;
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            redirectToHomePage();
        } else if (id == R.id.nav_students) {
            startActivity(new Intent(this, StudentsFragment.class));
        } else if (id == R.id.nav_announcements) {
            startActivity(new Intent(this, AnnouncementsFragment.class));
        } else if (id == R.id.nav_add_classes) {
            startActivity(new Intent(this, AddClassActivity.class));
        } else if (id == R.id.nav_edit_classes) {
            startActivity(new Intent(this, EditClassActivity.class));
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginFragment.class));
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void redirectToHomePage() {
        Intent intent = new Intent(EditClassActivity.this, MainActivity.class);
        intent.putExtra("navigateTo", "AdminFragment");
        startActivity(intent);
        finish();
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
