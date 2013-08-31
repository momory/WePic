package momory.service.wepic;

import org.json.JSONException;
import org.json.JSONObject;

import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;

import android.content.ContentResolver;
import android.database.Cursor;

public class UploadPicture extends BackgroundService {
	
	private final static String TAG = UploadPicture.class.getSimpleName();
	private String mHelloTo = "World";
	private int preCount = 0;
	private int newCount = 0;
	private int totalInsertCount = 0;
	private int cnt = 0;
	private long preTime = 0;
	private Cursor cur;
	private JSONObject result;

	@Override
	protected JSONObject doWork() {
		result = new JSONObject();
		try {
		ContentResolver cr = getContentResolver();
		cur =	cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
				null, 
				null, 
				null, 
				null);
		
		if(cnt == 0){	// 최초 실행시
			preCount = cur.getCount();	// first time total image count
			newCount = cur.getCount();
			this.preTime = Long.valueOf(String.valueOf(System.currentTimeMillis()).substring(0, 10));
			Log.d(TAG, "first run sec : " + this.preTime);
			cnt = 1;
		} else {		// 감시 중....
			newCount = cur.getCount();	// 새로 카운팅된 이미지 수를 newCount에 넣음
			if(preCount < newCount){	// 기존의 이미지 수보다 새로운 이미지 수가 많다면,
				Log.d(TAG, "insert new image");
				cur =	cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						null,
						"date_added > " + this.preTime + " and date_modified > " + this.preTime,
						null, 
						null);
				if(cur.getCount() > 0){
					if(cur.moveToFirst()){
						do{
							Log.d(TAG, "file name  : " + cur.getString(cur.getColumnIndex(MediaColumns.DISPLAY_NAME)));
							Log.d(TAG, "file path  : " + cur.getString(cur.getColumnIndex(Images.ImageColumns.DATA)));
							Log.d(TAG, "data added : " + cur.getString(cur.getColumnIndex(Images.ImageColumns.DATE_ADDED)));
							totalInsertCount ++;
						}while(cur.moveToNext());
					}
				} else {
					Log.d(TAG, "this image file is download file!!");
				}
			}	//end of checking new image
			preCount = newCount;
		}
		cur.close();
		
			result.put("Message", "total insert : " + totalInsertCount);
		} catch (JSONException e) {
		}

		return result;	
	}

	@Override
	protected JSONObject getConfig() {
		JSONObject result = new JSONObject();
		
		try {
			result.put("HelloTo", this.mHelloTo);
		} catch (JSONException e) {
		}
		
		return result;
	}

	@Override
	protected void setConfig(JSONObject config) {
		try {
			if (config.has("HelloTo"))
				this.mHelloTo = config.getString("HelloTo");
		} catch (JSONException e) {
		}
		
	}     

	@Override
	protected JSONObject initialiseLatestResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void onTimerEnabled() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onTimerDisabled() {
		// TODO Auto-generated method stub
		
	}
}