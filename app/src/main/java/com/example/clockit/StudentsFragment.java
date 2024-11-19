package com.example.clockit;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
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
        drawerLayout = view.findViewById(R.id.drawer_menu);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        }

        // Initialize Navigation Drawer
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Navigate to Home
                getFragmentManager().beginTransaction().replace(R.id.fragment_container, new AdminFragment()).commit();
            } else if (id == R.id.nav_students) {
                // Reload StudentsFragment
                getFragmentManager().beginTransaction().replace(R.id.fragment_container, new StudentsFragment()).commit();
            } else if (id == R.id.nav_logout) {
                // Handle Logout
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new LoginFragment()).commit();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Setup SearchView
        searchView = view.findViewById(R.id.searchView);  // Make sure this ID exists in fragment_students.xml
        searchView.setQueryHint("Search students");

        // RecyclerView Setup
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentList = new ArrayList<>();
        adapter = new StudentAdapter(getContext(), studentList);  // Pass context and list
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
                // Handle possible errors
            }
        });
    }

    private void filterStudents(String query) {
        List<Student> filteredList = new ArrayList<>();
        for (Student student : studentList) {
            if (student.getName() != null && student.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(student);
            } else if (student.getStudentId() != null && student.getStudentId().toLowerCase().contains(query.toLowerCase())) { // Updated to getStudentId()
                filteredList.add(student);
            }
        }
        adapter.setStudentList(filteredList);
    }
}
