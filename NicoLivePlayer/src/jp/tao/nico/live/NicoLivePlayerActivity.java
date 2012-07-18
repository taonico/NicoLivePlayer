package jp.tao.nico.live;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author tao
 *
 */
public class NicoLivePlayerActivity extends Activity {
	private EditText email; 
	private EditText password;
	//通常のログインをする
	private Button btnLogin;
	//アラート受信用のログインをする（通常のログインしたアカウントはログアウトすることはない）
	private Button btnLoginAlert;
	//番組ID:lv000000000から番組情報を取得してコメントサーバに接続します
	//今後、放送Videoも取得したい
	private Button btnLiveNo;
	//コメントサーバまたはアラートコメントサーバからの接続を切ります
	private Button btnDisconnect;
	//状態表示、コメント表示	
	private EditText etResponse;
	//表示をPasswordから番組IDに書き換えています
	private TextView tvPassword;
	
	private NicoMessage nicoMesssage = null;
	private NicoRequest nico = null;
	private NicoSocket nicosocket = null;
	//
	private NicoInfoDataFile nicoInfoDataFile = null;
	private int _senderID = 0;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
                
        email = (EditText)findViewById(R.id.et_mail);
        password = (EditText)findViewById(R.id.et_password);
        btnLogin = (Button)findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new Login());
        btnLoginAlert = (Button)findViewById(R.id.btn_loginAlert);
        btnLoginAlert.setOnClickListener(new LoginAlert());
        btnLiveNo = (Button)findViewById(R.id.btnLive);
        btnLiveNo.setOnClickListener(new Live());
        btnDisconnect = (Button)findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(new Disconnect());
        etResponse = (EditText)findViewById(R.id.ed_response);
        tvPassword = (TextView)findViewById(R.id.tv_password);
        
        
        nicoMesssage = new NicoMessage();
        nico = new NicoRequest(nicoMesssage);
        nicoInfoDataFile = new NicoInfoDataFile();
        nicoInfoDataFile.storeFile();
    }
    /**
     * 設定ファイル保存と復元
     */
    class NicoInfoDataFile {
    	private NicoFile nicoFile = new NicoFile("NicoLivePlater.ini");
    	private CheckBox checkBox = (CheckBox)findViewById(R.id.cbIsStore);
    	public void saveFile() {
    		NicoInfoData data = new NicoInfoData();

    		if (checkBox.isChecked()){    		
    			data.mail = NicoCrypt.encrypt(NicoKey.getKey(), email.getText().toString());
    			data.password = NicoCrypt.encrypt(NicoKey.getKey(), password.getText().toString());
    			data.sessionCookie = NicoCrypt.encrypt(NicoKey.getKey(), nico.getLoginCookie());
    			data.lastUrl = NicoWebView.CONNECT_URL;
    			data.isStore = checkBox.isChecked();
    		} else {
    			data.mail = "0".getBytes();
    			data.password = "0".getBytes();
    			data.sessionCookie = NicoCrypt.encrypt(NicoKey.getKey(), nico.getLoginCookie());
    			data.lastUrl = NicoWebView.CONNECT_URL;
    			data.isStore = checkBox.isChecked();
    		}

    		nicoFile.saveFile(getApplicationContext(), data);
    	}
    	
    	public void storeFile(){
    		if (nicoFile.canReadFile(getApplicationContext())){
    			//ログインデータが保存されていれば、チェックボックスの状態を復元する
    			checkBox.setChecked(((NicoInfoData)nicoFile.openFile(getApplicationContext())).isStore);
    			//チェックボックスが付いれいれば、メールとパスワードを復元する
    			if (checkBox.isChecked()){
    				email.setText(NicoCrypt.decrypt(NicoKey.getKey(),
    						((NicoInfoData)nicoFile.openFile(getApplicationContext())).mail));
    				password.setText(NicoCrypt.decrypt(NicoKey.getKey(),
    						((NicoInfoData)nicoFile.openFile(getApplicationContext())).password));
    			}
    		}
    	}
    }
    
    /**
     * ログイン処理
     */
    class Login implements Handler.Callback, OnClickListener, Runnable {
    	final Handler handler = new Handler(this);
    	
    	public void onClick(View v) {
    		setSenderID(R.id.btn_login);
    		nicoInfoDataFile.saveFile();
    		key();
    		new Thread(this).start();
		}
    	
    	public void run() {
			nico.login(email.getText().toString(),password.getText().toString());
			Message message = handler.obtainMessage(R.id.btn_login);
			handler.sendMessage(message);
		}
    	
		public boolean handleMessage(Message arg0) {
			if (nico.isLogin()){
				tvPassword.setText("番組ID");
				password.setText("lv");
				password.setInputType(InputType.TYPE_CLASS_NUMBER);
				btnLogin.setVisibility(View.GONE);
				btnLoginAlert.setVisibility(View.GONE);
				btnLiveNo.setVisibility(View.VISIBLE);
				etResponse.setText("ログインしました");
				
				// インテントのインスタンス生
				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				// 次画面のアクティビティ起動
				intent.putExtra("LoginCookie", nico.getLoginCookie());
				startActivity(intent);
			}else{
				etResponse.setText("ログインできませんでした");
			}
			return true;
		}
    }
    
    /**
     * アラートログイン処理
     */
    class LoginAlert implements Handler.Callback, OnReceiveListener, OnClickListener, Runnable {
		final Handler handler = new Handler(this);
		
    	public void onClick(View v){
        	key();
    		setSenderID(R.id.btn_loginAlert);
    		nicosocket = new NicoSocket(nicoMesssage);
    		nicosocket.setOnReceiveListener(this);

    		new Thread (this).start();
        }
    	
    	public void run() {
			nico.loginAlert(email.getText().toString(),password.getText().toString());
			nicosocket.connectCommentServer(nico.getAlertAddress(), nico.getAlertPort(), nico.getAlertThread(),"1");
			Message message = handler.obtainMessage(R.id.btn_loginAlert);
			handler.sendMessage(message);
		}
    	
		public boolean handleMessage(Message msg) {
			if(nicosocket.isConnected()){
				new Thread(nicosocket.getAlertSocketRunnable()).start();
				btnLiveNo.setVisibility(View.GONE);
				btnLogin.setVisibility(View.GONE);
				btnLoginAlert.setVisibility(View.GONE);
    			btnDisconnect.setVisibility(View.VISIBLE);
			}else{
				etResponse.setText("アラートログインに失敗しました");
			}
			
			return true;
		}

		public void onReceive(String[] receivedMessege) {
			etResponse.append(receivedMessege[0] + ":" + receivedMessege[1] + ":" + receivedMessege[2] + "\n");
		}
    }
    
    /**
     * 生放送コメント取得処理
     */    
    class Live implements Handler.Callback, OnReceiveListener, OnClickListener, Runnable {
		final Handler handler = new Handler(this);
		
    	public void onClick(View v){
        	setSenderID(R.id.btnLive);
    		key();
    		nicosocket = new NicoSocket(nicoMesssage);
    		nicosocket.setOnReceiveListener(this);
    		new Thread(this).start();
        }
    	
    	public void run() {
			nico.getPlayerStatus(password.getText().toString());
			nicosocket.connectCommentServer(nico.getAddress(), nico.getPort(), nico.getThread(), "1000");
			Message message = handler.obtainMessage(R.id.btnLive);
			handler.sendMessage(message);
		}
    	
		public boolean handleMessage(Message msg) {
			if (nicosocket.isConnected()){
				new Thread(nicosocket).start();	
    			btnLiveNo.setVisibility(View.GONE);
    			btnDisconnect.setVisibility(View.VISIBLE);
			}else{
				etResponse.setText("番組に接続できませんでした");
			}
			
			return true;
		}

		public void onReceive(String[] receivedMessege) {
			etResponse.append(receivedMessege[0] + ":" + receivedMessege[1] + ":" + receivedMessege[2] + "\n");
		}
    }
    
    /**
     * コメントサーバ切断処理
     */
    class Disconnect implements OnClickListener {
    	public void onClick(View v){
    		switch (getSenderID()){

    		case R.id.btnLive : {
    			if(nicosocket.isConnected()){
    				if(nicosocket.closeSocket()){
    					etResponse.setText("disconnected");
    					btnLiveNo.setVisibility(View.VISIBLE);
    					btnDisconnect.setVisibility(View.GONE);
    				}
    			}
    			return;
    		}
    		case R.id.btn_loginAlert : {
    			if(nicosocket.isConnected()){
    				if(nicosocket.closeSocket()){
    					etResponse.setText("disconnected");
    					btnLogin.setVisibility(View.VISIBLE);
    					btnLoginAlert.setVisibility(View.VISIBLE);
    					btnDisconnect.setVisibility(View.GONE);
    				}
    			}
    			return;
    		}
    		}
    	}
    }
    
    public int getSenderID() {
		return this._senderID;
	}
    public void setSenderID(int senderID){
    	this._senderID = senderID;
    }
    
    private void key(){
    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
}