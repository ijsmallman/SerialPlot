package org.SerialPlot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;

public class DataBuffer {
	
	private Context mContext;
	private Handler mHandler;
	
	private ArrayList<Number> mBuffer;
	
	// Message types sent to the main activity
    public static final int MESSAGE_NEW_VALUE = 100;
    
    //for debug
    Random r = new Random();
	
	public DataBuffer(Context context, Handler handler){
		mContext = context;
		mHandler = handler;
		
		mBuffer = new ArrayList<Number>();
	}
	
	public void add(String data, boolean notify){
		// try accounts for when crap is sent over serial
		//		- parseDouble will cause the app to crash
		//		  in this case.
		Double d;
		try {
			d = Double.parseDouble(data);
			mBuffer.add(d);
			if (notify) notifyNewVal(d);
		} catch (Exception e) {}
	}
	
	public void add(Number val){
		mBuffer.add(val);
	}
	
	public void remove(int index){
		mBuffer.remove(index);
	}
	
	public void clear() {
		mBuffer.clear();
	}
	
	public ArrayList<Number> getBuffer(){
		return mBuffer;
	}
	
	public List<Number> getLastElements(int n){
		int start = mBuffer.size()-n;
		if (start < 0){
			return mBuffer;
		} else {
			return mBuffer.subList(start, mBuffer.size());
		}
	}
	
	public void setBuffer(ArrayList<Number> data) {
		mBuffer = data;
	}
	
	public void notifyNewVal(Double d){
		mHandler.obtainMessage(MESSAGE_NEW_VALUE, d).sendToTarget();
	}
	
	public int size() {
		return mBuffer.size();
	}
}
