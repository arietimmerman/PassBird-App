/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;

import java.util.concurrent.ExecutionException;

import com.passbird.helpers.Logger;
import com.passbird.helpers.Utils;
import com.passbird.model.Password;
import com.passbird.R;
import com.passbird.helpers.DatabaseHelper;
import com.passbird.model.Browser;

public class LoginActivity extends Activity {

    private final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);

    private String location;
    private String title;
    private String username;
    private String passwordString;
    private String elementId;
    private String elementName;
    private String sessionID;
    private String browserId;
    private String domain;

    private RequestQueue requestQueue;
    private Password password;

    private PreparePublicKey preparePublicKey;

    private static final int ACTIVITY_ID = 44;

    private class PreparePublicKey extends AsyncTask<Browser, Void, AsymmetricKeyParameter> {
        @Override
        protected AsymmetricKeyParameter doInBackground(Browser... params) {
            return Utils.getPublicKey(params[0].getPublic_key());
        }
    }

    private void showNoSession() {
        setContentView(R.layout.activity_login_no_session);

        TextView loginTitle = (TextView) findViewById(R.id.login_title);
        loginTitle.setText("Are you connected?");
    }

    private void handleResponse(JSONObject response) throws Exception {

        if ("{}".equals(response.toString())) { //no session is open
            throw new Exception("aasdgasg");
        }

        browserId = Utils.jsonObjectGetString(response, "browser_id");
        sessionID = Utils.jsonObjectGetString(response, "id");

        Browser browser = databaseHelper.getBrowser(browserId);

        //preload public key
        preparePublicKey = new PreparePublicKey();
        preparePublicKey.execute(browser);

        JSONObject encryptedMessage = Utils.jsonObjectGetJSONObject(response, "message");

        if ("{}".equals(encryptedMessage.toString())) {
            throw new Exception("aasdgasg");
        }

        JSONObject message = Utils.getJSONObject(Utils.decrypt(encryptedMessage, browser.getMy_private_key()));

        Logger.log("Received", message.toString());

        location = Utils.jsonObjectGetString(message, "location");
        domain = Utils.jsonObjectGetString(message, "domain");
        title = Utils.jsonObjectGetString(message, "title");
        username = Utils.jsonObjectGetString(message, "username");
        passwordString = Utils.jsonObjectGetString(message, "password");
        elementId = Utils.jsonObjectGetString(message, "element_id");
        elementName = Utils.jsonObjectGetString(message, "element_name");

        Logger.log("search by domain", domain);

        password = databaseHelper.getPasswordByDomain(domain);

        setContentView(R.layout.activity_login);

        if (password == null) { //no password is known for the domain

            Logger.log("register", "register");

            TextView loginTitle = (TextView) findViewById(R.id.login_title);
            loginTitle.setText(domain);
            ((Button)findViewById(R.id.button_allow)).setText(R.string.allow_and_save);
            findViewById(R.id.edit).setVisibility(View.VISIBLE);

            updateUsernamePassword(username, passwordString);

            EditText passwordField = (EditText) findViewById(R.id.password);
            passwordField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideKeyboard(v);
                    }
                }
            });

        } else {
            Logger.log("login", "login");

            TextView loginTitle = (TextView) findViewById(R.id.login_title);
            loginTitle.setText(domain);

            updateUsernamePassword(password.getValue(Password.KEY_USERNAME), password.getValue(Password.KEY_PASSWORD));

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);

        checkForRequest();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);

        View editView = findViewById(R.id.edit);
        if(editView != null && editView.getVisibility() == View.VISIBLE){
            menu.findItem(R.id.edit_button).setVisible(false);
            menu.findItem(R.id.refresh_button).setVisible(true);
        }else{
            menu.findItem(R.id.edit_button).setVisible(true);
            menu.findItem(R.id.refresh_button).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.edit_button:

                invalidateOptionsMenu();
                expand(findViewById(R.id.edit));
                ((Button)findViewById(R.id.button_allow)).setText(R.string.allow_and_save);

                return true;

            case R.id.refresh_button:

                Toast.makeText(getApplicationContext(),"Tries to copy your username and password from the open web page",Toast.LENGTH_LONG).show();

                JSONObject jsonObject = new JSONObject();

                try {
                    jsonObject.put("inResponseto", sessionID);
                    jsonObject.put("waitForResponse", true);

                    JSONObject message = new JSONObject();
                    message.put("action", "updatePassword");

                    jsonObject.put("message", encrypt(message));

                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.rest_generic), jsonObject.toString(),
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {

                                    Log.w("received response ", response.toString());

                                    JSONObject encryptedMessage = null;
                                    try {
                                        encryptedMessage = response.getJSONObject("message");

                                        Browser browser = databaseHelper.getBrowser(browserId);
                                        JSONObject message = new JSONObject(Utils.decrypt(encryptedMessage, browser.getMy_private_key()));

                                        username = Utils.jsonObjectGetString(message, "username");
                                        passwordString = Utils.jsonObjectGetString(message, "password");

                                        updateUsernamePassword(username, passwordString);
                                    } catch (JSONException e) {
                                        e.printStackTrace();

                                        Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
                                        finish();

                                    }

                                    Log.w("response", response.toString());
                                }
                            }, null);


                    requestQueue.add(request);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void updateUsernamePassword(String username, String password) {
        EditText usernameField = (EditText) findViewById(R.id.username);
        usernameField.setText(username);

        EditText passwordField = (EditText) findViewById(R.id.password);
        passwordField.setText(password);
    }

    /**
     * on button click
     *
     * @param view view
     */
    public void allowPassword(View view) {

        sendPassword(password.getValue(Password.KEY_PASSWORD), password.getValue(Password.KEY_USERNAME), sessionID);

        finish();

    }

    public void savePassword(View view) {
        EditText passwordField = (EditText) findViewById(R.id.password);
        String passwordText = passwordField.getText().toString();

        EditText usernameField = (EditText) findViewById(R.id.username);
        username = usernameField.getText().toString();

        JSONObject extra = new JSONObject();
        try {
            extra.put("elementId", elementId);
            extra.put("elementName", elementName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (password == null) {
            password = new Password();
        }

        password.setValue(Password.KEY_LOCATION,location);
        password.setValue(Password.KEY_DOMAIN,domain);
        password.setValue(Password.KEY_TITLE, title);
        password.setValue(Password.KEY_USERNAME, username);
        password.setValue(Password.KEY_ICON, Password.findAutoIcon(domain));
        password.setValue(Password.KEY_PASSWORD, passwordText);

        sendPassword(password.getValue(Password.KEY_PASSWORD), password.getValue(Password.KEY_USERNAME), sessionID);

        databaseHelper.savePassword(password);

        Toast.makeText(this, "Password send and saved", Toast.LENGTH_SHORT).show();

        finish();

    }

    private void sendPassword(final String password, final String username, final String inResponseTo) {

        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("inResponseto", inResponseTo);
            jsonObject.put("closeSession", true);

            JSONObject message = new JSONObject();
            message.put("action", "sendPassword");
            message.put("password", password);
            message.put("username", username);

            jsonObject.put("message", encrypt(message));

            Logger.log("sendPassword", jsonObject.toString());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.rest_generic), jsonObject.toString(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.w("response", response.toString());
                        }
                    }, null);

            requestQueue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void ignorePassword(View view) {
        password.putExtra("ignore", true);

        databaseHelper.savePassword(password);

        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("inResponseto", sessionID);

            JSONObject message = new JSONObject();
            message.put("action", "ignore");

            jsonObject.put("message", encrypt(message));

            Logger.log("sendPassword", jsonObject.toString());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.rest_generic), jsonObject.toString(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.w("response", response.toString());
                        }
                    }, null);

            requestQueue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        finish();
    }

    private JSONObject encrypt(JSONObject plain) {
        JSONObject result = null;
        try {
            result = Utils.encrypt(plain, preparePublicKey.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return result;
    }

    public void showGeneratePassword(View view) {
        startActivityForResult(new Intent(this, GeneratePasswordActivity.class), ACTIVITY_ID);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.log("result", "onActivityResult");

        if (requestCode == ACTIVITY_ID && resultCode == RESULT_OK) {

            EditText passwordField = (EditText) findViewById(R.id.password);
            Logger.log("new password", data.getStringExtra("password"));
            passwordField.setText(data.getStringExtra("password"));
        }
    }

    public void openBrowserRegister(View view) {
        startActivityForResult(new Intent(this, RegisterActivity.class), 1);
    }

    private void checkForRequest() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {

                String sessionId = getIntent().getStringExtra("sessionId");
                String browserId = getIntent().getStringExtra("browserId");

                if(sessionId == null || browserId == null){
                    return "No request found";
                }

                requestQueue = Volley.newRequestQueue(getApplicationContext());

                JsonObjectRequest request;

                JSONObject jsonObject = new JSONObject();

                try {

                    jsonObject.put("waitForResponse", true);
                    jsonObject.put("inResponseto", sessionId);

                    JSONObject message = new JSONObject();
                    message.put("action", "getPasswordRequest");
                    Browser browser = databaseHelper.getBrowser(browserId);

                    if (browser == null) {
                        return "Received an illegal request from an unregistered link";
                    }

                    jsonObject.put("message", Utils.encrypt(message, Utils.getPublicKey(browser.getPublic_key())));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                request = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.rest_generic), jsonObject.toString(),
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.w("response", response.toString());
                                try {
                                    handleResponse(response);
                                } catch (Exception e) {
                                    showNoSession();
                                }
                            }
                        }, null);


                request.setRetryPolicy(new DefaultRetryPolicy(2000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                requestQueue.add(request);

                return null;
            }

            @Override
            protected void onPostExecute(String result) {

                if(result != null){
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                    finish();
                }

            }
        }.execute(null, null, null);
    }

    public static void expand(final View v) {
        v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }


}
