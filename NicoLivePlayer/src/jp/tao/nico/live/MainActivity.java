package jp.tao.nico.live;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnReceiveListener, Handler.Callback{
	private NicoMessage nicoMesssage = null;
	private NicoRequest nicoRequest = null;
	private NicoSocket nicosocket = null;
	
	private EditText etResponse;
	//ビデオ表示
	private WebView video;
	private String url = "";
	private String _liveID = "";
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        etResponse = (EditText)findViewById(R.id.ed_response_main);
        video = (WebView)findViewById(R.id.videoView);
        
        nicoMesssage = new NicoMessage();
        nicoRequest = new NicoRequest(nicoMesssage);
        nicoRequest.setLoginCookie(getIntent().getStringExtra("LoginCookie"));
        NicoWebView nwv = new NicoWebView(nicoRequest.getLoginCookie(), video);
        nwv.loadUrl();
        url = NicoWebView.CONNECT_URL;
        final Handler handler = new Handler(this);
        nwv.setHandler(handler);
    }
    
    public boolean handleMessage(Message message) {
		switch (message.what){
		case NicoWebView.ON_PAGE_STARTED:
			if (isChangedUrl(message.obj.toString())){
				getComment();
				return true;
			}
			break;
		}
		
		return false;
    }
    
    private void getComment() {
    	_liveID = nicoMesssage.getLiveID(url);
    	if (_liveID.equals("")){ return; }
    	
    	final Handler handler = new Handler(new CommentHandler());
		nicosocket = new NicoSocket(nicoMesssage);
		nicosocket.setOnReceiveListener(this);
		
		new Thread(new Runnable(){
			public void run() {
				nicoRequest.getPlayerStatus(_liveID);
				nicosocket.connectCommentServer(nicoRequest.getAddress(), nicoRequest.getPort(), nicoRequest.getThread());
    			Message message = handler.obtainMessage();
				handler.sendMessage(message);
			}}).start();
	}

	private boolean isChangedUrl(String url){
    	if (!this.url.equals(url)){
    		this.url = url;
    		return true;
    	}
    	
    	return false;
    }
	
	public void onReceive(String receivedMessege){
		etResponse.append(receivedMessege + "\n");
		return;
    }

	public void setSenderID(int senderID) {

	}

	public int getSenderID() {
		return 0;
	}
	
	class CommentHandler implements Handler.Callback{
		public boolean handleMessage(Message msg) {
			if (nicosocket.isConnected()){
				new Thread(nicosocket).start();	
			}else{
				etResponse.setText("番組に接続できませんでした");
			}
			
			return true;
		}
	}
}
