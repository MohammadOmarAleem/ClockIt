package com.example.clockit;

public class Student {
    private String name;
    private String studentId; // Rename to studentId to match Firebase
    private String email;

    public Student() {
        // Default constructor required for calls to DataSnapshot.getValue(Student.class)
    }

    public Student(String name, String studentId, String email) {
        this.name = name;
        this.studentId = studentId;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getStudentId() {
        return studentId; // Getter for studentId
    }

    public String getEmail() {
        return email;
    }
}
