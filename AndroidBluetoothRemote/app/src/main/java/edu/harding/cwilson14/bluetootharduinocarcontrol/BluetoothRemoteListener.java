package edu.harding.cwilson14.bluetootharduinocarcontrol;

import android.widget.TextView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

/**
 * Created by Curtis Wilson on 9/9/2017.
 */

public class BluetoothRemoteListener implements PropertyChangeListener {
	private WeakReference<MainActivity> mActivity;
	
	public BluetoothRemoteListener(MainActivity activity) {
		mActivity = new WeakReference<>(activity);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
		MainActivity mainActivity = mActivity.get();
		if (mainActivity != null)
		switch (propertyChangeEvent.getPropertyName()) {
			
			case "isConnected":
				((TextView)mainActivity.findViewById(R.id.labelStatusValue)).setText(((boolean) propertyChangeEvent.getNewValue()) ? R.string.statusConnected : R.string.statusDisconnected);
				break;
			
			case "isConnecting":
				((TextView)mainActivity.findViewById(R.id.labelStatusValue)).setText(R.string.statusConnecting);
				break;
			
			case "motorLeft":
				int i = ((int)propertyChangeEvent.getNewValue()*100)/255;
				((TextView)mainActivity.findViewById(R.id.labelLeftMotorValue)).setText(i + "%");
				break;
			
			case "motorRight":
				int v = ((int)propertyChangeEvent.getNewValue()*100)/255;
				((TextView)mainActivity.findViewById(R.id.labelRightMotorValue)).setText(v + "%");
				break;
		}
	}
}
