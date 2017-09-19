package edu.harding.cwilson14.bluetootharduinocarcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.annotation.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Curtis Wilson on 9/9/2017.
 */
enum SIDE {
	LEFT,
	RIGHT,
	BOTH
}

public class BluetoothRemote {
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothSocket mBluetoothSocket;
	private OutputStream mOutputStream;
	private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private ArrayList<PropertyChangeListener> mPropertyChangeListener = new ArrayList<>();
	private int leftMotor = 0;
	private int rightMotor = 0;
	
	public BluetoothRemote(final Activity activity) throws ExceptionInInitializerError {
		mBluetoothAdapter = getBluetooth(activity);
	}
	
	public BluetoothRemote(final Activity activity, PropertyChangeListener propertyChangeListener) throws ExceptionInInitializerError {
		registerListener(propertyChangeListener);
		mBluetoothAdapter = getBluetooth(activity);
	}
	
	private BluetoothAdapter getBluetooth(final Activity activity) throws ExceptionInInitializerError {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			throw new ExceptionInInitializerError("Bluetooth not supported on device.");
		}
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBtIntent, 1);
		}
		return mBluetoothAdapter;
	}
	
	public void connect() {
		notifyListeners("isConnecting", false, true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (mBluetoothDevice == null) {
					mBluetoothDevice = findArduino();
					if (mBluetoothDevice == null)
						notifyListeners("isConnected", true, false);
				}
				
				try {
					mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
					mBluetoothSocket.connect();
					mOutputStream = mBluetoothSocket.getOutputStream();
					
					MainActivity mainActivity = MainActivity.getCurrentInstance();
					if (mainActivity != null) {
						mainActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								notifyListeners("isConnected", false, true);
							}
						});
					}
				} catch (IOException e) {
					MainActivity mainActivity = MainActivity.getCurrentInstance();
					if (mainActivity != null) {
						mainActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								notifyListeners("isConnected", true, false);
							}
						});
					}
				}
			}
		}).start();
	}
	
	public void close() {
		try {
			if (mOutputStream != null)
				mOutputStream.close();
			
			if (mBluetoothSocket != null)
				mBluetoothSocket.close();
			
			notifyListeners("isConnected", true, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Nullable
	private BluetoothDevice findArduino() {
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices())
			if (device.getName().equals("HC-05"))
				return device;
		return null;
	}
	
	public boolean sendMotorCommand(SIDE side, int speed, boolean forwards) {
		
		// Adjust for negative speeds.
		if (speed < 0) {
			forwards = !forwards;
			speed = Math.abs(speed);
		}
		
		// Clamp the speed and convert it to a byte.
		Integer tempSpeed = Math.max(Math.min(speed, 255), 0);
		
		// Set the display speed.
		int displaySpeed = tempSpeed;
		if (!forwards) displaySpeed = displaySpeed * -1;
		
		// Compensate for the smaller range by flipping it with a negative (-128 - 127).
		if (tempSpeed > 127) tempSpeed -= 256;
		// Convert it to a byte.
		byte bSpeed = Byte.parseByte(tempSpeed.toString());
		
		
		// Set the side.
		if (side == SIDE.LEFT) {
			notifyListeners("motorLeft", leftMotor, leftMotor = displaySpeed);
		} else if (side == SIDE.RIGHT) {
			notifyListeners("motorRight", rightMotor, rightMotor = displaySpeed);
		} else {
			// For both sides, split the command into left and right.
			sendMotorCommand(SIDE.LEFT, speed, forwards);
			return sendMotorCommand(SIDE.RIGHT, speed, forwards);
		}
		
		// Attempt to send the data.
		try {
			if (mOutputStream == null) {
				notifyListeners("isConnected", true, false);
				return false;
			}
			
			// Build the data packet.
			// Format: SSSSSSDM
			// S = Speed
			// D = Direction (forwards = 1, backwards = 0)
			// M = Motor (left = 0, right = 1)
			byte data = (byte)(bSpeed & 252);
			if (side == SIDE.RIGHT) data += 1;
			if (forwards) data += 2;
			
			// Send the data.
			mOutputStream.write(data);
			return true;
		} catch (IOException e) {
			notifyListeners("isConnected", true, false);
			return false;
		}
	}
	
	
	// Observer Design Pattern from http://www.vogella.com/tutorials/DesignPatternObserver/article.html.
	public void registerListener(PropertyChangeListener propertyChangeListener) {
		mPropertyChangeListener.add(propertyChangeListener);
	}
	private void notifyListeners(String property, Object oldValue, Object newValue) {
		for (PropertyChangeListener listener : mPropertyChangeListener) {
			listener.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
		}
	}
}
