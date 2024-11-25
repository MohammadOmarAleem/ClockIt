// Announcement.java
package com.example.clockit;

public class Announcement {
    private String title;
    private String date;
    private String time;
    private String message;

    public Announcement() {
        // Required empty constructor for Firebase
    }

    public Announcement(String title, String date, String time, String message) {
        this.title = title;
        this.date = date;
        this.time = time;
        this.message = message;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
