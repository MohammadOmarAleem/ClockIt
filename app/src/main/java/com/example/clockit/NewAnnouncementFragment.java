package com.example.clockit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NewAnnouncementFragment extends Fragment {

    private EditText editTitle, editMessage;
    private Spinner spinnerRecipients;
    private Button buttonSendAnnouncement;
    private DatabaseReference announcementsRef;

    public NewAnnouncementFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_announcement, container, false);

        // Set up Toolbar with Back Button
        Toolbar toolbar = view.findViewById(R.id.toolbar_new_announcement);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        // Initialize Firebase reference
        announcementsRef = FirebaseDatabase.getInstance().getReference("Announcements");

        // Initialize UI elements
        editTitle = view.findViewById(R.id.edit_title);
        editMessage = view.findViewById(R.id.edit_message);
        spinnerRecipients = view.findViewById(R.id.spinner_recipients);
        buttonSendAnnouncement = view.findViewById(R.id.button_send_announcement);

        // Set up Spinner options for recipients
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.recipients_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecipients.setAdapter(adapter);

        // Handle "Send Announcement" button click
        buttonSendAnnouncement.setOnClickListener(v -> sendAnnouncement());

        return view;
    }

    private void sendAnnouncement() {
        String title = editTitle.getText().toString().trim();
        String message = editMessage.getText().toString().trim();
        String recipients = spinnerRecipients.getSelectedItem().toString();

        // Check if fields are filled
        if (title.isEmpty() || message.isEmpty()) {
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
                    Toast.makeText(getContext(), "Announcement sent", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send announcement", Toast.LENGTH_SHORT).show());
    }
}
