package com.example.notificationreporter;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import com.example.notificationreporter.ui.home.HomeFragment;

public class NotificationReporterService extends NotificationListenerService {
    BluetoothLEWork bleWork;
    NotificationDbHelper helper;
    SQLiteDatabase db;
    final static int ICON_SIZE = 16;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        try {
            Context context = getApplicationContext();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String device_str = sharedPref.getString(getString(R.string.bluetooth_device), "");
            String address_str = sharedPref.getString(getString(R.string.bluetooth_address), "");
            bleWork =  new BluetoothLEWork(context, device_str, address_str) {
                @Override
                public void onBLEConectionFailed(String str) {
                    Toast.makeText(context, "Failed to connect to the device. " + str, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onBLEConnected(String device) {
                    Toast.makeText(context, "Device: " + device + " Connected.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onBLEServiseStarted() {};

                @Override
                public void onBLEDisconnected() {
                }

                @Override
                public void onMessageReceived(String text) {
                }

                @Override
                public void onMessageWritten() {
                }
            };
            bleWork.connect();
        }catch(Exception ex){
            Log.e(HomeFragment.TAG, ex.getMessage());
        }
        return START_REDELIVER_INTENT;
    }
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(HomeFragment.TAG, "NotificationMonitorService onCreate");
        helper = new NotificationDbHelper(this);
        db = helper.getReadableDatabase();

    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(HomeFragment.TAG,"onNotificationPosted");
        processNotification(sbn, true);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(HomeFragment.TAG,"onNotificationRemoved");
        processNotification(sbn, false);
    }

    private void setIconImage(Notification notification, byte[] bmp_bin){
        Bundle extras = notification.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        CharSequence infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);

        Icon smallIcon = notification.getLargeIcon();
        Drawable icon = smallIcon.loadDrawable(this);
        Bitmap bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, ICON_SIZE, ICON_SIZE);
        icon.draw(canvas);


        for( int y = 0 ; y < ICON_SIZE ; y++ ){
            for( int x = 0 ; x < ICON_SIZE ; x++ ){
                int color = bitmap.getPixel(x, y);
                int r = (color >> 16) & 255;
                int g = (color >> 8) & 255;
                int b = color & 255;
                r = r >> 3;
                g = g >> 2;
                b = b >> 3;
                int color565 = ((r << 11) | (g << 5) | b);
                bmp_bin[(y * ICON_SIZE + x) * 2 + 1] = (byte)(color565 >> 8);
                bmp_bin[(y * ICON_SIZE + x) * 2 ] = (byte)(color565 & 256);
            }
        }
        bitmap.recycle();
    }

    private void processNotification( StatusBarNotification sbn, boolean posted ){
        int id = sbn.getId();
        String packageName = sbn.getPackageName();
        String groupKey = sbn.getGroupKey();
        String key = sbn.getKey();
        String tag = sbn.getTag();
        long time = sbn.getPostTime();

        Log.d(HomeFragment.TAG,"id:" + id + " packageName:" + packageName + " posted:" + posted + " time:" +time);
        Log.d(HomeFragment.TAG,"groupKey:" + groupKey + " key:" + key + " tag:" + tag);

        try {
            ApplicationInfo app = HomeFragment.packageManager.getApplicationInfo(packageName, 0);
            String label = app.loadLabel(HomeFragment.packageManager).toString();

            Notification notification = sbn.getNotification();
            CharSequence tickerText = notification.tickerText;

            Context context = getApplicationContext();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            boolean led_only = sharedPref.getBoolean(getString(R.string.led_only), false);
            boolean icon_send = sharedPref.getBoolean(getString(R.string.icon_send), false);

            if( helper.hasPackageName(db, packageName)) {
                if (posted) {
                    if (tickerText == null) return;
                    String text_to_send = led_only ? "LED_ONLY_COMMAND" : label + ":" + tickerText.toString();
                    bleWork.sendText(text_to_send);
                    if (led_only == false && icon_send){
                        byte[] bmp_bin = new byte[ICON_SIZE * ICON_SIZE * 2];
                        setIconImage(notification, bmp_bin);
                        Thread.sleep(300);//通信負荷低減のため
                        bleWork.sendImage(bmp_bin);
                    }
                } else {
                    bleWork.sendText("CLEAR_COMMAND");
                }

            }
        }catch(Exception ex){
            Log.e(HomeFragment.TAG, ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        Log.d(HomeFragment.TAG, "onDestroy");

        try {
            bleWork.disconnect();
        }catch(Exception ex){
            Log.e(HomeFragment.TAG, ex.getMessage());
        }
    }

}
