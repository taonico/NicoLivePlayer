# Android Nico Live Player
## ニコニコ生放送
ニコニコミュニティ
## [プログラム超初心者がニコ生あぷり作成♪](http://com.nicovideo.jp/community/co1460511)

Androidでニコ生動画とコメントのビューアーアプリを作成しているコミュニティーへの応援プログラムです。
Eclipse の Git Reposit 機能により作成しました。
PCでも動くように通信部分のロジックは共通コードにしました。

1. NicoMessage
1. NicoRequest
1. NicoSocket
1. OnReceiveListener

PCでの JAVA Runtime JVM (JRE) で動作させるには、[Apache HttpClient](http://hc.apache.org/httpclient-3.x/)が必要です。

## 実装されている機能
1. 通常ログインからのクッキー取得 site=nicolive
1. 番組情報を取得 getplayerstatus
1. 番組からのコメント取得（取得開始コメントは、接続時からのコメント）
1. アラートログイン site=nicolive_antenna,getalertstatus
1. アラート受信（ソケット通信）


##  JAVA Runtime JVM (JRE) Sample code
	package jp.tao.nico.player;

	import jp.tao.nico.live.*;

	public class NicoPlayerCommon implements OnReceiveListener{
		private NicoMessage nicoMesssage = null;
		private NicoRequest nico = null;
		private NicoSocket nicosocket = null;
	
		public NicoPlayerCommon(){
			nicoMesssage = new NicoMessage();
        	nico = new NicoRequest(nicoMesssage);
		}
	
		public static void main(String args[]){
			new NicoPlayerCommon().start();
		}

		public void start() {
			System.out.println(nico.login("mail@mail.net", "password"));
			System.out.println(nico.getPlayerStatus("lv79157921"));
		
			nicosocket = new NicoSocket(nicoMesssage);
			nicosocket.onReceive(this);
			nicosocket.connectCommentServer(nico.getAddress(), nico.getPort(), nico.getThread());
			new Thread(nicosocket).start();	
		}
	
	 	public void onReceive(String receivedMessege){
			 System.out.println(receivedMessege);
		 }
	}