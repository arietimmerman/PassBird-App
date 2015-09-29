/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.passbird.R;
import com.passbird.helpers.DatabaseHelper;
import com.passbird.helpers.Logger;
import com.passbird.model.Password;

public class Edit extends Activity {

    private DatabaseHelper databaseHelper;
    private Password password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        databaseHelper = DatabaseHelper.getInstance(this);

        Bundle extras = getIntent().getExtras();

        String action = extras!=null?extras.getString("action"):"";
        int passwordId = extras!=null?extras.getInt("id"):-1;

        if("edit".equals(action)){
            password = databaseHelper.getPassword(passwordId);

            EditText titleEditText = (EditText) findViewById(R.id.title);
            titleEditText.setText(password.getValue(Password.KEY_TITLE));
            EditText domainEditText = (EditText) findViewById(R.id.domain);
            domainEditText.setText(password.getValue(Password.KEY_DOMAIN));
            EditText usernameEditText = (EditText) findViewById(R.id.username);
            usernameEditText.setText(password.getValue(Password.KEY_USERNAME));
            EditText passwordEditText = (EditText) findViewById(R.id.password);
            passwordEditText.setText(password.getValue(Password.KEY_PASSWORD));
            EditText noteEditTExt = (EditText) findViewById(R.id.note);
            noteEditTExt.setText((String)password.getExtra("note"));
        }else{
            password = new Password();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.save_password) {
            savePassword();
            return true;
        }else if(id == R.id.delete_password){
            DatabaseHelper.getInstance(this).deletePassword(password);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void savePassword(){

        EditText titleEditText = (EditText) findViewById(R.id.title);
        EditText domainEditText = (EditText) findViewById(R.id.domain);
        EditText usernameEditText = (EditText) findViewById(R.id.username);
        EditText passwordEditText = (EditText) findViewById(R.id.password);
        EditText noteEditTExt = (EditText) findViewById(R.id.note);

        password.setValue(Password.KEY_TITLE,titleEditText.getText().toString());
        password.setValue(Password.KEY_DOMAIN, domainEditText.getText().toString());
        password.setValue(Password.KEY_USERNAME, usernameEditText.getText().toString());
        password.setValue(Password.KEY_PASSWORD, passwordEditText.getText().toString());
        password.putExtra("note", noteEditTExt.getText().toString());

        databaseHelper.savePassword(password);

        Toast.makeText(this, "Password saved", Toast.LENGTH_LONG).show();

        finish();

    }

    public void showGeneratePassword(View view){
        startActivityForResult(new Intent(this, GeneratePasswordActivity.class), 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.log("result", "onActivityResult");

        if (resultCode == RESULT_OK) {
            EditText passwordEditText = (EditText) findViewById(R.id.password);
            Logger.log("new password", data.getStringExtra("password"));
            passwordEditText.setText(data.getStringExtra("password"));

            Toast.makeText(this, "Don't fotget to save", Toast.LENGTH_LONG).show();
        }
    }

}