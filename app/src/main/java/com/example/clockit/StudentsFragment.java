package com.example.clockit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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

public class StudentsFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private List<Student> studentList;
    private DatabaseReference databaseReference;

    public StudentsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_students, container, false);

        // Initialize Drawer and Toolbar
        drawerLayout = view.findViewById(R.id.drawer_layout); // Updated ID for DrawerLayout
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        }

        // Set up ActionBarDrawerToggle for navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                requireActivity(), drawerLayout, view.findViewById(R.id.toolbar),
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize Navigation Drawer
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        // Setup SearchView
        searchView = view.findViewById(R.id.searchView);
        searchView.setQueryHint("Search students");

        // RecyclerView Setup
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentList = new ArrayList<>();
        adapter = new StudentAdapter(getContext(), studentList);
        recyclerView.setAdapter(adapter);

        // Fetch students from Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        fetchStudents();

        // Set up Search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterStudents(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterStudents(newText);
                return false;
            }
        });

        return view;
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();

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
        } else if (itemId == R.id.attend_log) {
            transaction.replace(R.id.fragment_container, new Attend_log_History()).commit();
        } else if (itemId == R.id.nav_help) {
            transaction.replace(R.id.fragment_container, new HelpFragment()).commit();
        } else if (itemId == R.id.nav_edit_classes) {
            transaction.replace(R.id.fragment_container, new EditClassActivity()).commit();
        } else if (itemId == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            transaction.replace(R.id.fragment_container, new LoginFragment()).commit();
            Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    private void loadFragment(Fragment fragment) {
        if (getView() != null) {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            Toast.makeText(getContext(), "Fragment container not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchStudents() {
        databaseReference.orderByChild("accountType").equalTo("User").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Student student = dataSnapshot.getValue(Student.class);
                    if (student != null) {
                        studentList.add(student);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch students: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterStudents(String query) {
        List<Student> filteredList = new ArrayList<>();
        for (Student student : studentList) {
            if (student.getName() != null && student.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(student);
            } else if (student.getStudentId() != null && student.getStudentId().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(student);
            }
        }
        adapter.setStudentList(filteredList);
    }
}
