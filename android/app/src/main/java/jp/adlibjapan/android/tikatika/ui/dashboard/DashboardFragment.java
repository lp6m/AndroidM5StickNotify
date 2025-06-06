package jp.adlibjapan.android.tikatika.ui.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import jp.adlibjapan.android.tikatika.MainActivity;
import jp.adlibjapan.android.tikatika.R;
import jp.adlibjapan.android.tikatika.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DashboardFragment extends Fragment {
    private Context mContext;
    private String mDeviceAddress = "";
    private String mDeviceName = "";

    private BluetoothAdapter mBluetoothAdapter;
    private Button open_bluetooth_menu;
    private Button button_connect;
    private Button button_reconnect;
    private Switch led_only_switch;
    private Switch icon_send_switch;
    private TextView device_text;
    private TextView address_text;
    private FragmentDashboardBinding binding;
    private SharedPreferences sharedPref;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        mContext = getContext();
        open_bluetooth_menu = view.findViewById(R.id.open_bluetooth_menu);
        open_bluetooth_menu.setOnClickListener(this::onClick);
        button_connect = view.findViewById(R.id.button_connect);
        button_connect.setOnClickListener(this::onClick);
        button_reconnect = view.findViewById(R.id.button_reconnect);
        button_reconnect.setOnClickListener(this::onClick);
        led_only_switch = view.findViewById(R.id.led_only_switch);
        led_only_switch.setOnCheckedChangeListener(this::onSwitchChanged);
        icon_send_switch = view.findViewById(R.id.icon_send_switch);
        icon_send_switch.setOnCheckedChangeListener(this::onSwitchChanged);

        device_text = view.findViewById(R.id.bluetooth_device_text);
        address_text = view.findViewById(R.id.bluetooth_address_text);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        refreshGUIFromSharedPref();

        mBluetoothAdapter = getBluetoothAdapter();
        return view;
    }

    private void onSwitchChanged(CompoundButton compoundButton, boolean b) {
        SharedPreferences.Editor editor = sharedPref.edit();
        if (compoundButton == led_only_switch) {
            editor.putBoolean(getString(R.string.led_only), led_only_switch.isChecked());
        } else if (compoundButton == icon_send_switch) {
            editor.putBoolean(getString(R.string.icon_send), icon_send_switch.isChecked());
        }
        editor.commit();
    }

    private void updateDeviceText(){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.bluetooth_device), mDeviceName);
        editor.putString(getString(R.string.bluetooth_address), mDeviceAddress);
        editor.commit();
    }
    private void refreshGUIFromSharedPref(){
        String device_str = sharedPref.getString(getString(R.string.bluetooth_device), "");
        String address_str = sharedPref.getString(getString(R.string.bluetooth_address), "");
        device_text.setText(String.format("Bluetooth Device: %s", device_str));
        address_text.setText(String.format("Bluetooth Address: %s", address_str));
        boolean led_only = sharedPref.getBoolean(getString(R.string.led_only), false);
        boolean icon_send = sharedPref.getBoolean(getString(R.string.icon_send), false);
        led_only_switch.setChecked(led_only);
        icon_send_switch.setChecked(icon_send);
    }
    public void onClick(View v) {
        if (v == open_bluetooth_menu){
            //Bluetooth設定を開く
            Intent intentOpenBluetoothSettings = new Intent();
            intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intentOpenBluetoothSettings);
        } else if (v == button_connect){
            //デバイス選択画面を出し、選ばれたら接続
            selectAndConnect();
        } else if (v == button_reconnect){
            //再接続
            MainActivity activity = (MainActivity)getActivity();
            activity.restartService();
        }
    }


    public BluetoothAdapter getBluetoothAdapter() {
        // Bluetoothアダプタの取得
        BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();
    }

    private void selectAndConnect(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        int number = pairedDevices.size();
        List<String> deviceAddressList = new ArrayList<String>();
        List<String> deviceNameList = new ArrayList<String>();
        if (pairedDevices.size() > 1) {
            for (BluetoothDevice device : pairedDevices) {
                String device_name = device.getName();
                String device_address = device.getAddress();
                if (device_name.contains("M5")){
                    deviceAddressList.add(device_address);
                    deviceNameList.add(device_name);
                }
            }
            mDeviceName = deviceNameList.get(0);
            mDeviceAddress = deviceAddressList.get(0);
        } else {
            return;
        }
        String[] deviceAddressArr = new String[deviceAddressList.size()];
        String[] deviceNameArr = new String[deviceNameList.size()];
        deviceAddressList.toArray(deviceAddressArr);
        deviceNameList.toArray(deviceNameArr);
        new AlertDialog.Builder(mContext)
                .setTitle("Select Bluetooth Device")
                .setSingleChoiceItems(deviceNameArr, 0, (dialog, item) -> {
                    mDeviceName = deviceNameArr[item];
                    mDeviceAddress = deviceAddressArr[item];
                })
                .setPositiveButton("Select", (dialog, id) -> {
                    updateDeviceText();
                    ((Activity)mContext).runOnUiThread(() -> {
                        refreshGUIFromSharedPref();
                    });
                    MainActivity activity = (MainActivity)getActivity();
                    activity.restartService();
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}