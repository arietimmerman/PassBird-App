/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.ShortenedDigest;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.generators.KDF2BytesGenerator;
import org.spongycastle.crypto.kems.RSAKeyEncapsulation;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.crypto.util.PublicKeyFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.passbird.activity.MainActivity;

/**
 * Utils class
 */
public class Utils {

    private static final String PROPERTY_PRIVATE_KEY = "private_key";
    private static final String PROPERTY_PUBLIC_KEY = "public_key";

    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "appVersion";

    public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId != null && registrationId.isEmpty()) {
            Log.i("main", "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i("main", "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    public static SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static KeyPair generateKeyPair(){
        KeyPair keys = null;

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = new SecureRandom();
            keyGen.initialize(2048, random);

            keys = keyGen.generateKeyPair();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return keys;
    }

    public static void storeKeyPair(Context context, KeyPair keys){

        final SharedPreferences prefs = Utils.getGCMPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.PROPERTY_PRIVATE_KEY, Base64.encodeToString(keys.getPrivate().getEncoded(), Base64.DEFAULT));
        editor.putString(Utils.PROPERTY_PUBLIC_KEY, Base64.encodeToString(keys.getPublic().getEncoded(), Base64.DEFAULT));
        editor.apply();

    }

    public static String decrypt(JSONObject jsonObject, String privateKeyString){

        return decrypt(jsonObject,getPrivateKey(privateKeyString));

    }

    @SuppressWarnings("WeakerAccess")
    public static String decrypt(JSONObject jsonObject, AsymmetricKeyParameter privateKey){

        try {
            byte[] encapsulation = Base64.decode(jsonObject.getString("encapsulation"),Base64.DEFAULT);
            byte[] iv = Base64.decode(jsonObject.getString("iv"),Base64.DEFAULT);
            byte[] ciphertext = Base64.decode(jsonObject.getString("ciphertext"), Base64.DEFAULT);

            KDF2BytesGenerator kdf = new KDF2BytesGenerator(new ShortenedDigest(new SHA256Digest(), 20));
            SecureRandom secureRandom = new SecureRandom();
            RSAKeyEncapsulation rsaKeyEncapsulation = new RSAKeyEncapsulation(kdf, secureRandom);

            rsaKeyEncapsulation.init(privateKey);

            //initializes the cipher
            CBCBlockCipher cipher = new CBCBlockCipher(new AESEngine());

            //padded with PKCS7Padding
            PaddedBufferedBlockCipher paddedCipher = new PaddedBufferedBlockCipher(cipher);

            KeyParameter keyParameter = (KeyParameter) rsaKeyEncapsulation.decrypt(encapsulation,32);

            CipherParameters ivAndKey = new ParametersWithIV(keyParameter, iv);

            paddedCipher.reset();
            paddedCipher.init(false, ivAndKey);

            byte[] buf = new byte[paddedCipher.getOutputSize(ciphertext.length)];
            int len = paddedCipher.processBytes(ciphertext, 0, ciphertext.length, buf, 0);
            len += paddedCipher.doFinal(buf, len);

            // remove padding
            byte[] out2 = new byte[len];
            System.arraycopy(buf, 0, out2, 0, len);

            return new String(out2);

        } catch (JSONException | InvalidCipherTextException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static AsymmetricKeyParameter getPrivateKey(String privateKeyString){
        AsymmetricKeyParameter privateKey = null;

        privateKeyString = privateKeyString.replaceAll("(-+BEGIN RSA PRIVATE KEY-+\\r?\\n|-+END RSA PRIVATE KEY-+\\r?\\n?)", "");

        try {
            privateKey = PrivateKeyFactory.createKey(Base64.decode(privateKeyString, Base64.DEFAULT));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return privateKey;
    }


    public static AsymmetricKeyParameter getPublicKey(String publicKeyString){
        Logger.log("publicKeyString",publicKeyString);
        AsymmetricKeyParameter publicKey = null;
        publicKeyString = publicKeyString.replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "");
        try {
            publicKey = PublicKeyFactory.createKey(Base64.decode(publicKeyString, Base64.DEFAULT));


        } catch (IOException e) {
            Logger.log("fout",e.getMessage());
            e.printStackTrace();
        }

        Logger.log("return","return");

        return publicKey;
    }

    public static JSONObject encrypt(JSONObject plain, String publicKeyString){

        return encrypt(plain,getPublicKey(publicKeyString));

    }

    public static JSONObject encrypt(JSONObject plain, AsymmetricKeyParameter publicKey){

        JSONObject result = new JSONObject();

        //encapsulates the encrypted symetric key using the public key
        KDF2BytesGenerator kdf = new KDF2BytesGenerator(new ShortenedDigest(new SHA256Digest(), 20));
        SecureRandom secureRandom = new SecureRandom();
        RSAKeyEncapsulation rsaKeyEncapsulation = new RSAKeyEncapsulation(kdf, secureRandom);

        byte[] out = new byte[256];

        rsaKeyEncapsulation.init(publicKey);

        //store the encrypted key in "out" and return the unencrypted key in keyParameter
        KeyParameter keyParameter = (KeyParameter)rsaKeyEncapsulation.encrypt(out,32);

        Logger.log("encryption key", Base64.encodeToString(keyParameter.getKey(), Base64.DEFAULT));

        //initializes the cipher
        CBCBlockCipher cipher = new CBCBlockCipher(new AESEngine());
        //padded with PKCS7Padding
        PaddedBufferedBlockCipher paddedCipher = new PaddedBufferedBlockCipher(cipher);

        Logger.log("blocksize: ",String.valueOf(cipher.getBlockSize()));
        //generates the iv
        byte[] ivBytes = new byte[cipher.getBlockSize()];
        secureRandom.nextBytes(ivBytes);

        CipherParameters ivAndKey = new ParametersWithIV(keyParameter, ivBytes);
        paddedCipher.reset();
        paddedCipher.init(true, ivAndKey);

        try {
            byte[] dataToEncrypt = plain.toString().getBytes();
            byte[] encryptedData=new byte[paddedCipher.getOutputSize(dataToEncrypt.length)];


            int numberOfEncryptedBytes = paddedCipher.processBytes(dataToEncrypt,0,dataToEncrypt.length,encryptedData,0);
            numberOfEncryptedBytes += paddedCipher.doFinal(encryptedData, numberOfEncryptedBytes);


            byte[] out2 = new byte[numberOfEncryptedBytes];
            System.arraycopy(encryptedData, 0, out2, 0, numberOfEncryptedBytes);

            Logger.log("In buffer", String.valueOf(dataToEncrypt.length));
            Logger.log("zonder buffer",String.valueOf(out2.length));

            result.put("ciphertext",Base64.encodeToString(out2,Base64.DEFAULT));
            result.put("iv",Base64.encodeToString(ivBytes,Base64.DEFAULT));
            result.put("encapsulation",Base64.encodeToString(out,Base64.DEFAULT));
        } catch (Exception e) {
            Logger.log("Fout",e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public static String jsonObjectGetString(JSONObject jsonObject, String key){
        String result = null;
        try {
            result = jsonObject!=null?jsonObject.getString(key):"";
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }
    public static JSONObject jsonObjectGetJSONObject(JSONObject jsonObject, @SuppressWarnings("SameParameterValue") String key){
        JSONObject result = new JSONObject();
        try {
            result = jsonObject!=null?jsonObject.getJSONObject("message"):new JSONObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static JSONObject getJSONObject(String jsonString){
        JSONObject result = new JSONObject();

        try {
            result = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }


}
