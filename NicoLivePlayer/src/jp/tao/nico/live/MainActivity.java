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

public class MainActivity extends Activity {
	private NicoMessage nicoMesssage = null;
	private NicoRequest nicoRequest = null;
	private NicoSocket nicosocket = null;
	
	private EditText etResponse;
	//ビデオ表示
	private WebView video;
	private String _url = "";
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
        //クッキーを受け取る
        nicoRequest.setLoginCookie(getIntent().getStringExtra("LoginCookie"));
        NicoWebView nwv = new NicoWebView(nicoRequest.getLoginCookie(), video);
        
        //ニコ生ページをロードする
        nwv.loadUrl();
        _url = NicoWebView.CONNECT_URL;
        //WebViewがページを読み込みを開始した時のイベント通知ハンドラを設定
        nwv.setOnPageStartedHandler(new Handler(new ChangedUrlHandler()));
    }
    
    /**
     * WebViewのURL変更時の処理
     */
    class ChangedUrlHandler implements Handler.Callback {
    	public boolean handleMessage(Message message) {
    		switch (message.what){
    		case NicoWebView.ON_PAGE_STARTED:
    			if (isChangedUrl(message.obj.toString())){
    				new GetComment().getComment();
    				return true;
    			}
    			break;
    		}
    		return false;
    	}
    	private boolean isChangedUrl(String url){
    		if (!_url.equals(url)){
    			_url = url;
    			return true;
    		}
    		return false;
    	}
    }
    
    /**
     * 放送ページのコメント取得処理
     */
    class GetComment implements Handler.Callback, OnReceiveListener, Runnable {
    	final Handler handler = new Handler(this);
    	
    	public void getComment() {
        	_liveID = nicoMesssage.getLiveID(_url);
        	if (_liveID.equals("")){ return; }
        	
    		nicosocket = new NicoSocket(nicoMesssage);
    		nicosocket.setOnReceiveListener(this);
    		
    		new Thread(this).start();
    	}
    	
    	public void run() {
			nicoRequest.getPlayerStatus(_liveID);
			nicosocket.connectCommentServer(nicoRequest.getAddress(), nicoRequest.getPort(), nicoRequest.getThread());
			Message message = handler.obtainMessage();
			handler.sendMessage(message);
		}
    	
		public boolean handleMessage(Message msg) {
			if (nicosocket.isConnected()){
				new Thread(nicosocket).start();	
			}else{
				etResponse.setText("番組に接続できませんでした");
			}
			
			return true;
		}
		
		public void onReceive(String receivedMessege){
			etResponse.append(receivedMessege + "\n");
	    }
	}
}
