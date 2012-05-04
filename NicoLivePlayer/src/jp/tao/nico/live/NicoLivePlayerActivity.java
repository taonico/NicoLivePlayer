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
	//番組ID入力欄、じつはパスワード欄を再利用しています
	private EditText etLiveNo;
	//状態表示、コメント表示	
	private EditText etResponse;
	//表示をPasswordから番組IDに書き換えています
	private TextView tvPassword;
	
	private NicoMessage nicoMesssage = null;
	private NicoRequest nico = null;
	private NicoSocket nicosocket = null;
	private int _senderID = 0;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
                
        email = (EditText)findViewById(R.id.et_mail);
        password = (EditText)findViewById(R.id.et_password);
        btnLogin = (Button)findViewById(R.id.btn_login);
        //btnLogin.setOnClickListener(this);
        btnLoginAlert = (Button)findViewById(R.id.btn_loginAlert);
        btnLiveNo = (Button)findViewById(R.id.btnLive);
        btnDisconnect = (Button)findViewById(R.id.btnDisconnect);
        etLiveNo = (EditText)findViewById(R.id.et_password);
        etResponse = (EditText)findViewById(R.id.ed_response);
        tvPassword = (TextView)findViewById(R.id.tv_password);
        
        //
        nicoMesssage = new NicoMessage();
        nico = new NicoRequest(nicoMesssage);
    }
    
    /**
     * ログイン処理
     */
    public void onLoginButtonClick(View v){
    	setSenderID(R.id.btn_login);
		key();    			
		final Handler handler = new Handler(new LoginHandler());
		
		new Thread((new Runnable(){
			public void run() {
				nico.login(email.getText().toString(),password.getText().toString());
				Message message = handler.obtainMessage(R.id.btn_login);
				handler.sendMessage(message);
			}})).start();
		new Intent(this, MainActivity.class);
		return;
    }
    class LoginHandler implements Handler.Callback{
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
    public void onLoginAlertButtonClick(View v){
    	key();
		setSenderID(R.id.btn_loginAlert);
		
		LoginAlertHandler loginAlertHandler = new LoginAlertHandler();
		final Handler handler = new Handler(loginAlertHandler);
		nicosocket = new NicoSocket(nicoMesssage);
		nicosocket.setOnReceiveListener(loginAlertHandler);
		
		new Thread (new Runnable(){
			public void run() {
				nico.loginAlert(email.getText().toString(),password.getText().toString());
				nicosocket.connectCommentServer(nico.getAlertAddress(), nico.getAlertPort(), nico.getAlertThread());
				Message message = handler.obtainMessage(R.id.btn_loginAlert);
				handler.sendMessage(message);
			}}).start();
    }
    class LoginAlertHandler implements Handler.Callback, OnReceiveListener{
		public boolean handleMessage(Message msg) {
			if(nicosocket.isConnected()){
				new Thread(nicosocket.getAlertSocketRun()).start();
				btnLiveNo.setVisibility(View.GONE);
				btnLogin.setVisibility(View.GONE);
				btnLoginAlert.setVisibility(View.GONE);
    			btnDisconnect.setVisibility(View.VISIBLE);
			}else{
				etResponse.setText("アラートログインに失敗しました");
			}
			
			return true;
		}

		public void onReceive(String receivedMessege) {
			etResponse.append(receivedMessege + "\n");
		}
    }
    
    /**
     * 生放送コメント取得処理
     */
    public void onLiveButtonClick(View v){
    	setSenderID(R.id.btnLive);
		key();
		LiveHandler liveHandler = new LiveHandler();
		final Handler handler = new Handler(liveHandler);
		nicosocket = new NicoSocket(nicoMesssage);
		nicosocket.setOnReceiveListener(liveHandler);
		
		new Thread(new Runnable(){
			public void run() {
				nico.getPlayerStatus(etLiveNo.getText().toString());
				nicosocket.connectCommentServer(nico.getAddress(), nico.getPort(), nico.getThread());
    			Message message = handler.obtainMessage(R.id.btnLive);
				handler.sendMessage(message);
			}}).start();
    }
    class LiveHandler implements Handler.Callback, OnReceiveListener{
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

		public void onReceive(String receivedMessege) {
			etResponse.append(receivedMessege + "\n");
		}
    }
    
    /**
     * コメントサーバ切断処理
     */
    public void onDisconnectButtonClick(View v){
    	switch (getSenderID()){

    	case R.id.btnLive : {
    		if(nicosocket.isConnected()){
    			if(nicosocket.closeSockt()){
    				etResponse.setText("disconnected");
    				btnLiveNo.setVisibility(View.VISIBLE);
    				btnDisconnect.setVisibility(View.GONE);
    			}
    		}
    		return;
    	}
    	case R.id.btn_loginAlert : {
    		if(nicosocket.isConnected()){
    			if(nicosocket.closeSockt()){
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