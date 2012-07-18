package jp.tao.nico.live;

import jp.tao.nico.live.NicoSocket.NicoLiveComment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.WebView;
import android.widget.ListView;

@SuppressLint("ParserError")
public class MainActivity extends Activity {
	private NicoMessage nicoMesssage = null;
	private NicoRequest nicoRequest = null;
	private NicoLiveComment nicoLiveComment = null;
	
	private NicoCommentListView _commentList;
	//ビデオ表示
	private WebView video;
	private String _url = "";
	private String _liveID = "";
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        _commentList = new NicoCommentListView((ListView)findViewById(R.id.commentListView2), getApplicationContext());
        video = (WebView)findViewById(R.id.videoView);
        //_commentList.append(new String[]{"123","52125133","こんばんは"});
        //_commentList.append(new String[]{"124","52125133","こんばんはーーー"});

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

    	NicoSocket nicosocket;
    	
    	public void getComment() {
        	_liveID = nicoMesssage.getLiveID(_url);
        	if (_liveID.equals("")){ return; }
        	
        	if (nicosocket == null){
        		nicosocket = new NicoSocket(nicoMesssage);
        		nicosocket.setOnReceiveListener(this);
        	}
        	if (nicoLiveComment != null && nicoLiveComment.isConnected()){
        		if (nicoLiveComment.getLiveID().equals(_liveID)){
        			return;
        		}
        		else {
        			nicoLiveComment.close();
        		}
        	}
    		new Thread(this).start();
    	}
    	
    	public void run() {
			nicoRequest.getPlayerStatus(_liveID);
			nicoLiveComment = nicosocket.startNicoLiveComment(nicoRequest, _liveID);
			Message message = handler.obtainMessage();
			handler.sendMessage(message);
		}
    	
		public boolean handleMessage(Message msg) {
			if (nicoLiveComment.isConnected()){
				new Thread(nicoLiveComment).start();	
			}else{
				_commentList.append(new String[]{"Live ID:",_liveID,"番組に接続できませんでした"});
			}
			
			return true;
		}
		
		public void onReceive(String[] receivedMessege){
			if (receivedMessege[0].equals("chatresult")) {
				//txtSendMessage.setText(nicoLiveComment.getComment());
			}
        	else {
        		handler.post(new ReceivedMessege(receivedMessege));
        	}
			if (receivedMessege[2].equals("/disconnect") && nicoLiveComment.isConnected()){
				nicoLiveComment.close();
			}
	    }
		private class ReceivedMessege implements Runnable {
			private String[] receivedMessege;
			public ReceivedMessege(String[] receivedMessege){
				this.receivedMessege = receivedMessege;
			}
			@Override
			public void run() {
				_commentList.append(receivedMessege);
			}
		}
	}
}
