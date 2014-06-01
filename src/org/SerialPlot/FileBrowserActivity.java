package org.SerialPlot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class FileBrowserActivity extends ListActivity{
	
	private File currentDir;
	private FileArrayAdapter adapter;
	
	private Stack<File> dirStack = new Stack<File>();
	
	public static String EXTRA_FILE_PATH = "file_path";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		currentDir = new File("/sdcard/");
		fill(currentDir);
	}
	
	private void fill(File f){
		
		File[] dirs = f.listFiles();
		this.setTitle("Current Dir: " + f.getName());
		
		List<Option> dir = new ArrayList<Option>();
		List<Option> fls = new ArrayList<Option>();
		
		try{
			for(File ff : dirs)
			{
				if(ff.isDirectory())
					dir.add(new Option(ff.getName(),"Folder",ff.getAbsolutePath()));
				else
					fls.add(new Option(ff.getName(),"File Size:"+ff.length(),ff.getAbsolutePath()));
			}
		} catch (Exception e) {}
		
		Collections.sort(dir);
		Collections.sort(fls);
		dir.addAll(fls);
		if(!f.getName().equalsIgnoreCase("sdcard"))
			dir.add(0,new Option("..","Parent Directory",f.getParent()));
		
		adapter = new FileArrayAdapter(FileBrowserActivity.this,R.layout.file_view,dir);
		this.setListAdapter(adapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		super.onListItemClick(l, v, position, id);
		Option o = adapter.getItem(position);
		if(o.getData().equalsIgnoreCase("folder")) {
			dirStack.push(currentDir);
			currentDir = new File(o.getPath());
			fill(currentDir);
		}
		else if (o.getData().equalsIgnoreCase("parent directory")) {
			currentDir = dirStack.pop();
			fill(currentDir);
		}
		else {
			onFileClick(o);
		}
	}
	
	private void onFileClick(Option o)
	{
		// Create the result Intent and include the MAC address
        Intent intent = new Intent();
        intent.putExtra(EXTRA_FILE_PATH, o.getPath());
        
        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();
	}
	
	@Override
	public void onBackPressed() {
		if (dirStack.size() == 0)
		{
			finish();
			return;
		}
		currentDir = dirStack.pop();
		fill(currentDir);
	}

}
