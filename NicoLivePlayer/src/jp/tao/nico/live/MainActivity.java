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
	private String _url = "";
	private String _liveID = "";
	private NicoWebView _nicoWebView;
	private String _embed1 = "<embed type=\"application/x-shockwave-flash\" src=\"http://nl.nimg.jp/sp/swf/spplayer.swf?120501105350\" width=\"100%\" height=\"100%\" style=\"\" id=\"flvplayer\" name=\"flvplayer\" bgcolor=\"#FFFFFF\" quality=\"high\" allowscriptaccess=\"always\" flashvars=\"playerRev=120501105350_0&amp;playerTplRev=110721071458&amp;playerType=sp&amp;v=";
	private String _embed2 = "&amp;lcname=&amp;pt=community&amp;category=&amp;watchVideoID=&amp;videoTitle=&amp;gameKey=&amp;gameTime=&amp;isChannel=&amp;ver=2.5&amp;userOwner=false&amp;us=0\">";
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        _commentList = new NicoCommentListView((ListView)findViewById(R.id.commentListView2), getApplicationContext());
        /*_commentList.append(new String[]{"123","sFDtGZH13i_HWs6x9_l9lDSo_fk","こんばんは"});
        _commentList.append(new String[]{"124","52125133","こんばんはーーー"});
        _commentList.append(new String[]{"125","564884","こんばんは"});
        _commentList.append(new String[]{"126","134536","こんばんはーーー"});*/

        nicoMesssage = new NicoMessage();
        nicoRequest = new NicoRequest(nicoMesssage);
        //クッキーを受け取る
        nicoRequest.setLoginCookie(getIntent().getStringExtra("LoginCookie"));
        _nicoWebView = new NicoWebView(nicoRequest.getLoginCookie(), (WebView)findViewById(R.id.videoView));
        
        //ニコ生ページをロードする
        _nicoWebView.loadUrl();
        _url = NicoWebView.CONNECT_URL;
        //WebViewがページを読み込みを開始した時のイベント通知ハンドラを設定
        _nicoWebView.setOnPageStartedHandler(new Handler(new ChangedUrlHandler()));
        //WebViewがページを読み込みを完了した時のイベント通知ハンドラを設定
        _nicoWebView.setOnPageFinishedHandler(new Handler(new OnPageFinishedHandler()));
    }
    
    /**
     * WebViewのURL変更時の処理
     */
    private class ChangedUrlHandler implements Handler.Callback {
    	public boolean handleMessage(Message message) {
    		switch (message.what){
    		case NicoWebView.ON_PAGE_STARTED:
    			if (isChangedUrl(message.obj.toString())){
    				_liveID = nicoMesssage.getLiveID(_url, true);
    	        	if (_liveID.equals("")){ return false; }
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
    private class OnPageFinishedHandler implements Handler.Callback {
		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == NicoWebView.ON_PAGE_FINISHED){
	        	if (nicoMesssage.getLiveID(msg.obj.toString(), true).equals("")){ return false; }
	        	
				//Test用
	        	//_nicoWebView.loadData("<a href=\"http://sp.live.nicovideo.jp/\">sp.live.nicovideo.jp</a>");
	        	
	        	//embed tag を確認
	        	//System.out.println(_embed1 + _liveID + _embed2);	
	        	//直接 embed tag をLoadして、Nico Live Flash Playerを作成する
				//_nicoWebView.loadData(_embed1 + _liveID + _embed2);
	        	_nicoWebView.loadDataWithBaseURL(_embed1 + _liveID + _embed2);
	        	
	        	//ニコニコ生放送のJavascript Objets - SWFObject#write(so.write) を利用して Nico Live Flash Playerを作成する
	        	//_nicoWebView.loadUrl("javascript:document.write('<div id=\\\"sp_player\\\"></div>\');so.write('sp_player');");
				     	
	        	//JavaScript版
	        	//String playerUrl = "http://www.geocities.jp/geojavascript/NicoSPFlashPlayerTest.html?";
	        	//String playerUrl = "http://www.geocities.jp/geojavascript/NicoSPFlashPlayer.html?";
	        	
	        	//CGI版
	        	//String playerUrl = "http://www41.atpages.jp/taonico/NicoPlayer/test.cgi?v=";
	        	//String playerUrl = "http://www41.atpages.jp/taonico/NicoPlayer/index.cgi?v=";
	        	
	        	//_nicoWebView.loadUrl(playerUrl + _liveID);
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
