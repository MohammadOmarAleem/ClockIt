// AnnouncementsFragment.java
package com.example.clockit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button buttonSendAnnouncement;
    private DatabaseReference announcementsRef;
    private List<Announcement> announcementList;
    private AnnouncementsAdapter adapter;
    private DrawerLayout drawerLayout;

    public AnnouncementsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_announcements, container, false);

        // Set up Toolbar and DrawerLayout
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        drawerLayout = view.findViewById(R.id.drawer_layout_announcements);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
        }

        // Set up drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                activity, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set up NavigationView
        NavigationView navigationView = view.findViewById(R.id.nav_view_announcements);
        // Set up NavigationView
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new AdminFragment());
            } else if (itemId == R.id.nav_students) { // Replaced nav_participants with nav_students
                loadFragment(new StudentsFragment());
            } else if (itemId == R.id.nav_announcements) {
                loadFragment(new AnnouncementsFragment());
            } else if (itemId == R.id.nav_card_assign) {
                loadFragment(new CardAssignFragment());
            } else if (itemId == R.id.nav_add_classes) {
                // Start AddClassActivity instead of AddClassesFragment
                Intent intent = new Intent(getActivity(), AddClassActivity.class);
                startActivity(intent);
            } else if (itemId == R.id.nav_help) {
                loadFragment(new HelpFragment());
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });



        // Initialize RecyclerView and Button
        recyclerView = view.findViewById(R.id.recycler_announcements);
        buttonSendAnnouncement = view.findViewById(R.id.button_send_announcement);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementList = new ArrayList<>();
        adapter = new AnnouncementsAdapter(announcementList);
        recyclerView.setAdapter(adapter);

        // Set the item click listener
        adapter.setOnItemClickListener(announcement -> openAnnouncementDetailFragment(announcement));

        // Initialize Firebase reference
        announcementsRef = FirebaseDatabase.getInstance().getReference("Announcements");

        // Fetch announcements from Firebase
        fetchAnnouncements();

        // Handle "Send Announcement" button click
        buttonSendAnnouncement.setOnClickListener(v -> openNewAnnouncementFragment());

        return view;
    }

    private void fetchAnnouncements() {
        announcementsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                announcementList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Announcement announcement = snapshot.getValue(Announcement.class);
                    announcementList.add(announcement);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }

    private void openNewAnnouncementFragment() {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new NewAnnouncementFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openAnnouncementDetailFragment(Announcement announcement) {
        // Create a bundle with the announcement details
        Bundle bundle = new Bundle();
        bundle.putString("title", announcement.getTitle());
        bundle.putString("dateTime", announcement.getDate() + " " + announcement.getTime());
        bundle.putString("participants", "All Participants"); // or set specific participants
        bundle.putString("message", announcement.getMessage());

        // Create the detail fragment and pass the bundle
        AnnouncementDetailFragment detailFragment = new AnnouncementDetailFragment();
        detailFragment.setArguments(bundle);

        // Start the detail fragment
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, detailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
