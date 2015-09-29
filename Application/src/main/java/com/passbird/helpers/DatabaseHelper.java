/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import com.passbird.model.Password;
import com.passbird.model.Browser;

/**
 * Used for querying the sqlite database
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper databaseHelper;

    private static final String DATABASE_NAME = "test";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME_PASSWORD = "password";
    private static final String TABLE_NAME_BROWSER = "browser";

    private static final String KEY_ID = "id";

//    private static final String KEY_ELEMENT_ID = "element_id";
//    private static final String KEY_ELEMENT_NAME = "element_name";


    //browser table
    private static final String KEY_BROWSER_ID = "browser_id";
    private static final String KEY_PUBLIC_KEY = "secret_key";
    private static final String KEY_MY_PRIVATE_KEY = "my_private_key";
    private static final String KEY_BROWSER_NAME = "browser_name";

    public static DatabaseHelper getInstance(Context context){
        if(databaseHelper == null){
            databaseHelper =  new DatabaseHelper(context);
        }

        return databaseHelper;
    }

    private static final String TABLE_CREATE_PASSWORD =
            "CREATE TABLE " + TABLE_NAME_PASSWORD + "("
                    + KEY_ID + " INTEGER PRIMARY KEY,"
                    + Password.KEY_LOCATION + " TEXT,"
                    + Password.KEY_DOMAIN + " TEXT,"
                    + Password.KEY_TITLE + " TEXT,"
                    + Password.KEY_USERNAME + " TEXT,"
                    + Password.KEY_ICON + " TEXT,"
                    + Password.KEY_EXTRA + " TEXT,"
//                    + KEY_ELEMENT_ID + " TEXT,"
//                    + KEY_ELEMENT_NAME + " TEXT,"
                    + Password.KEY_PASSWORD + " TEXT" + ")";

    private static final String TABLE_CREATE_BROWSER =
            "CREATE TABLE " + TABLE_NAME_BROWSER + "("
                    + KEY_ID + " INTEGER PRIMARY KEY,"
                    + KEY_BROWSER_ID + " TEXT,"
                    + KEY_PUBLIC_KEY + " TEXT,"
                    + KEY_MY_PRIVATE_KEY + " TEXT,"
                    + KEY_BROWSER_NAME + " TEXT" + ")";

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_PASSWORD);
        db.execSQL(TABLE_CREATE_BROWSER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


    }

    public void savePassword(Password password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(Password.KEY_LOCATION, password.getValue(Password.KEY_LOCATION));
        values.put(Password.KEY_DOMAIN, password.getValue(Password.KEY_DOMAIN));
        values.put(Password.KEY_TITLE, password.getValue(Password.KEY_TITLE));
        values.put(Password.KEY_USERNAME, password.getValue(Password.KEY_USERNAME));
        values.put(Password.KEY_ICON, password.getValue(Password.KEY_ICON));
        values.put(Password.KEY_EXTRA, password.getExtra().toString());
        values.put(Password.KEY_PASSWORD, password.getValue(Password.KEY_PASSWORD));

        if(password.getId() == -1) {
            Logger.log("save", "insert");
            db.insert(TABLE_NAME_PASSWORD, null, values);
        }else {
            Logger.log("save", "update");
            db.update(TABLE_NAME_PASSWORD, values, "id = ?", new String[]{String.valueOf(password.getId())});
        }

        db.close();
    }

    public Password getPassword(int id) {
        Password password = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_PASSWORD + " WHERE " + KEY_ID + "=?", new String[]{String.valueOf(id)});

        if (cursor != null && cursor.moveToFirst()) {
            cursor.moveToFirst();
            password = new Password(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),cursor.getString(7));
        }

        if(cursor != null) cursor.close();

        return password;
    }

    public Browser getBrowser(String browserId) {
        Browser browser  = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_BROWSER + " WHERE " + KEY_BROWSER_ID + "=?", new String[]{browserId});

        if (cursor != null && cursor.moveToFirst()) {
            cursor.moveToFirst();
            browser = new Browser(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
        }

        if(cursor != null) cursor.close();

        return browser;
    }

    public Password getPasswordByLocation(String location) {
        Password password = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_PASSWORD + " WHERE " + Password.KEY_LOCATION + "=?", new String[] { location });

        if (cursor != null && cursor.moveToFirst()) {
            password = new Password(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),cursor.getString(7));
        }

        if(cursor != null) cursor.close();

        return password;
    }

    public Password getPasswordByDomain(String domain) {
        Password password = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_PASSWORD + " WHERE " + Password.KEY_DOMAIN + "=?", new String[]{domain});

        if (cursor != null && cursor.moveToFirst()) {
            password = new Password(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),cursor.getString(7));
        }

        if(cursor != null) cursor.close();

        return password;
    }

    public void deletePassword(Password password) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_PASSWORD,KEY_ID + "=?",new String[] { String.valueOf(password.getId()) });
    }

    public ArrayList<Password> getAllPasswords() {
        ArrayList<Password> passwordList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_NAME_PASSWORD;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Password password = new Password(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),cursor.getString(7));

                passwordList.add(password);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return passwordList;
    }

    public void addBrowser(Browser browser) {
        SQLiteDatabase db = this.getWritableDatabase();

        Logger.log("save browser", browser.toString());

        ContentValues values = new ContentValues();

        values.put(KEY_BROWSER_ID, browser.getBrowser_id());
        values.put(KEY_PUBLIC_KEY, browser.getPublic_key());
        values.put(KEY_MY_PRIVATE_KEY, browser.getMy_private_key());
        values.put(KEY_BROWSER_NAME, browser.getBrowser_name());

        db.insert(TABLE_NAME_BROWSER, null, values);
        db.close();
    }

    public void deleteBrowser(Browser browser) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_BROWSER, KEY_ID + "=?", new String[]{String.valueOf(browser.getId())});
    }

    public ArrayList<Browser> getAllBrowsers() {
        ArrayList<Browser> browserList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NAME_BROWSER;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Browser browser = new Browser(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
                browserList.add(browser);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return browserList;
    }

}
