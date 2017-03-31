package nz.ac.auckland.nihi.trainer.activities;

import java.util.Set;

import nz.ac.auckland.nihi.trainer.R;

import org.apache.log4j.Logger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceListActivity extends Activity {

	private static final Logger logger = Logger.getLogger(DeviceListActivity.class);

	public static final String EXTRA_DEVICE_ADDRESS = DeviceListActivity.class.getName() + ".device_address";
	public static final String EXTRA_DEVICE_NAME = DeviceListActivity.class.getName() + ".device_name";

	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDeviceArrayAdapter;
	private ArrayAdapter<String> mNewDeviceArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list_screen);

		setResult(Activity.RESULT_CANCELED); // incase user backs out

		Button scanButton = (Button) findViewById(R.id.button_scan);
		scanButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				doDiscovery();
				v.setVisibility(View.GONE);
			}
		});

		// initialise arrayAdapters
		mPairedDeviceArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		mNewDeviceArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

		// find and set up ListView for paired devices
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDeviceArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		// find and set up ListView for newly discovered devices
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDeviceArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery is finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		mBtAdapter = BluetoothAdapter.getDefaultAdapter(); // get local Bluetooth Adapter

		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices(); // get set of currently paired devices
		if (pairedDevices.size() > 0) {
			// findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			// findViewById(R.id.separator_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				mPairedDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
			}
		} else {
			String noDevices = getResources().getText(R.string.btdevices_none_paired).toString();
			mPairedDeviceArrayAdapter.add(noDevices);
		}
	}

	protected void doDiscovery() {

		logger.info("doDiscovery(): Discovering devices.");

		// Indicate scanning in the title
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.btdevices_scanning);

		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
		findViewById(R.id.separator_new_devices).setVisibility(View.VISIBLE);

		if (mBtAdapter.isDiscovering()) { // cancel discovery if its already discovering devices
			mBtAdapter.cancelDiscovery();
		}
		mBtAdapter.startDiscovery();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}

		this.unregisterReceiver(mReceiver);
	}

	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			mBtAdapter.cancelDiscovery();

			// TODO THIS IS HACKY!!!
			// Get the device MAC address, which is the last 17 chars in the View
			String info = ((TextView) v).getText().toString();
			String[] parts = info.split("\n");
			String address = parts[1];
			String name = parts[0];
			// String address = info.substring(info.length() - 17);
			// String name = info.substring(0, info.length() - address.length());

			// Create the result Intent and include the MAC address
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
			intent.putExtra(EXTRA_DEVICE_NAME, name);

			logger.info("mDeviceClickListener.onItemClick(): [name=" + name + ", address=" + address + "]");

			setResult(Activity.RESULT_OK, intent);
			finish();
		}

	};

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					mNewDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.btdevices_select_device);
				if (mNewDeviceArrayAdapter.getCount() == 0) {
					String noDevices = getResources().getText(R.string.btdevices_none_found).toString();
					mNewDeviceArrayAdapter.add(noDevices);
				}
			}

		}
	};
}
