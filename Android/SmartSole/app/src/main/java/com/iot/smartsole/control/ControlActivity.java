package com.iot.smartsole.control;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.smartsole.R;
import com.iot.smartsole.control.model.Data;
import com.iot.smartsole.control.model.Measurement;
import com.iot.smartsole.control.ui.data.DataViewModel;
import com.iot.smartsole.control.ui.device.DeviceViewModel;
import com.iot.smartsole.control.ui.home.HomeViewModel;
import com.iot.smartsole.control.ui.statistics.StatisticsViewModel;
import com.iot.smartsole.databinding.ActivityControlBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class ControlActivity extends AppCompatActivity {
    private final static String CONTROL_TAG = ControlActivity.class.getSimpleName();

    public final static String DEVICE = "DEVICE";
    public final static String FRAGMENT_DEVICE = "FRAGMENT_DEVICE";
    public final static String CHARACTERISTIC = "CHARACTERISTIC";

    private BluetoothLeService bluetoothLeService;
    private BluetoothDevice bluetoothLeDevice;

    HomeViewModel bluetoothLeHomeViewModel;
    DeviceViewModel bluetoothLeDeviceViewModel;
    DataViewModel bluetoothLeDataViewModel;
    StatisticsViewModel bluetoothLeStatisticsViewModel;

    private BluetoothGattService bluetoothLeGattSensingService;
    private LinkedHashMap<BluetoothGattService,
            LinkedList<BluetoothGattCharacteristic>> bluetoothLeDeviceData =
            new LinkedHashMap<BluetoothGattService, LinkedList<BluetoothGattCharacteristic>>();

    private boolean bluetoothLeDeviceConnected = false;
    private final ServiceConnection bluetoothLeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothLeService != null) {
                if (!bluetoothLeService.initialize()) {
                    Log.e(CONTROL_TAG, "Error occurred while initializing " +
                            "Bluetooth LE service");
                    finish();
                }
                bluetoothLeService.connect(bluetoothLeDevice);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothLeService = null;
        }
    };

    private final BroadcastReceiver bluetoothLeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.GATT_CONNECTED.equals(action)) {
                bluetoothLeDeviceConnected = true;
                Toast.makeText(context, bluetoothLeDevice.getName() +
                        " connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.GATT_DISCONNECTED.equals(action)) {
                bluetoothLeDeviceConnected = false;
                Toast.makeText(context, bluetoothLeDevice.getName() +
                        " disconnected", Toast.LENGTH_SHORT).show();
                finish();
            } else if (BluetoothLeService.GATT_ERROR.equals(action)) {
                bluetoothLeDeviceConnected = false;
                Toast.makeText(context, bluetoothLeDevice.getName() +
                        " has disconnected unexpectedly", Toast.LENGTH_SHORT).show();
                finish();
            } else if (BluetoothLeService.GATT_SERVICES_DISCOVERED.equals(action)) {
                collectBluetoothLeDiscoveredServices(
                        bluetoothLeService.getBluetoothLeGattServices());
            } else if (BluetoothLeService.GATT_CHARACTERISTIC_DATA_AVAILABLE.equals(action)) {
                String dataString =
                        intent.getStringExtra(BluetoothLeService.GATT_CHARACTERISTIC_EXTRA_DATA);
                String[] dataStringTokens = dataString.split("\n");
                String dataName = dataStringTokens[Data.NAME_IDX];
                String dataValue = dataStringTokens[Data.VALUE_IDX];
                Data data = new Data(dataName, dataValue);
                if (measurement != null) {
                    measurement.populateField(data);
                    if (measurement.isPopulated()) {
                        measurement.setMacAddress(bluetoothLeDevice.getAddress());
                        measurement.setCreatedAt(Calendar.getInstance().getTime());
                    }
                }

                Intent dataIntent = new Intent();
                dataIntent.putExtra(CHARACTERISTIC, data);
                bluetoothLeDataViewModel.selectIntent(dataIntent);
            }
        }
    };

    private Measurement measurement = null;
    private List<Measurement> measurementList = new ArrayList<>();
    private LocationManager locationManager = null;
    private ConnectivityManager connectivityManager = null;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityControlBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_device, R.id.nav_data, R.id.nav_statistics)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        Intent intent = getIntent();
        bluetoothLeDevice = (BluetoothDevice) intent.getParcelableExtra(DEVICE);

        bluetoothLeHomeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        bluetoothLeDeviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        bluetoothLeDataViewModel = new ViewModelProvider(this).get(DataViewModel.class);
        bluetoothLeStatisticsViewModel =
                new ViewModelProvider(this).get(StatisticsViewModel.class);

        Intent bluetoothLeServiceIntent = new Intent(ControlActivity.this,
                BluetoothLeService.class);
        bindService(bluetoothLeServiceIntent, bluetoothLeServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(bluetoothLeUpdateReceiver, buildBluetoothLeIntentFilter());
        if (bluetoothLeService != null)
            bluetoothLeService.connect(bluetoothLeDevice);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothLeUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(bluetoothLeServiceConnection);
        bluetoothLeDevice = null;
        bluetoothLeService = null;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { return true; }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /* Intent filter for broadcast receiver */
    @NotNull
    private static IntentFilter buildBluetoothLeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.GATT_ERROR);
        intentFilter.addAction(BluetoothLeService.GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.GATT_CHARACTERISTIC_DATA_AVAILABLE);

        return intentFilter;
    }

    /* Collect discovered services */
    private void collectBluetoothLeDiscoveredServices(
            List<BluetoothGattService> bluetoothGattServices) {
        if (bluetoothGattServices == null)
            return;

        for (BluetoothGattService bluetoothGattService : bluetoothGattServices) {
            if (bluetoothGattService.getUuid().toString().equals(
                    BluetoothLeService.SensingService.SOLE_SENSING_UUID)) {
                bluetoothLeGattSensingService = bluetoothGattService;
            } else {
                continue;
            }

            LinkedList<BluetoothGattCharacteristic> bluetoothGattCharacteristicList =
                    new LinkedList<BluetoothGattCharacteristic>();
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                    bluetoothGattService.getCharacteristics()) {
                bluetoothLeService.getBluetoothGatt().setCharacteristicNotification(
                        bluetoothGattCharacteristic, true);
                bluetoothGattCharacteristicList.add(bluetoothGattCharacteristic);
            }
            bluetoothLeDeviceData.put(bluetoothGattService, bluetoothGattCharacteristicList);
        }
    }

    /* Request device information */
    public void requestDevice() {
        Intent deviceIntent = new Intent();
        deviceIntent.putExtra(FRAGMENT_DEVICE, bluetoothLeDevice);
        bluetoothLeDeviceViewModel.selectItem(deviceIntent);
    }

    /* Request disconnect from the connected device */
    public void requestDisconnect() {
        bluetoothLeService.disconnect();
    }
}