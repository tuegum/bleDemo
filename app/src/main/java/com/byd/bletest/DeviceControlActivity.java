/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.byd.bletest;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.byd.bletest.uart.BleProfileService;
import com.byd.bletest.uart.BleProfileServiceReadyActivity;
import com.byd.bletest.uart.UARTInterface;
import com.byd.bletest.uart.UARTService;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends BleProfileServiceReadyActivity<UARTService.UARTBinder> implements UARTInterface, TextWatcher,View.OnClickListener {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();


    @BindView(R.id.editText)
    EditText editText;
    @BindView(R.id.commit)
    Button commit;
    @BindView(R.id.red)
    Button red;
    @BindView(R.id.green)
    Button green;
    @BindView(R.id.blue)
    Button blue;
    @BindView(R.id.off)
    Button off;
    @BindView(R.id.imageView)
    ImageView imageView;

    TextView mMotion;

    private TextView mConnectionState;
    private TextView mDataField;
    private UARTService.UARTBinder mServiceBinder;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (UARTService.BROADCAST_UART_RX.equals(action)) {
                String data = intent.getStringExtra(UARTService.EXTRA_DATA);
                displayData(data);
            } else if (UARTService.BROADCAST_UART_TX.equals(action)) {
                String data1 = intent.getStringExtra(UARTService.EXTRA_DATA);
                Log.d(TAG, "txddata=" + data1);
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onServiceBinded(UARTService.UARTBinder binder) {
        mServiceBinder = binder;
    }

    @Override
    protected void onServiceUnbinded() {
        mServiceBinder = null;
    }

    @Override
    protected Class<? extends BleProfileService> getServiceClass() {
        return UARTService.class;
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {
        setContentView(R.layout.gatt_services_characteristics);
        ButterKnife.bind(this);
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);

        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        editText = (EditText) findViewById(R.id.editText);
        red=(Button)findViewById(R.id.red);
        red.setOnClickListener(this);
        blue=(Button)findViewById(R.id.blue);
        blue.setOnClickListener(this);
        green=(Button)findViewById(R.id.green);
        green.setOnClickListener(this);
        off=(Button)findViewById(R.id.off);
        off.setOnClickListener(this);
        commit=(Button)findViewById(R.id.commit);
        commit.setOnClickListener(this);
        mMotion=(TextView)findViewById(R.id.motion_view);
        imageView=(ImageView)findViewById(R.id.imageView);
        editText.addTextChangedListener(this);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Rejecting Empty String
                    if (v.length() != 0 && !(v.getText().toString().trim().isEmpty())) {

                    }
                    return true;    // action handled
                } else {
                    return false;   // not handled
                }
            }

        });

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onConnectClicked(View view) {
        super.onConnectClicked(view);
    }

    @Override
    public void onDeviceConnected() {
        super.onDeviceConnected();
        mConnected = true;
        updateConnectionState(R.string.connected);
        invalidateOptionsMenu();

    }

    @Override
    public void onDeviceDisconnecting() {
        super.onDeviceDisconnecting();
    }

    @Override
    public void onDeviceDisconnected() {
        super.onDeviceDisconnected();
        mConnected = false;
        updateConnectionState(R.string.disconnected);
        invalidateOptionsMenu();
        clearUI();
    }

    @Override
    public void onServicesDiscovered(boolean optionalServicesFound) {

    }

    @Override
    protected boolean isDeviceConnected() {
        return super.isDeviceConnected();
    }

    @Override
    protected String getDeviceName() {
        return super.getDeviceName();
    }

    @Override
    protected void setDefaultUI() {

    }

    @Override
    protected int getDefaultDeviceName() {
        return 0;
    }

    @Override
    protected int getAboutTextId() {
        return 0;
    }

    @Override
    protected UUID getFilterUUID() {
        return null;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
            switch (data) {
                case Command.BLUE_RESPON:
                    imageView.setBackgroundColor(Color.BLUE);
                    break;
                case Command.RED_RESPON:
                    imageView.setBackgroundColor(Color.RED);
                    break;
                case Command.GREEN_RESPON:
                    imageView.setBackgroundColor(Color.GREEN);
                    break;
                case Command.OFF_RESPON:
                    imageView.setBackgroundColor(Color.WHITE);
                    break;
                case Command.MOTION_SINGLE:
                case Command.MOTION_DOUBLE:
                    mMotion.setText(data);
                    break;
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UARTService.BROADCAST_UART_TX);
        intentFilter.addAction(UARTService.BROADCAST_UART_RX);
        return intentFilter;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @OnClick({R.id.commit, R.id.red, R.id.green, R.id.blue, R.id.off})
    public void onViewClicked(View view) {
        switch (view.getId()){

        }
    }

    @Override
    public void send(String text) {

    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onViewClicked");
        switch (v.getId()) {
            case R.id.commit:
                String command = editText.getText().toString();
                mServiceBinder.send(command);
                break;
            case R.id.red:
                mServiceBinder.send(Command.RED);
                break;
            case R.id.green:
                mServiceBinder.send(Command.GREEN);
                break;
            case R.id.blue:
                mServiceBinder.send(Command.BLUE);
                break;
            case R.id.off:
                mServiceBinder.send(Command.OFF);
                break;
        }
    }
}
