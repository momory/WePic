package momory.service.wepic;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Thumbnails;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;

public class UploadPicture extends BackgroundService {

	private final static String TAG = UploadPicture.class.getSimpleName();
	private String mHelloTo = "World";
	private int cnt = 0;
	private long preTime = 0;
	private Cursor mediaDbCur;
	private Cursor thumbnailsDbCur;

	private FileInputStream mFileInputStream = null;
	private URL connectUrl = null;

	@Override
	protected JSONObject doWork() {
		// return obj
		JSONObject result = new JSONObject();

		// 최초 실행시 현재의 타임 기록
		if(cnt == 0){
			this.preTime = Long.valueOf(String.valueOf(System.currentTimeMillis()).substring(0, 10));
			Log.d(TAG, "first share sec : " + this.preTime);
			cnt = cnt + 1;
		}

		// observer pattern으로 DB check
		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// if external content get
				getContentResolver().registerContentObserver(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, 
						new ContentObserver(new Handler()) {
					@Override
					public void onChange(boolean selfChange) {
						Log.d("ScratchService","External Media has been added!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						uploadImage();
						super.onChange(selfChange);
					}
				});
				// if internal content get
				getContentResolver().registerContentObserver(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, false, 
						new ContentObserver(new Handler()) {
					@Override
					public void onChange(boolean selfChange) {
						Log.d("ScratchService","Internal Media has been added!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						uploadImage();
						super.onChange(selfChange);
					}
				});
			}
		}, 0);
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

	protected void uploadImage(){
		try {
			ContentResolver cr = getContentResolver();

			Log.d(TAG, "seach new image file");

			mediaDbCur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					null,
					"date_added > " + this.preTime + " and date_modified > " + this.preTime,
					null, 
					null);
			if(mediaDbCur.getCount() > 0){
				if(mediaDbCur.moveToFirst()){
					do{
						Log.d(TAG, "file name  : " + mediaDbCur.getString(mediaDbCur.getColumnIndex(MediaColumns.DISPLAY_NAME)));
						Log.d(TAG, "file path  : " + mediaDbCur.getString(mediaDbCur.getColumnIndex(Images.ImageColumns.DATA)));
						Log.d(TAG, "data added : " + mediaDbCur.getString(mediaDbCur.getColumnIndex(Images.ImageColumns.DATE_ADDED)));
						//						DoFileUpload();
						// update image
//						Log.d(TAG, "mini_thubm1 : " + mediaDbCur.getString(mediaDbCur.getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
						Log.d(TAG, "mini_thubm2 : " + mediaDbCur.getString(mediaDbCur.getColumnIndex(MediaColumns._ID)));
//						Log.d(TAG, "mini_thubm3 : " + mediaDbCur.getString(mediaDbCur.getColumnIndex(Thumbnails.DATA)));
						
						getThumbnailPath(mediaDbCur.getString(mediaDbCur.getColumnIndex(MediaColumns._ID)));
						
					}while(mediaDbCur.moveToNext());
					this.preTime = Long.valueOf(String.valueOf(System.currentTimeMillis()).substring(0, 10));
				}
			} else {
				Log.d(TAG, "delete image file.. skip.........!!");
			}
			mediaDbCur.close();

		} catch (Exception e) {
		}
	}

	private void DoFileUpload(String filePath) throws IOException {
		Log.d("Test" , "file path = " + filePath);		
		HttpFileUpload("http://www.inculab.net/upload.php", "", filePath);	
	}

	private void HttpFileUpload(String urlString, String params, String fileName) {

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";	

		try {
			mFileInputStream = new FileInputStream(fileName);			
			connectUrl = new URL(urlString);
			Log.d("Test", "mFileInputStream  is " + mFileInputStream);

			// open connection 
			HttpURLConnection conn = (HttpURLConnection)connectUrl.openConnection();			
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

			// write data
			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + fileName+"\"" + lineEnd);
			dos.writeBytes(lineEnd);

			int bytesAvailable = mFileInputStream.available();
			int maxBufferSize = 1024;
			int bufferSize = Math.min(bytesAvailable, maxBufferSize);

			byte[] buffer = new byte[bufferSize];
			int bytesRead = mFileInputStream.read(buffer, 0, bufferSize);

			Log.d("Test", "image byte is " + bytesRead);

			// read image
			while (bytesRead > 0) {
				dos.write(buffer, 0, bufferSize);
				bytesAvailable = mFileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = mFileInputStream.read(buffer, 0, bufferSize);
			}	

			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			// close streams
			Log.e("Test" , "File is written");
			mFileInputStream.close();
			dos.flush(); // finish upload...			

			// get response
			int ch;
			InputStream is = conn.getInputStream();
			StringBuffer b =new StringBuffer();
			while( ( ch = is.read() ) != -1 ){
				b.append( (char)ch );
			}
			String s=b.toString(); 
			Log.e("Test", "result = " + s);
			dos.close();			

		} catch (Exception e) {
			Log.d("Test", "exception " + e.getMessage());
			// TODO: handle exception
		}
	}

	private String getThumbnailPath(String miniThumbMagic){

		String theumbPath = "";
		ContentResolver cr = getContentResolver();

		thumbnailsDbCur =	cr.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
				null,
				Thumbnails.IMAGE_ID + "=?",
				new String[]{String.valueOf(miniThumbMagic)}, 
				null);
		if(thumbnailsDbCur.getCount() > 0){
			if(thumbnailsDbCur.moveToLast()){
				long id = thumbnailsDbCur.getLong(0);  
				int dataId = thumbnailsDbCur.getColumnIndex(Images.Thumbnails.DATA);  
				theumbPath = thumbnailsDbCur.getString(dataId);  
				Log.d(TAG, "id = " + id );  
				Log.d(TAG, "strThumPath = " + theumbPath );
			}
		} else {
			Log.d(TAG, "delete image file.. skip.........!!");
			Log.d(TAG, "miniThumbMagic : " + miniThumbMagic);
			
		}
		thumbnailsDbCur.close();

		return theumbPath;
	}
}