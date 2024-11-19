package com.example.clockit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StudentDetailActivity extends AppCompatActivity {
    TextView nameTextView, idTextView, emailTextView;
    Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);

        // Link the UI elements
        nameTextView = findViewById(R.id.student_name);
        idTextView = findViewById(R.id.student_id);
        emailTextView = findViewById(R.id.student_email);
        backButton = findViewById(R.id.backButton);

        // Get data from Intent
        String studentName = getIntent().getStringExtra("studentName");
        String studentID = getIntent().getStringExtra("studentID");
        String studentEmail = getIntent().getStringExtra("studentEmail");

        // Set data to TextViews
        nameTextView.setText(studentName);
        idTextView.setText(studentID);
        emailTextView.setText(studentEmail);

        // Set up the Back button to finish the activity
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the activity and go back to the previous one
            }
        });
    }
}
