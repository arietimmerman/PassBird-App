/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.passbird.R;
import com.passbird.helpers.DatabaseHelper;
import com.passbird.helpers.Logger;
import com.passbird.model.Password;
import com.univocity.parsers.common.processor.RowListProcessor;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ImportActivity extends Activity {

    private HashMap<Integer, String> mapping = new HashMap<>();
    private static final String EMPTY = "...";
    private MenuItem importButton;

    private RowListProcessor rowProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)) && type != null) {
            handleSendText(intent); // Handle text being sent
        }else{
            findViewById(R.id.help_text).setVisibility(View.VISIBLE);
        }

    }

    /**
     * Handles the file that is shared with PassBird
     * @param intent Intent referring to the shared file
     */
    private void handleSendText(Intent intent) {
        findViewById(R.id.import_text).setVisibility(View.VISIBLE);
        importButton.setVisible(true);

        Uri uri = intent.getData();

        if (uri != null) {
            try {
                LinearLayout listItems = (LinearLayout) findViewById(R.id.list_items);

                CsvParserSettings parserSettings = new CsvParserSettings();
                //parserSettings.setDelimiterDetectionEnabled(true);
                //parserSettings.setQuoteDetectionEnabled(true);
                parserSettings.setLineSeparatorDetectionEnabled(true);

                CsvFormat csvFormat = new CsvFormat();
                csvFormat.setDelimiter(';');

                parserSettings.setFormat(csvFormat);

                rowProcessor = new RowListProcessor();
                parserSettings.setRowProcessor(rowProcessor);

                parserSettings.setHeaderExtractionEnabled(true);

                CsvParser parser = new CsvParser(parserSettings);

                parser.parse(new InputStreamReader(getContentResolver().openInputStream(uri)));

                String[] headers = rowProcessor.getHeaders();

                List<String> list= new ArrayList<>();
                list.add(EMPTY);
                list.add(Password.KEY_LOCATION);
                list.add(Password.KEY_DOMAIN);
                list.add(Password.KEY_PASSWORD);
                list.add(Password.KEY_TITLE);
                list.add(Password.KEY_USERNAME);

                final ArrayAdapter<String> adapter= new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                for (int i = 0; i < headers.length; i++) {
                    View view = LayoutInflater.from(listItems.getContext()).inflate(R.layout.list_item_import, listItems, false);

                    Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
                    spinner.setAdapter(adapter);

                    final Integer column = i;
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            String value = String.valueOf(adapter.getItem(position));
                            Logger.log("save", String.format("%d : %s", column, value));

                            value = EMPTY.equals(value)?null:value;

                            mapping.put(column, value);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            mapping.put(column, "");
                        }

                    });

                    listItems.addView(view);

                    TextView textView = (TextView) view.findViewById(R.id.column);
                    textView.setText(headers[column]);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_import, menu);
        importButton = menu.findItem(R.id.action_import);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_import:

                if (!mapping.values().contains(Password.KEY_USERNAME) || !mapping.values().contains(Password.KEY_PASSWORD)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Error")
                            .setMessage("At a minimum, tell which fields represent the username and which represents the password")
                            .show();
                } else {

                    Integer procesCount = 0;

                    for (String[] row : rowProcessor.getRows()) {

                        Password password = new Password();

                        for (int i = 0; i < row.length; i++) {
                            Logger.log("veld", String.format("%s : %s", mapping.get(i), row[i]));
                            password.setValue(mapping.get(i), row[i]);
                        }

                        if (password.getValue(Password.KEY_TITLE).isEmpty()) {
                            password.setValue(Password.KEY_TITLE, "Auto imported");
                        }

                        DatabaseHelper.getInstance(this).savePassword(password);

                        procesCount++;
                    }


                    Toast.makeText(getApplicationContext(), String.format("Successfully imported %d passwords", procesCount), Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }


                return true;

            case android.R.id.home:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
