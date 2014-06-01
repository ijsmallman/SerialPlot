/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * 
 * Modified by Joe Smallman April 2012 to make appropriate for serial
 * communication with Arduino device
 *  
 */

package org.SerialPlot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import org.SerialPlot.R;
import com.android.*;
import com.androidplot.series.XYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class SerialPlotActivity extends Activity {
    
	// Debugging
    private static final String TAG = "ArduinoBTComActivity";
    private static final boolean D = true;
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT_AND_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_FILE = 5;
	
    private BTcom btCom;
    private BluetoothAdapter mBluetoothAdapter;
    
    private DataBuffer datBuf;
 	
    // Layout Views
    private XYPlot mySimpleXYPlot;
    //private ListView mConversationView;
    private EditText mOutEditText;
    private ActionBar mActionBar;
    
    
    // Plot variables
    private XYPlot mDataPlot;
    private int mCount = 0;
    private int mPlotMemory = 10; // Default size
    private ArrayList<Number> mPlotBuffer = new ArrayList<Number>();
    private SimpleXYSeries mDataSeries = new SimpleXYSeries("Log");
    
    private Random r = new Random();
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        // view stuff
        setContentView(R.layout.main);
        findViewById(R.id.button_send).setOnClickListener(clickListener);
        mOutEditText=(EditText)findViewById(R.id.edittext_out);
        mOutEditText.setOnClickListener(clickListener);
        mOutEditText.setOnEditorActionListener(new DoneOnEditorActionListener());
        
        mActionBar = getActionBar();
        
        mDataPlot = (XYPlot)findViewById(R.id.plot_log);
        mDataPlot.addSeries(mDataSeries, LineAndPointRenderer.class, new LineAndPointFormatter(Color.BLACK, null, null));
        mDataPlot.setDomainLabel("Sample");
        mDataPlot.getDomainLabelWidget().pack();
        mDataPlot.setDomainValueFormat(new DecimalFormat("#"));
        mDataPlot.setRangeLabel("Value");
        mDataPlot.getRangeLabelWidget().pack();
        mDataPlot.disableAllMarkup();

        // get Bluetooth Adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		datBuf = new DataBuffer(this, mHandler);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (btCom != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btCom.getState() == BTcom.STATE_NONE) {
              // Start the Bluetooth com services
              btCom.start();
            }
        }
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (btCom != null) btCom.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
	public void setupCom(){
		btCom = new BTcom(this, mHandler);
		return;
	}
	
	private final void setStatus(int resId) {
        mActionBar.setSubtitle(resId);
    }
	
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            
            case BTcom.MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BTcom.STATE_CONNECTED:
                    setStatus(R.string.connected);
                    break;
                case BTcom.STATE_CONNECTING:
                    setStatus(R.string.connecting);
                    break;
                case BTcom.STATE_LISTEN:
                case BTcom.STATE_NONE:
                    setStatus(R.string.not_connected);
                    break;
                }
                break;
                
            case BTcom.MESSAGE_WRITE:
                break;
            
            case BTcom.MESSAGE_READ:
                datBuf.add((String) msg.obj, true);
                break;
            
            case BTcom.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(BTcom.DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            
            case BTcom.MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(BTcom.TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            
            case DataBuffer.MESSAGE_NEW_VALUE:
            	Double d = (Double)msg.obj;
            	UpdatePlot(d);
            	break;
            	
            }
        }
    };
    
    private void UpdatePlot(Double val) {

    	mPlotBuffer.add(mCount);
    	mPlotBuffer.add(val);
        if (mPlotBuffer.size() > 2 * mPlotMemory) 
        {
        	mPlotBuffer.remove(0);
        	mPlotBuffer.remove(0);
        }
    	
        mDataSeries.setModel(mPlotBuffer, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
		mDataPlot.redraw();
		mCount++;
	}
    
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
		case REQUEST_ENABLE_BT_AND_CONNECT_DEVICE:
			if (resultCode == RESULT_OK) {
				Toast.makeText(this, R.string.bt_enabled, Toast.LENGTH_SHORT).show();
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
	    		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				break;
			}
			else {
				// User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
                break;
			}
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == RESULT_OK){
				if (btCom == null) setupCom();
				connectDevice(data, true);
			}
			break;		
		case REQUEST_FILE:
			if (resultCode == RESULT_OK){
				String path = data.getExtras().getString(FileBrowserActivity.EXTRA_FILE_PATH);
				//Toast.makeText(this, "File Clicked: " + path, Toast.LENGTH_SHORT).show();
				plotSavedData(path);
			}
			break;
		}
		return;
	}
	
	private void plotSavedData(String path){

		try {
			File f = new File(path);
			BufferedReader reader = new BufferedReader(new FileReader(f));
			
			String currentLine;
			
			datBuf.clear();
			while ((currentLine = reader.readLine()) != null) {
				datBuf.add(currentLine, false);
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		mDataSeries.setModel(datBuf.getBuffer(), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
		mDataPlot.redraw();
		
	}
	
	private void connectDevice(Intent data, boolean secure) {
		// MAC address
		String address = data.getExtras()
			.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
        btCom.connect(device, secure);
        return;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.scan_connect:
	    		if (btCom != null){
	    			btCom.stop();
	    		}
	    		datBuf.clear();
	    		mPlotBuffer.clear();
	    		// If BT is not on, request that it be enabled.
	            // A request to connect to device will then be called during onActivityResult
	    		if (!mBluetoothAdapter.isEnabled()) {
	                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	                startActivityForResult(enableIntent, REQUEST_ENABLE_BT_AND_CONNECT_DEVICE);
	            } else {
	                //Otherwise just request device connection straight off.
	                //Launch the DeviceListActivity to see the paired devices,
		    		//scan for devices and connect to chosen device
		    		serverIntent = new Intent(this, DeviceListActivity.class);
		    		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	            }
	    		return true;
	    	case R.id.disconnect:
	    		if (btCom != null){
	    			btCom.stop();
	    		}
	    		return true;
	    	case R.id.data_save:
	    		saveToFile(datBuf.getBuffer());
	    		return true;
	    	case R.id.data_clear:
	    		datBuf.clear();
	    		return true;
	    	case R.id.plot_load:
	    		//Launch the FileBrowserActivity
	    		serverIntent = new Intent(this, FileBrowserActivity.class);
	    		startActivityForResult(serverIntent, REQUEST_FILE);
	    		return true;
	    	case R.id.plot_settings:
	    		//TODO
	    		return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void saveToFile(ArrayList<Number> data){
		try {
			//date format for filename
			SimpleDateFormat formatter = new SimpleDateFormat("ddMMMyy");
			//get date
			Date now = new Date();
			//get/make file directory
			File dir = new File("/sdcard/SerialLog/");
			dir.mkdirs();
			//check to see if file exists
			//if doesnt exist create file, 
			//if does exist increment filename couter
			//filename in format ddMMMyyii.txt
			File file = new File("default.txt");
			for (int i = 0; i < 100; i++){
				String count = String.format("%02d", i);
				String filename = formatter.format(now) + count + ".txt";
				file = new File(dir, filename);
				if (!file.exists()) break;
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for (Number n : data) {
				writer.write(n.toString()+"\r\n");
			}
			writer.flush();
			writer.close();
			Toast.makeText(this, R.string.file_saved, Toast.LENGTH_SHORT).show();
		}catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.file_save_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void sendMessage(){
		// Check that we're actually connected before trying anything
        if (btCom.getState() != BTcom.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
		EditText et = (EditText)this.findViewById(R.id.edittext_out);
		String msg = et.getText().toString();
		msg += "\n";
		
		btCom.write(msg);
		return;
	}
	
	private View.OnClickListener clickListener = new View.OnClickListener() {
	
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_send:
				sendMessage();
				break;
			case R.id.edittext_out:
				mOutEditText.setText("");
				break;
			}
			return;
		}
	};
	
	class DoneOnEditorActionListener implements OnEditorActionListener {
	    @Override
	    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	        if (actionId == EditorInfo.IME_ACTION_DONE) {
	            InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	            sendMessage();
	            return true;	
	        }
	        return false;
	    }
	}
	
}