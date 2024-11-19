package com.example.clockit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "ClockIt.db";
    public static final String TABLE_NAME = "users";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_ACCOUNT_TYPE = "accountType"; // User or Admin

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table with email, username, password, and accountType columns
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_EMAIL + " TEXT PRIMARY KEY, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_ACCOUNT_TYPE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert a new user into the database
    public boolean insertUser(String email, String username, String password, String accountType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_EMAIL, email);
        contentValues.put(COLUMN_USERNAME, username);
        contentValues.put(COLUMN_PASSWORD, password);
        contentValues.put(COLUMN_ACCOUNT_TYPE, accountType);

        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;  // Return true if insertion succeeded
    }

    // Check if the user already exists by either email or username
    public boolean checkUserExists(String identifier) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + "=? OR " + COLUMN_USERNAME + "=?", new String[]{identifier, identifier});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    // Verify login credentials with either email or username
    public boolean verifyCredentials(String identifier, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE (" + COLUMN_EMAIL + "=? OR " + COLUMN_USERNAME + "=?) AND " + COLUMN_PASSWORD + "=?", new String[]{identifier, identifier, password});
        boolean valid = (cursor.getCount() > 0);
        cursor.close();
        return valid;
    }

    // Get the account type of a user
    public String getAccountType(String identifier) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ACCOUNT_TYPE + " FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + "=? OR " + COLUMN_USERNAME + "=?", new String[]{identifier, identifier});
        if (cursor.moveToFirst()) {
            String accountType = cursor.getString(0);
            cursor.close();
            return accountType;
        }
        cursor.close();
        return null;
    }
}

