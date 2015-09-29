/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.passbird.helpers.DatabaseHelper;
import com.passbird.R;

import com.passbird.helpers.Logger;
import com.passbird.model.Browser;

public class BrowsersActivity extends ListActivity {

    private ArrayList<Browser> browserList;
    private DatabaseHelper databaseHelper;
    private BrowserArrayAdapter browserArrayAdapter;

    private class BrowserArrayAdapter extends ArrayAdapter{

        @Override
        public int getCount() {
            Logger.log("getCount");
            return browserList.size();
        }

        public BrowserArrayAdapter(Context context, List objects) {
            super(context, android.R.layout.simple_list_item_single_choice, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_browser, parent, false);
            }

            TextView textView = (TextView)convertView.findViewById(android.R.id.text1);
            Browser browser = browserList.get(position);
            textView.setText(browser.getBrowser_name());

            ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.secondary_action);
            imageButton.setTag(position);

            final BrowserArrayAdapter parentAdapter = this;

            imageButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Integer position = (Integer)view.getTag();

                            Log.w("Delete", String.format("id: %d", position));
                            databaseHelper.deleteBrowser(browserList.get(position));

                            browserList.remove(browserList.get(position));

                            Log.w("Size", String.format("size: %d", browserList.size()));
                            parentAdapter.notifyDataSetChanged();

                        }
                    });

            return convertView;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Logger.log("onstart", "onstart");
        browserList = DatabaseHelper.getInstance(this).getAllBrowsers();

        browserArrayAdapter.notifyDataSetChanged();

        ArrayList<Browser> browserList = DatabaseHelper.getInstance(this).getAllBrowsers();
        for(Browser browser : browserList){
            Logger.log("Browser", String.valueOf(browser.getId()));
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browsers);

        databaseHelper = DatabaseHelper.getInstance(this);

        browserList = DatabaseHelper.getInstance(this).getAllBrowsers();
        browserArrayAdapter = new BrowserArrayAdapter(this, browserList);
        setListAdapter(browserArrayAdapter);
    }

    private void showMessage(String message){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("You are lucky");
        alertDialog.setMessage(message);
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.weblogin:

                startActivity(new Intent(this, LoginActivity.class));

                return true;

            case R.id.register_browser:

                startActivityForResult(new Intent(this, RegisterActivity.class), 1);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            showMessage(data.getStringExtra("message"));
        }
    }
}
