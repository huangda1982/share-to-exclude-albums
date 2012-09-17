package me.huangda1982.ShareToExcludeAlbums;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String LIST_KEY_CHECKED = "checked";
	private static final String LIST_KEY_PATH = "path";

	List<Map<String, Object>> mList;

	{
		mList = new ArrayList<Map<String, Object>>();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
       
		String[] from = {LIST_KEY_CHECKED, LIST_KEY_PATH};
        int[] to = {R.id.list_item_check, R.id.list_item_text};

        SimpleAdapter adapter = new SimpleAdapter(this, mList, R.layout.list_item, from, to) {
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View view = super.getView(position, convertView, parent);
        		
        		view.setTag(position);
        		
        		return view;
        	}
        };
        
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
			
			public boolean setViewValue(View view, Object data,	String textRepresentation) {
				boolean rv = false;
				
				if (data.getClass() == Boolean.class) {
					CheckBox checkBox = (CheckBox) view;
					Boolean checked = (Boolean) data;
					if (checkBox != null && checked != null) {
						checkBox.setChecked(checked);
						
						checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener () {

							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								View parent = (View) buttonView.getParent();
								if (parent != null) {
									int position = (Integer) parent.getTag();
									Log.i("Info", "position = " + position);
									
									Map<String, Object> map = mList.get(position);
									map.put(LIST_KEY_CHECKED, isChecked);
								}
							}
						});
						
						rv = true;
					}
				}

				return rv;
			}
		});
		
		ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(adapter);

		Intent intent = getIntent();
		String action = intent.getAction();
	    Log.i("Info", "action = " + action);
		
		HashSet<String> set = new HashSet<String>();

		if (Intent.ACTION_SEND.equals(action)) {
			Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		    if (imageUri != null) {
		    	Log.i("Info", "uri = " + imageUri.toString());
		    	String path = this.getPath(imageUri);
		    	if (path != null) {
		    		set.add(path);
		    	}
		    }
	    } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
	    	ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
	    	if (imageUris != null) {
		    	for(Uri imageUri : imageUris) {
				    if (imageUri != null) {
				    	Log.i("Info", "uri = " + imageUri.toString());
				    	String path = this.getPath(imageUri);
				    	if (path != null) {
				    		set.add(path);
				    	}
				    }
		    	}
	    	}
	    }
	    
	    if (set.isEmpty()) {
	    	TextView txt = (TextView) findViewById(R.id.txtEclude);
	    	txt.setText(getString(R.string.strGetPathFailed));
	    	
	    	Button btnOK = (Button) findViewById(R.id.btnOK);
	    	btnOK.setEnabled(false);
	    } else {
	    	for (String path : set) {
	    		addIntoListView(path);
	    	}
	    }
    }
    
    public void onBtnClick(View view) {
    	switch (view.getId()) {
    	case R.id.btnOK:
    		this.exclude();
    		this.finish();
	   		break;
    	case R.id.btnCancle:
    		this.finish();
    	default:
    		break;
    	}
    }

	private void addIntoListView(String path) {
		this.addIntoListView(true, path);
	}

	private void addIntoListView(boolean checked, String path) {
        Map<String, Object> map;

        map = new HashMap<String, Object>();
        map.put(LIST_KEY_CHECKED, Boolean.valueOf(checked));
        map.put(LIST_KEY_PATH, path);

        mList.add(map);
    }

	private String getPath(Uri imageUri) {
    	Cursor c = getContentResolver().query(imageUri, null, null, null, null);
    	if (c.moveToFirst()) {
    		int colIndex = c.getColumnIndex(MediaStore.MediaColumns.DATA);
    		if (colIndex >= 0) {
	    		String path = c.getString(colIndex);
	    		path = new File(path).getParent();
	    		Log.i("Info", "path = " + path);
	    		return path;
    		}
    	}
    	
    	return null;
 	}
    
    private void exclude() {
		for (Map<String, Object> map : mList) {
			boolean checked = (Boolean) map.get(LIST_KEY_CHECKED);
			Log.i("Info", (String) map.get(LIST_KEY_PATH) + ": " + String.valueOf(checked));
			if (checked) {
				String path = (String) map.get(LIST_KEY_PATH);
				if (path != null) {
					this.exclude(path);
				}
			}
		}
	}

	private void exclude(String path) {
		File noMedia = new File(path + File.separator + getString(R.string.strNoMediaFile));
    	boolean rv = false;
        
		try {
			rv = noMedia.createNewFile();

			if (rv) {
				Uri uri = Uri.parse("file://" + path);
				Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, uri);
				sendBroadcast(intent);
				Log.i("Info", "Send broadcast: \"" + intent.toUri(0) + "\"");
			} else {
				errorMessage(true, R.string.strCreateFailed, noMedia.getAbsolutePath());
			}
		} catch (IOException e) {
			errorMessage(true, R.string.strCreateFailed, noMedia.getAbsolutePath());
			e.printStackTrace();
		}
    }

	private void errorMessage(boolean user, int id, Object ... args) {
		String strMsg = String.format(getString(id), args);
		Log.e("Error", strMsg);
		if (user) {
			Toast.makeText(this, strMsg, Toast.LENGTH_SHORT).show();
		}
	}
}
