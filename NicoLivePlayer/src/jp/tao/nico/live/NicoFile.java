package jp.tao.nico.live;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.os.Environment;


public class NicoFile {
	private final String FileNotFound = "ファイルがみつかりません";
	private String errStatus = null;
	private String saveFileName = null;
	
	public NicoFile(String saveFileName){
		this.saveFileName = saveFileName;
	}
	public String getErrorStatus()
	{
		return errStatus;
	}
	
	public boolean saveFile(Context applicationContext, Object saveData){
		ObjectOutputStream oos = null;
		
		try {
			oos = new ObjectOutputStream(applicationContext.openFileOutput(saveFileName, Context.MODE_PRIVATE));
		} catch (FileNotFoundException e) {
			errStatus = FileNotFound;
			return false;
		} catch (IOException e) {
			return false;
		}
		
		errStatus = "";
		return saveFile(oos, saveData);
	}
	/**
	 * @param saveFileOutputStream =new ObjectOutputStream(openFileOutput(saveFileName, Context.MODE_PRIVATE));
	 */
	public boolean saveFile(ObjectOutputStream saveFileOutputStream, Object saveData){
		try
		{			
			saveFileOutputStream.writeObject(saveData);
			saveFileOutputStream.flush();
			saveFileOutputStream.close();            
		}catch(IOException e){
			return false;
		}
		
		errStatus = "";
		return true;
	}
	
	/**
	 * 
	 * @return NicoInfoData
	 */
	public Object openFile(Context applicationContext){
		try {
			return openFile(applicationContext.openFileInput(saveFileName));
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	/**
	 * FileInputStream fis = activity.openFileInput(filename);
	 * @return NicoInfoData
	 */
	public Object openFile(FileInputStream fis){
		Object fileData;
		
		try{
			ObjectInputStream ois = new ObjectInputStream(fis);
			fileData = ois.readObject();
			ois.close();
		}catch(IOException e){
			this.errStatus = "";
			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
		
        return fileData;
	}
	
	/**
	 * AndroidManifest.xmlに追加
	 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"></uses-permission>
	 */
	public boolean saveFileAtSDcard(){
		String sdCardDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		
		errStatus = "";
		return true;
	}
	
	public boolean canReadFile(Context applicationContext){
		if (!applicationContext.getFileStreamPath(saveFileName).exists()){ return false; }
		if (applicationContext.getFileStreamPath(saveFileName).canRead()){ return true; }
		return false;
	}
}

