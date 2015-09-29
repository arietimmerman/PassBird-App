/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyPair;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.passbird.R;

import com.passbird.helpers.Logger;
import com.passbird.helpers.Utils;
import com.passbird.helpers.DatabaseHelper;
import com.passbird.model.Browser;


public class RegisterActivity extends Activity {

    private final static String KEY_BROWSER_ID = "b";
    private final static String KEY_ONE_TIME_SECRET = "o";

    private RequestQueue requestQueue = null;

    private GenerateKeyPair generateKeyPairTask;

    private class GenerateKeyPair extends AsyncTask<Void, Void, KeyPair> {

        @Override
        protected KeyPair doInBackground(Void... params) {
            return Utils.generateKeyPair();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        generateKeyPairTask = new GenerateKeyPair();
        generateKeyPairTask.execute();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_browser);
    }

    public void startScanner(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan a PassBird QR Code");
        integrator.setBeepEnabled(false);
        integrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    private void processSecondReponse(JSONObject responseTwo, Browser browser) {
        Logger.log("secondResponse", responseTwo.toString());
        DatabaseHelper.getInstance(getApplicationContext()).addBrowser(browser);

        Intent data = new Intent();
        data.putExtra("message", "You have succesfully connected your browser!");
        setResult(RESULT_OK, data);

        finish();

    }

    private void processFirstReponse(JSONObject responseOne, final String browserId, final String oneTimeSecretKey) {
        if (responseOne != null) {
            try {

                final Browser browser = new Browser(browserId);

                KeyPair myKeyPair = generateKeyPairTask.get();

                String encryptedPublicKey = responseOne.getString("public_key");
                String initializationVector = responseOne.getString("initialization_vector");

                byte[] cipherBytes = Base64.decode(encryptedPublicKey, Base64.DEFAULT);
                byte[] iv = Base64.decode(initializationVector, Base64.DEFAULT);
                byte[] keyBytes = Base64.decode(oneTimeSecretKey, Base64.DEFAULT);

                SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

                Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
                cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));

                byte[] result = cipher.doFinal(cipherBytes);

                String theirPublicKey = new String(result);

                X509EncodedKeySpec x509EncodedKeySpecPrivate = new X509EncodedKeySpec(myKeyPair.getPrivate().getEncoded());

                String myPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" + Base64.encodeToString(x509EncodedKeySpecPrivate.getEncoded(), Base64.DEFAULT) + "-----END RSA PRIVATE KEY-----";

                JSONObject requestTwo = new JSONObject();
                requestTwo.put("browser_id", browserId);

                X509EncodedKeySpec x509EncodedKeySpecPublic = new X509EncodedKeySpec(myKeyPair.getPublic().getEncoded());

                JSONObject payload = new JSONObject();
                payload.put("registration_id", Utils.getRegistrationId(getApplicationContext()));
                payload.put("public_key", "-----BEGIN PUBLIC KEY-----\n" + Base64.encodeToString(x509EncodedKeySpecPublic.getEncoded(), Base64.DEFAULT) + "-----END PUBLIC KEY-----"); //my public key

                Logger.log("will encrypt", payload.toString());

                JSONObject encryptedMessage = Utils.encrypt(payload, theirPublicKey);

                requestTwo.put("message", encryptedMessage);

                Logger.log("after encrypt", encryptedMessage.toString());

                String url = getResources().getString(R.string.rest_browser_registered);

                browser.setPublic_key(theirPublicKey);
                browser.setMy_private_key(myPrivateKey);
                browser.setBrowser_name(responseOne.getString("browser_name"));

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestTwo,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                processSecondReponse(response, browser);
                            }
                        },
                        null);

                getRequestQueue().add(jsonObjectRequest);

            } catch (Exception e) {
                Logger.log("exception", e.getMessage());
                e.printStackTrace();
            }

        }
    }

    /**
     * When a QR code is scanned the onActivityResult method is called
     * It will inform the server that registration has finished. The server will forward the message to the browser
     */

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Logger.log("log","agasdgadsg");

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        Logger.log("log",String.valueOf(requestCode));
        Logger.log("log",String.valueOf(resultCode));
        Logger.log("log",String.valueOf(result));
        if (result != null && result.getContents() != null) {

            //relativelayout_progress
            RelativeLayout progress = (RelativeLayout) findViewById(R.id.relativelayout_progress);
            progress.setVisibility(View.VISIBLE);

            String contents = result.getContents();

            Logger.log("receoved", "activity result");

            try {

                JSONObject jsonObject = new JSONObject(contents);

                final String browserId = jsonObject.getString(KEY_BROWSER_ID);
                final String oneTimeSecretKey = jsonObject.getString(KEY_ONE_TIME_SECRET);

                JSONObject requestOne = new JSONObject();
                requestOne.put("browser_id", browserId);

                Logger.log("will send", requestOne.toString());

                String url = getResources().getString(R.string.rest_browser_scanned);

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestOne,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                processFirstReponse(response, browserId, oneTimeSecretKey);
                            }
                        },
                        null);

                getRequestQueue().add(jsonObjectRequest);


            } catch (JSONException e) {
                Logger.log("JSONException", e.getMessage());
                //failed to scan. Rescan
                Intent data = new Intent();
                data.putExtra("message", "An error occured. Please contact Arie");
                setResult(RESULT_CANCELED, data);
                finish();
            }

            Logger.log("result", contents);
            // Handle successful scan
        } else if (resultCode == RESULT_CANCELED) {
            finish();
        }
    }


    private RequestQueue getRequestQueue() {
        if (requestQueue == null) requestQueue = Volley.newRequestQueue(this);

        return requestQueue;
    }
}

