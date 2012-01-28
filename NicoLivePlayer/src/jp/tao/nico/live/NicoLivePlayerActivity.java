package jp.tao.nico.live;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class NicoLivePlayerActivity extends Activity implements OnClickListener, OnReceiveListener, Handler.Callback {
	private EditText email; 
	private EditText password;
	private Button btnLogin;
	private Button btnLiveNo;
	private Button btnDisconnect;
	private EditText etLiveNo;
	private EditText etResponse;
	private TextView tvPassword;
	private VideoView video;
	
	private NicoMessage nicoMesssage = null;
	private NicoRequest nico = null;
	private NicoSocket nicosocket = null;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        email = (EditText)findViewById(R.id.et_mail);
        password = (EditText)findViewById(R.id.et_password);
        btnLogin = (Button)findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(this);
        btnLiveNo = (Button)findViewById(R.id.btnLive);
        btnLiveNo.setOnClickListener(this);
        btnDisconnect = (Button)findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(this);
        etLiveNo = (EditText)findViewById(R.id.et_password);
        etResponse = (EditText)findViewById(R.id.ed_response);
        tvPassword = (TextView)findViewById(R.id.tv_password);
        video = (VideoView)findViewById(R.id.videoview);
        
        
        nicoMesssage = new NicoMessage();
        nico = new NicoRequest(nicoMesssage);
    }
    
    public void onClick(View v){
    	switch (v.getId()) {
    		case R.id.btn_login :{
    			key();    			
    			final Handler handler = new Handler(this);
    			
    			new Thread((new Runnable(){
    				public void run() {
    					nico.login(email.getText().toString(),password.getText().toString());
    					Message message = new Message();
    					message.arg1 = R.id.btn_login;
    					handler.sendMessage(message);
    				}})).start();
    			
    			return;
			}
    		
    		case R.id.btnLive : {
    			key();
    			final Handler handler = new Handler(this);
    			nicosocket = new NicoSocket(nicoMesssage);
    			nicosocket.onReceive(this);
    			
    			new Thread(new Runnable(){
					public void run() {
						nico.getPlayerStatus(etLiveNo.getText().toString());		    			
		    			nicosocket.connectCommentServer(nico.getAddress(), nico.getPort(), nico.getThread());
		    			Message message = new Message();
    					message.arg1 = R.id.btnLive;
    					handler.sendMessage(message);
					}}).start();
    			
    			return;
    		}
    		
    		case R.id.btnDisconnect : {
    			if(nicosocket.isConnected()){
    				if(nicosocket.closeSockt()){
    					etResponse.setText("disconnected");
    					btnLiveNo.setVisibility(View.VISIBLE);
    					btnDisconnect.setVisibility(View.GONE);
    					video.stopPlayback();
    				}
    			}
    			return;
    		}
    	}
    }

    private void playVideo(Uri uri){
        video.requestFocus();
        video.setMediaController(new MediaController(this));
        video.setVideoURI(uri);
        video.start();
    }
    
    public void onReceive(String receivedMessege){
    	etResponse.append(receivedMessege + "\n");
    }
    
    private void key(){
    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

	public boolean handleMessage(Message arg0) {
		switch (arg0.arg1){
			case R.id.btn_login :{
				if (nico.isLogin()){
					tvPassword.setText("番組ID");
					password.setText("lv");
					password.setInputType(InputType.TYPE_CLASS_NUMBER);
					btnLogin.setVisibility(View.GONE);
					btnLiveNo.setVisibility(View.VISIBLE);
					etResponse.setText("ログインしました");
				}else{
					etResponse.setText("ログインできませんでした");
				}
				return true;
			}
			
			case R.id.btnLive : {
    			if (nicosocket.isConnected()){
    				new Thread(nicosocket).start();	
        			btnLiveNo.setVisibility(View.GONE);
        			btnDisconnect.setVisibility(View.VISIBLE);
        			//playVideo(uri);
    			}else{
    				etResponse.setText("番組に接続できませんでした");
    			}
    			
    			return true;
    		}
		}
		
		return false;
	}
}