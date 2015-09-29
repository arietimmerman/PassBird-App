/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.helpers;

import android.util.Log;

import com.passbird.BuildConfig;

public class Logger {

    public static void log(String message){
        log("log",message);
    }

    public static void log(String tag, String message){
        if (BuildConfig.DEBUG) {
            Log.w("log", message);
        }
    }

}
