package com.example.clockit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

public class EditClassActivity extends Fragment {

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.activity_edit_class, container, false);

        // Set up Toolbar and Drawer
        setupToolbar(rootView);

        // Initialize Firebase
        classDatabase = FirebaseDatabase.getInstance().getReference("classes");

        // Initialize Views
        initializeViews(rootView);

        // Setup RecyclerView
        setupRecyclerView();

        // Load Classes from Firebase
        loadClasses();

        // Set Button Listeners
        btnUpdateClass.setOnClickListener(v -> updateClass());
        btnDeleteClass.setOnClickListener(v -> deleteClass());

        return rootView;
    }

    private void setupToolbar(View rootView) {
        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        drawerLayout = rootView.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                requireActivity(), drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = rootView.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item);
            return true;
        });
    }

    private LinearLayout layoutEditClass; // Declare at the top

    private void initializeViews(View rootView) {
        rvClassList = rootView.findViewById(R.id.rvClassList);
        etClassName = rootView.findViewById(R.id.etClassName);
        etClassCode = rootView.findViewById(R.id.etClassCode);
        etClassDescription = rootView.findViewById(R.id.etClassDescription);
        etClassStartTime = rootView.findViewById(R.id.etClassStartTime);
        etClassEndTime = rootView.findViewById(R.id.etClassEndTime);
        etClassRoom = rootView.findViewById(R.id.etClassRoom);

        cbMonday = rootView.findViewById(R.id.cbMonday);
        cbTuesday = rootView.findViewById(R.id.cbTuesday);
        cbWednesday = rootView.findViewById(R.id.cbWednesday);
        cbThursday = rootView.findViewById(R.id.cbThursday);
        cbFriday = rootView.findViewById(R.id.cbFriday);
        cbSaturday = rootView.findViewById(R.id.cbSaturday);
        cbSunday = rootView.findViewById(R.id.cbSunday);

        btnUpdateClass = rootView.findViewById(R.id.btnUpdateClass);
        btnDeleteClass = rootView.findViewById(R.id.btnDeleteClass);

        layoutEditClass = rootView.findViewById(R.id.layoutEditClass); // Ensure correct ID
        layoutEditClass.setVisibility(View.GONE); // Initially hide
    }



    private void setupRecyclerView() {
        rvClassList.setLayoutManager(new LinearLayoutManager(requireContext()));
        classList = new ArrayList<>();
        classAdapter = new ClassAdapter(classList, this::onClassSelected);
        rvClassList.setAdapter(classAdapter);
    }

    private void loadClasses() {
        String currentProfessorId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get the current professor's ID

        classDatabase.orderByChild("adminId").equalTo(currentProfessorId) // Filter classes by adminId
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        classList.clear(); // Clear the list before adding new data
                        for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                            ClassModel classModel = classSnapshot.getValue(ClassModel.class);
                            if (classModel != null) {
                                classModel.setClassId(classSnapshot.getKey()); // Set the class ID
                                classList.add(classModel); // Add the class to the list
                            }
                        }
                        classAdapter.notifyDataSetChanged(); // Notify the adapter of the data change

                        if (classList.isEmpty()) {
                            Toast.makeText(requireContext(), "No classes found for this professor.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load classes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void onClassSelected(ClassModel classModel) {
        if (layoutEditClass != null) {
            layoutEditClass.setVisibility(View.VISIBLE); // Show the layout when a class is selected
        }

        // Populate the fields with the selected class details
        selectedClassId = classModel.getClassId();
        etClassName.setText(classModel.getClassName());
        etClassCode.setText(classModel.getClassCode());
        etClassDescription.setText(classModel.getClassDescription());
        etClassStartTime.setText(classModel.getStartTime());
        etClassEndTime.setText(classModel.getEndTime());
        etClassRoom.setText(classModel.getRoomNumber());

        // Check or uncheck the checkboxes based on the selected class's days
        cbMonday.setChecked(classModel.getDays().contains("Monday"));
        cbTuesday.setChecked(classModel.getDays().contains("Tuesday"));
        cbWednesday.setChecked(classModel.getDays().contains("Wednesday"));
        cbThursday.setChecked(classModel.getDays().contains("Thursday"));
        cbFriday.setChecked(classModel.getDays().contains("Friday"));
        cbSaturday.setChecked(classModel.getDays().contains("Saturday"));
        cbSunday.setChecked(classModel.getDays().contains("Sunday"));
    }


    private void updateClass() {
        if (selectedClassId == null) {
            Toast.makeText(requireContext(), "Please select a class to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        classDatabase.child(selectedClassId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "Class data not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String adminId = snapshot.child("adminId").getValue(String.class);
                if (adminId == null) {
                    Toast.makeText(requireContext(), "Admin ID not found.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(requireContext(), "Class updated successfully!", Toast.LENGTH_SHORT).show();
                            redirectToHomePage();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update class.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error fetching class data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteClass() {
        if (selectedClassId == null) {
            Toast.makeText(requireContext(), "Please select a class to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        classDatabase.child(selectedClassId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Class deleted successfully!", Toast.LENGTH_SHORT).show();
                    redirectToHomePage();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to delete class.", Toast.LENGTH_SHORT).show());
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
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null); // Optional: add to back stack if needed
        transaction.commit();
    }
    private boolean handleNavigation(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            loadFragment(new AdminFragment());
        } else if (itemId == R.id.nav_students) {
            loadFragment(new StudentsFragment());
        } else if (itemId == R.id.nav_announcements) {
            loadFragment(new AnnouncementsFragment());
        } else if (itemId == R.id.nav_card_assign) {
            loadFragment(new CardAssignFragment());
        } else if (itemId == R.id.nav_add_classes) {
            loadFragment(new AddClassActivity());
        } else if (itemId == R.id.nav_help) {
            loadFragment(new HelpFragment());
        }  else if (itemId == R.id.attend_log) {
            loadFragment(new Attend_log_History());
        }else if (itemId == R.id.nav_edit_classes) {
            loadFragment(new EditClassActivity());
        }

        else if (itemId == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            loadFragment(new LoginFragment());
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void redirectToHomePage() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.putExtra("navigateTo", "AdminFragment");
        startActivity(intent);
        requireActivity().finish();
    }
}
