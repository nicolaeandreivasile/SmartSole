package com.iot.smartsole.control;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iot.smartsole.control.asynchronous.BluetoothGattManager;
import com.iot.smartsole.control.asynchronous.operation.BluetoothGattClose;
import com.iot.smartsole.control.asynchronous.operation.BluetoothGattConnect;
import com.iot.smartsole.control.asynchronous.operation.BluetoothGattDisconnect;
import com.iot.smartsole.control.asynchronous.operation.BluetoothGattDiscover;
import com.iot.smartsole.control.asynchronous.operation.BluetoothGattRead;
import com.iot.smartsole.control.asynchronous.operation.BluetoothGattWrite;
import com.iot.smartsole.control.model.Data;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class BluetoothLeService extends Service {
    private final static String BLE_SERVICE_TAG = BluetoothLeService.class.getSimpleName();

    public final static String GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String GATT_ERROR = "ACTION_GATT_ERROR";
    public final static String GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String GATT_CHARACTERISTIC_DATA_AVAILABLE =
            "ACTION_GATT_CHARACTERISTIC_DATA_AVAILABLE";
    public final static String GATT_CHARACTERISTIC_EXTRA_DATA = "GATT_CHARACTERISTIC_EXTRA_DATA";
    public final static String GATT_CHARACTERISTIC_RESTART_OPERATION =
            "ACTION_GATT_CHARACTERISTIC_RESTART_OPERATION";

    private String bluetoothLeConnectionState = GATT_DISCONNECTED;

    private final Binder binder = new LocalBinder();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt bluetoothGatt = null;
    private BluetoothGattManager bluetoothGattManager = new BluetoothGattManager(this);
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    broadcastUpdate(GATT_CONNECTED, null);
                    bluetoothLeConnectionState = GATT_CONNECTED;
                    bluetoothGatt = gatt;

                    BluetoothGattDiscover bluetoothGattDiscover =
                            new BluetoothGattDiscover(BluetoothLeService.this,
                                    bluetoothGatt, Calendar.getInstance().getTime());
                    bluetoothGattManager.scheduleTask(bluetoothGattDiscover);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    BluetoothGattClose bluetoothGattClose =
                            new BluetoothGattClose(BluetoothLeService.this,
                                    null, Calendar.getInstance().getTime());
                    bluetoothGattManager.scheduleTask(bluetoothGattClose);

                    broadcastUpdate(GATT_DISCONNECTED, null);
                    bluetoothLeConnectionState = GATT_DISCONNECTED;
                }
            } else {
                Log.w(BLE_SERVICE_TAG, "onConnectionStateChange received: status " + status);

                BluetoothGattClose bluetoothGattClose =
                        new BluetoothGattClose(BluetoothLeService.this,
                                null, Calendar.getInstance().getTime());
                bluetoothGattManager.scheduleTask(bluetoothGattClose);

                broadcastUpdate(GATT_ERROR, null);
                bluetoothLeConnectionState = GATT_ERROR;
            }
            bluetoothGattManager.notifyTaskCompleted();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(GATT_SERVICES_DISCOVERED, null);
            } else {
                Log.w(BLE_SERVICE_TAG, "onServicesDiscovered received: status " + status);
            }
            bluetoothGattManager.notifyTaskCompleted();
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(GATT_CHARACTERISTIC_DATA_AVAILABLE, characteristic);
            bluetoothGattManager.notifyTaskCompleted();
        }
    };

    private void broadcastUpdate(final String bluetoothLeAction,
                                 BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        Intent broadcastUpdateIntent = new Intent(bluetoothLeAction);
        if (bluetoothGattCharacteristic != null) {
            final String characteristicName = SensingService
                    .lookup(bluetoothGattCharacteristic.getUuid().toString());
            if (characteristicName == null)
                return;

            final byte[] characteristicDataByteArray = bluetoothGattCharacteristic.getValue();
            if (characteristicDataByteArray != null && characteristicDataByteArray.length > 0) {
                int value = 0;
                int mask = 0xFFFFFFFF;
                for (int index = 0; index < characteristicDataByteArray.length &&
                        Math.pow(2, index) < Integer.SIZE; index++) {
                    int intByte =
                            mask & (characteristicDataByteArray[index] << (index * Byte.SIZE));
                    value ^= intByte;
                }
                String characteristicData = String.valueOf(value);

                broadcastUpdateIntent.putExtra(GATT_CHARACTERISTIC_EXTRA_DATA,
                        characteristicName + "\n" + characteristicData);
            }
        }
        sendBroadcast(broadcastUpdateIntent);
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(BLE_SERVICE_TAG, "Error occurred while initializing Bluetooth manager");
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(BLE_SERVICE_TAG, "Error occurred while initializing Bluetooth adapter");
            return false;
        }

        return true;
    }

    public void connect(final BluetoothDevice bluetoothLeDevice) {
        BluetoothGattConnect bluetoothGattConnect = new BluetoothGattConnect(this,
                bluetoothGattCallback, bluetoothLeDevice, Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattConnect);
    }

    public void disconnect() {
        if (bluetoothGatt == null)
            return;

        BluetoothGattDisconnect bluetoothGattDisconnect = new BluetoothGattDisconnect(this,
                bluetoothGatt, bluetoothGattCallback, Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattDisconnect);

    }

    private void close() {
        if (bluetoothGatt == null)
            return;

        BluetoothGattClose bluetoothGattClose = new BluetoothGattClose(this, bluetoothGatt,
                Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattClose);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public List<BluetoothGattService> getBluetoothLeGattServices() {
        if (bluetoothGatt == null)
            return null;

        return bluetoothGatt.getServices();
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public static class SensingService {
        public static final String SOLE_SENSING_UUID = "c5e15750-62b8-4e9b-acb0-dc94739b6bbd";
        public static final String CONTROL_CHARACTERISTIC = "Control";

        private static HashMap<String, String> attributes = new LinkedHashMap<String, String>();

        static {
            attributes.put("86aa2651-5a92-4265-a676-4d77ec3354e7", Data.INTERIOR_FRONT);
            attributes.put("3e40cd36-6dc6-474b-8cbd-740c7774852a", Data.EXTERIOR_FRONT);
            attributes.put("cca5bd11-3448-4b42-b359-49307e084248", Data.BACK);
        }

        public static String lookup(String characteristicUUID) {
            String characteristicName = attributes.get(characteristicUUID);

            return characteristicName;
        }
    }
}
