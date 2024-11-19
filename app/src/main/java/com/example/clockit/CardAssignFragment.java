package com.example.clockit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CardAssignFragment extends Fragment {
    private DrawerLayout drawerLayout;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private EditText cardInput;
    private Button assignButton;
    private ProgressBar loadingView;
    private TextView successMessage;
    private TableLayout userDataTable;

    private static final int REFRESH_INTERVAL_MS = 5000; // Refresh every 5 seconds

    public CardAssignFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_assign, container, false);

        // Set up Toolbar and DrawerLayout
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        drawerLayout = view.findViewById(R.id.drawer_layout);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                activity, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
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
                startActivity(new Intent(getActivity(), AddClassActivity.class));
            } else if (itemId == R.id.nav_help) {
                loadFragment(new HelpFragment());
            } else if (itemId == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                loadFragment(new LoginFragment());
                Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Initialize UI elements
        cardInput = view.findViewById(R.id.cardInput);
        assignButton = view.findViewById(R.id.assignButton);
        loadingView = view.findViewById(R.id.loadingView);
        successMessage = view.findViewById(R.id.successMessage);
        userDataTable = view.findViewById(R.id.userDataTable);

        assignButton.setOnClickListener(v -> assignCard());

        // Initialize refreshHandler
        refreshHandler = new Handler();

        // Start periodic refresh
        setupAutoRefresh();

        return view;
    }

    private void setupAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchUserData(); // Fetch data from the server
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS); // Schedule next refresh
            }
        };
        refreshHandler.post(refreshRunnable); // Start the refresh
    }

    private void fetchUserData() {
        String url = "https://script.google.com/macros/s/AKfycbxNhyb8DBHOQlcpDdeMAWakOBDi1l4LcqYsPJadhzV9RrO5O1asVRDPaS3GWr1F0T5Uow/exec";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        // Clear existing table rows except header
                        int childCount = userDataTable.getChildCount();
                        if (childCount > 1) {
                            userDataTable.removeViews(1, childCount - 1);
                        }

                        // Parse the response and populate the table
                        JSONArray jsonArray = new JSONArray(response);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String name = jsonObject.optString("Name", "");
                            String uid = jsonObject.optString("UID", "");

                            if (!name.isEmpty() && !uid.isEmpty()) {
                                addTableRow(name, uid);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Error fetching data: " + error.getMessage(), Toast.LENGTH_SHORT).show());

        // Set retry policy
        RetryPolicy retryPolicy = new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        // Add request to queue
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        queue.add(stringRequest);
    }

    private void addTableRow(String name, String uid) {
        TableRow row = new TableRow(getContext());
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView nameView = new TextView(getContext());
        nameView.setText(name);
        nameView.setPadding(16, 16, 16, 16);
        nameView.setTextSize(16);

        TextView uidView = new TextView(getContext());
        uidView.setText(uid);
        uidView.setPadding(16, 16, 16, 16);
        uidView.setTextSize(16);

        row.addView(nameView);
        row.addView(uidView);

        // Alternate row colors
        if (userDataTable.getChildCount() % 2 == 0) {
            row.setBackgroundColor(Color.parseColor("#F0F0F0"));
        }

        userDataTable.addView(row);
    }

    private void assignCard() {
        String cardInfo = cardInput.getText().toString().trim();

        if (cardInfo.isEmpty()) {
            Toast.makeText(getContext(), "Please enter card information", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();

        // Hide input and button, show loading
        cardInput.setVisibility(View.GONE);
        assignButton.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);

        // Send card info to Google Sheet
        addCardInfoToSheet(cardInfo);

        // Show loading for 3 seconds
        refreshHandler.postDelayed(() -> {
            loadingView.setVisibility(View.GONE);
            successMessage.setVisibility(View.VISIBLE);

            // Reset UI after 5 seconds
            refreshHandler.postDelayed(() -> {
                successMessage.setVisibility(View.GONE);
                cardInput.setVisibility(View.VISIBLE);
                assignButton.setVisibility(View.VISIBLE);
                cardInput.setText("");
            }, 5000);
        }, 3000);
    }

    private void addCardInfoToSheet(String cardInfo) {
        ProgressDialog dialog = ProgressDialog.show(getContext(), "Adding Item", "Please wait....", true, false);

        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://script.google.com/macros/s/AKfycbzTNcTq6jIsro4B0Rz6CtHuEs-XGDw7C6S1wRgCIxgtXG3yIIpfZOPHNk8eW1OxQ9ddZw/exec",
                response -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Card information added successfully!", Toast.LENGTH_SHORT).show();
                    fetchUserData(); // Refresh the table after adding new data
                },
                error -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Failed to add card information. Please try again.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "addItem2");
                params.put("userName", cardInfo);
                return params;
            }
        };

        RetryPolicy retryPolicy = new DefaultRetryPolicy(50000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(stringRequest);
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable); // Stop the refresh when fragment is destroyed
        }
    }
}
