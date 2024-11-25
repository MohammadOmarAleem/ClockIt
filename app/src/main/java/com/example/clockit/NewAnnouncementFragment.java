package com.example.clockit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class NewAnnouncementFragment extends Fragment {

    private EditText editTitle, editMessage;
    private Spinner spinnerRecipients;
    private Button buttonSendAnnouncement;
    private DatabaseReference announcementsRef, usersRef;

    public NewAnnouncementFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_announcement, container, false);

        // Set up Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar_new_announcement);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        // Initialize Firebase references
        announcementsRef = FirebaseDatabase.getInstance().getReference("Announcements");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize UI elements
        editTitle = view.findViewById(R.id.edit_title);
        editMessage = view.findViewById(R.id.edit_message);
        buttonSendAnnouncement = view.findViewById(R.id.button_send_announcement);

        // Handle "Send Announcement" button click
        buttonSendAnnouncement.setOnClickListener(v -> handleSendAnnouncement());

        return view;
    }

    private void handleSendAnnouncement() {
        String title = editTitle.getText().toString().trim();
        String message = editMessage.getText().toString().trim();

        // Validate fields
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current date and time
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // Create announcement object
        Announcement announcement = new Announcement(title, date, time, message);

        // Save to Firebase
        announcementsRef.push().setValue(announcement)
                .addOnSuccessListener(aVoid -> {
                    fetchUserEmailsAndSendEmail(title, message);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send announcement", Toast.LENGTH_SHORT).show());
    }

    private void fetchUserEmailsAndSendEmail(String title, String message) {
        usersRef.orderByChild("accountType").equalTo("User").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> emailList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String email = snapshot.child("email").getValue(String.class);
                    if (email != null && !email.isEmpty()) {
                        emailList.add(email);
                    }
                }

                if (!emailList.isEmpty()) {
                    sendEmail(emailList, title, message);
                } else {
                    Toast.makeText(getContext(), "No user emails found to send announcement.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to fetch user emails.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendEmail(ArrayList<String> emailList, String subject, String body) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822"); // MIME type for email
        emailIntent.putExtra(Intent.EXTRA_EMAIL, emailList.toArray(new String[0]));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "No email client installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
