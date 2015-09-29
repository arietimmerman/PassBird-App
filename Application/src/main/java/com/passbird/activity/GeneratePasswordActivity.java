/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.passbird.helpers.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import com.passbird.R;

public class GeneratePasswordActivity extends Activity {

    private int minNumbers = 5;
    private int minLetters = 5;
    private int minLength = 15;
    private int minSpecial = 2;

    private static final String numbers = "0123456789";
    private static final String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String special = "-_!?$#";

    private void generatePassword(){

        StringBuilder password = new StringBuilder();

        SecureRandom secureRandom = new SecureRandom();

        List<Character> characters = new ArrayList<>();

        for(int i=0;i< minNumbers;i++){
            characters.add(numbers.charAt(secureRandom.nextInt(numbers.length())));
        }

        for(int i=0;i< minLetters;i++){
            characters.add(letters.charAt(secureRandom.nextInt(letters.length())));
        }

        for(int i=0;i< minSpecial;i++){
            characters.add(special.charAt(secureRandom.nextInt(special.length())));
        }

        while(characters.size() < minLength){
            characters.add(letters.charAt(secureRandom.nextInt(letters.length())));
        }

        while(characters.size() > 0){
            int index = secureRandom.nextInt(characters.size());
            password.append(characters.get(index));
            characters.remove(index);
        }

        EditText passwordField = (EditText) findViewById(R.id.generated_password);

        passwordField.setText(password.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_password);

        int[] ids = {R.id.password_policy_digits,R.id.password_policy_letters,R.id.password_policy_length,R.id.password_policy_special};

        for(final int id : ids) {
            final LinearLayout linearLayout = (LinearLayout) findViewById(id);
            final SeekBar seekBar = (SeekBar) linearLayout.findViewWithTag("seekbar");
            final TextView titleText = (TextView) linearLayout.findViewWithTag("title");
            final TextView textView = (TextView) linearLayout.findViewWithTag("textview");

            switch(id){
                case R.id.password_policy_digits:
                    seekBar.setMax(10);
                    seekBar.setProgress(minNumbers);
                    textView.setText(String.valueOf(minNumbers));
                    titleText.setText(R.string.minimum_numbers);
                    break;

                case R.id.password_policy_letters:
                    seekBar.setMax(10);
                    seekBar.setProgress(minLetters);
                    textView.setText(String.valueOf(minLetters));
                    titleText.setText(R.string.minimum_letters);
                    break;
                case R.id.password_policy_length:
                    seekBar.setMax(32);
                    seekBar.setProgress(minLength);
                    textView.setText(String.valueOf(minLength));
                    titleText.setText(R.string.minimum_length);
                    break;

                case R.id.password_policy_special:
                    seekBar.setMax(10);
                    seekBar.setProgress(minSpecial);
                    textView.setText(String.valueOf(minSpecial));
                    titleText.setText(R.string.minimum_special);
                    break;
            }

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    textView.setText(String.valueOf(progress));

                    switch(id){
                        case R.id.password_policy_digits:
                            minNumbers = progress;
                            break;

                        case R.id.password_policy_letters:
                            minLetters = progress;
                            break;
                        case R.id.password_policy_length:
                            minLength = progress;
                            break;

                        case R.id.password_policy_special:
                            minSpecial = progress;
                            break;
                    }


                    generatePassword();


                }
            });
        }

        generatePassword();
    }


    private void usePassword(){
        Intent data = new Intent();

        EditText passwordField = (EditText) findViewById(R.id.generated_password);
        Logger.log("passwordField", passwordField.getText().toString());
        data.putExtra("password", passwordField.getText().toString());
        setResult(RESULT_OK, data);

        finish();
    }

    public void toggleShowPassword(View view){

        EditText passwordField = (EditText) findViewById(R.id.generated_password);
        if(passwordField.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            passwordField.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }else{
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_generate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.save_password) {
            usePassword();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
