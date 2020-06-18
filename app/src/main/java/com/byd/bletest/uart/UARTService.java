/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.byd.bletest.uart;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.byd.bletest.BleManager;
import com.byd.bletest.utility.Constants;

public class UARTService extends BleProfileService implements UARTManagerCallbacks {
	private static final String TAG = "UARTService";

	public static final String BROADCAST_UART_TX = "no.nordicsemi.android.nrftoolbox.uart.BROADCAST_UART_TX";
	public static final String BROADCAST_UART_RX = "no.nordicsemi.android.nrftoolbox.uart.BROADCAST_UART_RX";
	public static final String EXTRA_DATA = "no.nordicsemi.android.nrftoolbox.uart.EXTRA_DATA";

	/** A broadcast message with this action and the message in {@link Intent#EXTRA_TEXT} will be sent t the UART device. */
	public final static String ACTION_SEND = "no.nordicsemi.android.nrftoolbox.uart.ACTION_SEND";
	/** A broadcast message with this action is triggered when a message is received from the UART device. */
	private final static String ACTION_RECEIVE = "no.nordicsemi.android.nrftoolbox.uart.ACTION_RECEIVE";
	/** Action send when user press the DISCONNECT button on the notification. */
	public final static String ACTION_DISCONNECT = "no.nordicsemi.android.nrftoolbox.uart.ACTION_DISCONNECT";
	/** A source of an action. */
	public final static String EXTRA_SOURCE = "no.nordicsemi.android.nrftoolbox.uart.EXTRA_SOURCE";
	public final static int SOURCE_NOTIFICATION = 0;
	public final static int SOURCE_WEARABLE = 1;
	public final static int SOURCE_3RD_PARTY = 2;

	private final static int NOTIFICATION_ID = 349; // random
	private final static int OPEN_ACTIVITY_REQ = 67; // random
	private final static int DISCONNECT_REQ = 97; // random

	private UARTManager mManager;

	private final LocalBinder mBinder = new UARTBinder();

	public class UARTBinder extends LocalBinder implements UARTInterface {
		@Override
		public void send(final String text) {
			mManager.send(text);
		}

	}

	@Override
	protected LocalBinder getBinder() {
		return mBinder;
	}

	@Override
	protected BleManager<UARTManagerCallbacks> initializeManager() {
		return mManager = new UARTManager(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		registerReceiver(mDisconnectActionBroadcastReceiver, new IntentFilter(ACTION_DISCONNECT));
		registerReceiver(mIntentBroadcastReceiver, new IntentFilter(ACTION_SEND));
	}

	@Override
	public void onDestroy() {
		// when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
		cancelNotification();
		unregisterReceiver(mDisconnectActionBroadcastReceiver);
		unregisterReceiver(mIntentBroadcastReceiver);

		super.onDestroy();
	}

	@Override
	protected void onRebind() {
		// when the activity rebinds to the service, remove the notification
		cancelNotification();
	}

	@Override
	protected void onUnbind() {

	}

	@Override
	protected void onServiceStarted() {
		//
	}

	@Override
	public void onDeviceConnected() {
		super.onDeviceConnected();
		sendMessageToWearables(Constants.UART.DEVICE_CONNECTED, notNull(getDeviceName()));
	}

	@Override
	protected boolean stopWhenDisconnected() {
		return false;
	}

	@Override
	public void onDeviceDisconnected() {
		super.onDeviceDisconnected();
		sendMessageToWearables(Constants.UART.DEVICE_DISCONNECTED, notNull(getDeviceName()));
	}

	@Override
	public void onLinklossOccur() {
		super.onLinklossOccur();
		sendMessageToWearables(Constants.UART.DEVICE_LINKLOSS, notNull(getDeviceName()));
	}

	private String notNull(final String name) {
		if (!TextUtils.isEmpty(name))
			return name;
		return "empty";
	}

	@Override
	public void onDataReceived(final String data) {
		Log.d(TAG, "\"" + data + "\" received");

		final Intent broadcast = new Intent(BROADCAST_UART_RX);
		broadcast.putExtra(EXTRA_DATA, data);
		this.sendBroadcast(broadcast);

		// send the data received to other apps, e.g. the Tasker
		final Intent globalBroadcast = new Intent(ACTION_RECEIVE);
		globalBroadcast.putExtra(Intent.EXTRA_TEXT, data);
		sendBroadcast(globalBroadcast);
	}

	@Override
	public void onDataSent(final String data) {
        Log.d(TAG,  "\"" + data + "\" sent");

		final Intent broadcast = new Intent(BROADCAST_UART_TX);
		broadcast.putExtra(EXTRA_DATA, data);
		this.sendBroadcast(broadcast);
	}

	/**
	 * Sends the given message to all connected wearables. If the path is equal to {@link Constants.UART#DEVICE_DISCONNECTED} the service will be stopped afterwards.
	 * @param path message path
	 * @param message the message
	 */
	private void sendMessageToWearables(final @NonNull String path, final @NonNull String message) {

	}

	/**
	 * Creates the notification
	 * 
	 * @param messageResId
	 *            message resource id. The message must have one String parameter,<br />
	 *            f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>
	 * @param defaults
	 *            signals that will be used to notify the user
	 */
	private void createNotification(final int messageResId, final int defaults) {

	}

	/**
	 * Cancels the existing notification. If there is no active notification this method does nothing
	 */
	private void cancelNotification() {
		final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}

	/**
	 * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
	 */
	private final BroadcastReceiver mDisconnectActionBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_NOTIFICATION);
			switch (source) {
				case SOURCE_NOTIFICATION:
                    Log.d(TAG, "[Notification] Disconnect action pressed");
					break;
				case SOURCE_WEARABLE:
                    Log.d(TAG,  "[WEAR] '" + Constants.ACTION_DISCONNECT + "' message received");
					break;
			}
			if (isConnected())
				getBinder().disconnect();
			else
				stopSelf();
		}
	};

	/**
	 * Broadcast receiver that listens for {@link #ACTION_SEND} from other apps. Sends the String or int content of the {@link Intent#EXTRA_TEXT} extra to the remote device.
	 * The integer content will be sent as String (65 -> "65", not 65 -> "A").
	 */
	private BroadcastReceiver mIntentBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final boolean hasMessage = intent.hasExtra(Intent.EXTRA_TEXT);
			if (hasMessage) {
				String message = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (message == null) {
					final int intValue = intent.getIntExtra(Intent.EXTRA_TEXT, Integer.MIN_VALUE); // how big is the chance of such data?
					if (intValue != Integer.MIN_VALUE)
						message = String.valueOf(intValue);
				}

				if (message != null) {
					final int source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_3RD_PARTY);
					switch (source) {
						case SOURCE_WEARABLE:
                            Log.d(TAG, "[WEAR] '" + Constants.UART.COMMAND + "' message received with data: \"" + message + "\"");
							break;
						case SOURCE_3RD_PARTY:
						default:
                            Log.d(TAG,  "[Broadcast] " + ACTION_SEND + " broadcast received with data: \"" + message + "\"");
							break;
					}
					mManager.send(message);
					return;
				}
			}
			// No data od incompatible type of EXTRA_TEXT
			if (!hasMessage)
                Log.d(TAG, "[Broadcast] " + ACTION_SEND + " broadcast received no data.");
			else
                Log.d(TAG, "[Broadcast] " + ACTION_SEND + " broadcast received incompatible data type. Only String and int are supported.");
		}
	};
}
