package jp.tao.nico.live;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NicoLivePlayerActivity extends Activity implements OnClickListener, OnReceiveListener {
	private EditText email; 
	private EditText password;
	private Button btnLogin;
	private Button btnLiveNo;
	private Button btnDisconnect;
	private EditText etLiveNo;
	private EditText etResponse;
	private TextView tvPassword;
	
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
        
        
        nicoMesssage = new NicoMessage();
        nico = new NicoRequest(nicoMesssage);
    }
    
    public void onClick(View v){
    	switch (v.getId()) {
    		case R.id.btn_login :{
    			key();
    			etResponse.setText(nico.login(email.getText().toString(),password.getText().toString()));
    			
    			if (nico.isLogin()){
    				tvPassword.setText("番組ID");
        			password.setText("lv");
        			password.setInputType(InputType.TYPE_CLASS_NUMBER);
        			btnLogin.setVisibility(View.GONE);
        			btnLiveNo.setVisibility(View.VISIBLE);
    			}
    			
    			break;
    		}
    		
    		case R.id.btnLive : {
    			key();
    			etResponse.setText(nico.getPlayerStatus(etLiveNo.getText().toString()));
    			nicosocket = new NicoSocket(nicoMesssage);
    			nicosocket.onReceive(this);
    			nicosocket.connectCommentServer(nico.getAddress(), nico.getPort(), nico.getThread());
    			if (nicosocket.isConnected()){
    				new Thread(nicosocket).start();	
        			btnLiveNo.setVisibility(View.GONE);
        			btnDisconnect.setVisibility(View.VISIBLE);
    			}else{
    				etResponse.setText("番組に接続できませんでした");
    			}
    			
    			break;
    		}
    		
    		case R.id.btnDisconnect : {
    			if(nicosocket.isConnected()){
    				if(nicosocket.closeSockt()){
    					etResponse.setText("disconnected");
    					btnLiveNo.setVisibility(View.VISIBLE);
    					btnDisconnect.setVisibility(View.GONE);
    				}
    			}
    			
    		}
    	}
    }

    public void onReceive(String receivedMessege){
    	etResponse.append(receivedMessege + "\n");
    }
    
    private void key(){
    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
}