package edu.harding.cwilson14.bluetootharduinocarcontrol;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private BluetoothRemote mBluetoothRemote;
	private TextView mLabelCurrentStatus;
	private TextView mLabelTiltX;
	private TextView mLabelTiltY;
	private TextView mLabelTiltZ;
	private TextView mLabelLeftMotor;
	private TextView mLabelRightMotor;
	private Switch mSwitchUseSensors;
	private SeekBar mSeekBarSpeed;
	private Spinner mSpinnerDirection;
	private Spinner mSpinnerSide;
	private static MainActivity instance;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		instance = this;
		
		// Find widgets.
		mLabelCurrentStatus = (TextView) findViewById(R.id.labelStatusValue);
		mSeekBarSpeed = (SeekBar) findViewById(R.id.seekbarSpeed);
		mSwitchUseSensors = (Switch) findViewById(R.id.switchUseSensors);
		mSpinnerDirection = (Spinner)findViewById(R.id.spinnerDirection);
		mSpinnerSide = (Spinner)findViewById(R.id.spinnerSide);
		mLabelTiltX = (TextView) findViewById(R.id.labelTiltXValue);
		mLabelTiltY = (TextView) findViewById(R.id.labelTiltYValue);
		mLabelTiltZ = (TextView) findViewById(R.id.labelTiltZValue);
		mLabelLeftMotor = (TextView) findViewById(R.id.labelLeftMotorValue);
		mLabelRightMotor = (TextView) findViewById(R.id.labelRightMotorValue);
		
		mLabelLeftMotor.setText("0%");
		mLabelRightMotor.setText("0%");
		
		// Set data for spinners.
		mSpinnerDirection.setAdapter(ArrayAdapter.createFromResource(this, R.array.directions_array, R.layout.support_simple_spinner_dropdown_item));
		mSpinnerSide.setAdapter(ArrayAdapter.createFromResource(this, R.array.sides_array, R.layout.support_simple_spinner_dropdown_item));
		
		// Initialize Bluetooth connection.
		try {
			mBluetoothRemote = new BluetoothRemote(this, new BluetoothRemoteListener(this));
			mBluetoothRemote.connect();
		} catch (ExceptionInInitializerError e) {
			mLabelCurrentStatus.setText(getText(R.string.bluetoothNotSupported));
		}
		
		// Initialize device sensors.
		SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor mSensorAccelerometer = (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		mSensorManager.registerListener(new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				Float azimuth = sensorEvent.values[0]; // -Z
				Float pitch = sensorEvent.values[1]; // X
				Float roll = sensorEvent.values[2]; // Y
				
				// Portrait
				// Accleration = Y [0, 10]
				// Turn = Z [-10, 10]
				
				// Landscape
				// Accleration = Y [0, 10]
				// Turn = X [-10, 10]
				if (mSwitchUseSensors.isChecked()) {
					int left, right;
					left = Math.round(255 * (roll / 10));
					right = left;
					
					if (azimuth != 0) {
						left -= 255 * (azimuth / 10);
						right -= 255 * (-azimuth / 10);
					}
					
					if (mBluetoothRemote != null) {
						mBluetoothRemote.sendMotorCommand(SIDE.LEFT, Math.abs(left), left > 0);
						mBluetoothRemote.sendMotorCommand(SIDE.RIGHT, Math.abs(right), right > 0);
					}
				}
				
				mLabelTiltX.setText(pitch.toString());
				mLabelTiltY.setText(roll.toString());
				mLabelTiltZ.setText(azimuth.toString());
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		}, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    public void sendInstructionsOnClick(View view) {
		// Get the side.
		SIDE side;
		String selectedSide = mSpinnerSide.getSelectedItem().toString();
		if (selectedSide.equals("Left")) side = SIDE.LEFT;
		else if (selectedSide.equals("Right")) side = SIDE.RIGHT;
		else side = SIDE.BOTH;
		
		// Get the direction.
		boolean forwards = mSpinnerDirection.getSelectedItem().equals(("Forwards"));
		
		// Send the command.
		if (mBluetoothRemote != null)
			mBluetoothRemote.sendMotorCommand(side, mSeekBarSpeed.getProgress(), forwards);
		
		// Turn off the use of sensors.
		mSwitchUseSensors.setChecked(false);
	}
	
	public void sendStop(View view) {
		if (mBluetoothRemote != null)
			mBluetoothRemote.sendMotorCommand(SIDE.BOTH, 0, true);
		mSwitchUseSensors.setChecked(false);
	}
	
	public void connectOnClick(View view) {
		if (mBluetoothRemote != null)
			mBluetoothRemote.connect();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
		if (mBluetoothRemote != null)
			mBluetoothRemote.close();
	}
	
	public static MainActivity getCurrentInstance() {
		return instance;
	}
}
