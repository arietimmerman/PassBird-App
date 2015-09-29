/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.IconTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.joanzapata.android.iconify.Iconify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.passbird.helpers.Logger;
import com.passbird.model.Password;
import com.passbird.R;
import com.passbird.helpers.Utils;
import com.passbird.helpers.DatabaseHelper;

public class MainActivity extends ListActivity {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private GoogleCloudMessaging gcm;
    private String regid;
    private Context context;

    private final String SENDER_ID = "449435541952";

    private ArrayList<Password> passwordList;
    private PasswordArrayAdapter passwordArrayAdapter;

    private class PasswordArrayAdapter extends ArrayAdapter{

        @Override
        public int getCount() {
            return passwordList.size();
        }

        public PasswordArrayAdapter(Context context, List objects) {
            super(context, android.R.layout.simple_list_item_single_choice, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_password, parent, false);
            }

            TextView textView = (TextView)convertView.findViewById(android.R.id.text1);
            TextView textViewDomain = (TextView)convertView.findViewById(android.R.id.text2);
            IconTextView icon = (IconTextView)convertView.findViewById(R.id.icon_text_view);

            Password password = passwordList.get(position);
            LinearLayout listItem = (LinearLayout) convertView.findViewById(R.id.primary_target);

            textView.setText(password.getValue(Password.KEY_TITLE));
            textViewDomain.setText(password.getValue(Password.KEY_DOMAIN));

            icon.setText("{fa-" + password.getValue(Password.KEY_ICON) + "}");
            Iconify.addIcons(icon);
            listItem.setTag(position);

            listItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Integer position = (Integer)view.getTag();

                    Log.w("Clicked", String.format("id: %d", position));
                    showEdit(position);

                }
            });

            return convertView;
        }


    }

    private void showEdit(Integer position){
        Password password = passwordList.get(position);

        Intent intent = new Intent(this, Edit.class);
        intent.putExtra("id", password.getId());
        intent.putExtra("action","edit");

        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        passwordList = DatabaseHelper.getInstance(this).getAllPasswords();

        passwordArrayAdapter.notifyDataSetChanged();

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = Utils.getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i("main", "No valid Google Play Services APK found.");
        }

        setContentView(R.layout.main);

        passwordList = DatabaseHelper.getInstance(this).getAllPasswords();
        passwordArrayAdapter = new PasswordArrayAdapter(this, passwordList);
        setListAdapter(passwordArrayAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void showMessage(String title, String message){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Well done!");
        alertDialog.setMessage(message);
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.weblogin:

                startActivity(new Intent(this, LoginActivity.class));

                return true;

            case R.id.browsers:

                startActivity(new Intent(this, BrowsersActivity.class));

                return true;

            case R.id.register_browser:

                startActivityForResult(new Intent(this, RegisterActivity.class), 1);

                return true;

            case R.id.import_passwords:

                startActivityForResult(new Intent(this, ImportActivity.class), 1);

                return true;


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            showMessage("Browser registered",data.getStringExtra("message"));
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                showMessage("Device not supported","Seems like your device does not support push messages");
                finish();
            }
            return false;
        }
        return true;
    }

    private void registerInBackground() {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                Logger.log("main", "start async task");
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

        }.execute(null, null, null);
    }



    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = Utils.getGCMPreferences(context);
        int appVersion = Utils.getAppVersion(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.PROPERTY_REG_ID, regId);
        editor.putInt(Utils.PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    public void newPassword(View view){
        startActivity(new Intent(this, Edit.class));
    }

}
