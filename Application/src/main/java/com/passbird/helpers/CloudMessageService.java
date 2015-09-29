/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.helpers;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.passbird.activity.LoginActivity;
import com.passbird.R;

public class CloudMessageService extends IntentService {
    private static final int NOTIFICATION_ID = 1;

    public CloudMessageService() {
        super("CloudMessageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {

            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                //check if approval is needed
                Log.i("received", "Received: " + extras.toString());
                sendNotification(extras);

            }
        }

        CloudMessageReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(Bundle bundle) {

        Boolean quiet = Boolean.valueOf(bundle.getString("quiet"));

        if(!quiet) {
            NotificationManager mNotificationManager = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent loginActivityIntent = new Intent(this, LoginActivity.class);

            Logger.log("Received", bundle.getString("sessionId", ""));

            loginActivityIntent.putExtra("sessionId", bundle.getString("sessionId", null));
            loginActivityIntent.putExtra("browserId",bundle.getString("browserId",null));

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, loginActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.passbird_notification)
                            .setContentTitle(bundle.getString("title"))
                            .setStyle(new NotificationCompat.BigTextStyle().bigText("A new password request requires approval"))
                            .setContentText("A new password request requires approval")
                            .setAutoCancel(true)
                            //.setDefaults(Notification.DEFAULT_VIBRATE);
                            .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);

            builder.setContentIntent(contentIntent);
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
