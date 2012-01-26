package jp.tao.nico.live;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class NicoSocket implements Runnable{
	private final String SERVER_ERR_MESSAGE = "エラー内容：サーバーとの接続に失敗しました。";
	private final String SERVER_MESSAGE = "サーバーからのメッセージ：サーバーとの接続に成功しました。";
	private Socket socket = null;
    private BufferedReader reader = null;
    private OutputStreamWriter osw = null;
    private NicoMessage nicoMesssage = null;
	private OnReceiveListener onReceive;
    
	public NicoSocket(NicoMessage nicoMesssage){
		this.nicoMesssage = nicoMesssage;
	}
	
	public BufferedReader getCommentStream(){
		return this.reader;
	}
	
	public String connectCommentServer(String addr, int port, String thread) {
        
        try {
            // サーバーへ接続
            socket = new Socket(addr, port);
            
            osw = new OutputStreamWriter(socket.getOutputStream());
			osw.write(nicoMesssage.getChatMessage(thread));
			osw.flush();

            // メッセージ取得オブジェクトのインスタンス化
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
 
        } catch (UnknownHostException e) {
            return SERVER_ERR_MESSAGE;
        } catch (IOException e) {
            return SERVER_ERR_MESSAGE;
        }

        return SERVER_MESSAGE;
    }
	
	public void run(){
        while (socket.isConnected()){
            try{
            	nicoMesssage.getCommentMessage(reader, onReceive);
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }
	
	public void onReceive(final OnReceiveListener onReceive){
		this.onReceive = onReceive;
	}

	public boolean isConnected() {
		if (socket == null){
			return false;
		}
		return this.socket.isConnected();
	}
	
	public boolean closeSockt(){
		 try {
             // 接続終了処理
			 osw.close();
             reader.close();
             socket.close();
         } catch (IOException e) {
         	return false;
         }
		 
		return true;
	}
}
