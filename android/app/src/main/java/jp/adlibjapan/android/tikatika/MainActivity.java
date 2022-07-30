package jp.adlibjapan.android.tikatika;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import jp.adlibjapan.android.tikatika.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity{

    private static final int REQUEST_ENABLEBLUETOOTH = 1; // Bluetooth機能の有効化要求時の識別コード

    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Bottom Navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        //通知アクセスの許可を確認・許可されていない場合はプロンプトを表示
        if (checkNotificationPermission() == false){
            AlertDialog alertDialog = buildNotificationServiceAlertDialog();
            alertDialog.show();
        }
        //Bluetoothアクセスの許可を確認・許可されていない場合はプロンプトを表示
        if (checkBluetoothPermission() == false){
            Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( enableBtIntent, REQUEST_ENABLEBLUETOOTH );
        }
        checkBluetooth();
        //通知サービスの起動
        restartService();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void restartService(){
        Intent intent = new Intent(getApplication(), NotificationReporterService.class);
        if (isMyServiceRunning(NotificationReporterService.class)){
            stopService(intent);
        }
        startService(intent);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
    //https://stackoverflow.com/questions/63834256/how-to-check-if-user-granted-bind-notification-listener-service-permission
    public boolean checkNotificationPermission() {
        Context c = this;
        String pkgName = c.getPackageName();
        final String flat = Settings.Secure.getString(c.getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkBluetoothPermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                        == PackageManager.PERMISSION_GRANTED);
    }


    public boolean checkBluetooth() {
        // Android端末がBLUETOOTHをサポートしてるかの確認
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this, "checkBluetooth() PackageManager.FEATURE_BLUETOOTH is not suported", Toast.LENGTH_LONG).show();
            return false;
        }
        if (((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter() == null){
            Toast.makeText(this, "getBluetoothAdapter() bluetoothManager is suported", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("設定");
        alertDialogBuilder.setMessage("アプリの通知アクセスを許可してください");
        alertDialogBuilder.setPositiveButton("許可する",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton("許可しない",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLEBLUETOOTH) { // Bluetooth有効化要求
            if (Activity.RESULT_CANCELED == resultCode) {    // 有効にされなかった
                Toast.makeText(this, "Bluetooth usage was not allowed.", Toast.LENGTH_LONG).show();
                //button_connect.setEnabled(false);
                //button_clear.setEnabled(false);
                //bluetooth_allowed = false;
                //finish();    // アプリ終了宣言
            } else {

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
