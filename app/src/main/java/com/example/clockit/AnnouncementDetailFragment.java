package com.example.clockit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class AnnouncementDetailFragment extends Fragment {

    private TextView titleTextView, dateTimeTextView, participantsTextView, messageTextView;

    public AnnouncementDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_announcement_detail, container, false);

        // Set up Toolbar with Back Button
        Toolbar toolbar = view.findViewById(R.id.toolbar_detail);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        // Initialize TextViews
        titleTextView = view.findViewById(R.id.text_title_detail);
        dateTimeTextView = view.findViewById(R.id.text_date_time_detail);
        participantsTextView = view.findViewById(R.id.text_participants_detail);
        messageTextView = view.findViewById(R.id.text_message_detail);

        // Retrieve and display the data
        Bundle args = getArguments();
        if (args != null) {
            String title = args.getString("title");
            String dateTime = args.getString("dateTime");
            String participants = args.getString("participants");
            String message = args.getString("message");

            titleTextView.setText(title);
            dateTimeTextView.setText(dateTime);
            participantsTextView.setText(participants);
            messageTextView.setText(message);
        }

        return view;
    }
}
