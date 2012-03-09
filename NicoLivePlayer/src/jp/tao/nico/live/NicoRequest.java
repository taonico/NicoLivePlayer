package jp.tao.nico.live;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;

import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class NicoRequest {
	//ログインAPI
	private final String _nicoHost = "secure.nicovideo.jp";
	private final String _nicoPath = "/secure/login";
	//認証API
	private final String _apiHost = "live.nicovideo.jp";
	private final String _getalertstatus = "/api/getalertstatus";
	//ユーザー認証を行わないAPI
	private final String _getalertinfo = "/api/getalertinfo";
	//番組情報取得API
	private final String _getstreaminfo = "/api/getstreaminfo/";
	//番組情報
	private final String _getplayerstatus = "/api/getplayerstatus";
	//NGリストを取得[XML] (放送主のみ)
	private final String _configurengword = "/configurengword?mode=get&video=";
	//
	private SchemeRegistry schemeRegistry = new SchemeRegistry();
	private HttpParams httpParams = new BasicHttpParams();
	private NicoMessage nicoMessage = null;
	//Login -> getplayerstatus
	private CookieStore _cookieStore;
	private String _loginCookie = null;
	
	//Alert server
	private String _alertaddr = null;
	private int _alertport;
	private String _alertthread = null;
	//getplayerstatus
	private PlayerStatusData _playerStatusData = null;
	private String _addr;
	private int _port;
	private String _thread;
	//Login
	private boolean _isLogin = false;
	//Login Alert
	private boolean _isLoginAlert = false;
	private NodeList _community_id = null;
	
	
	public NicoRequest (NicoMessage nicoMesssage){
		this.nicoMessage = nicoMesssage;
		
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
	    socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", socketFactory, 443));
		
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
	}
	
	public boolean isLogin(){
		return this._isLogin;
	}
	public boolean isLoginAlert(){
		return this._isLoginAlert;
	}
	public CookieStore getCookieStore(){
		return this._cookieStore;
	}
	public String getLoginCookie(){
		if (isLogin() && this._loginCookie.toString().equals("")){
			getLoginCookie(this._cookieStore);
		}
		return this._loginCookie;
	}
	
	public String login (String mail, String password) {
		
		try{
			
			DefaultHttpClient client = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, schemeRegistry), httpParams);
			
			/*
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.path(_nicoPath);
			uriBuilder.appendQueryParameter("site","nicolive");
			*/
			HttpPost post = new HttpPost(_nicoPath + "?site=nicolive");//uriBuilder.build().toString());

			// POST データの設定
			List<BasicNameValuePair> postParams = new ArrayList<BasicNameValuePair>();
			postParams.add(new BasicNameValuePair("mail", mail));
			postParams.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));

			client.execute(new HttpHost(_nicoHost, 443, "https"), post);
			_cookieStore = client.getCookieStore();
			
			/*HttpResponse response = 
			 * if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				//
				if (! _cookieStore.getCookies().isEmpty()){
					return _cookieStore.getCookies().get(0).getValue();
				}
				return "";
			}*/
		}
        catch (Exception e){
        	_isLogin = false;
        	return e.getMessage();//"ログインに失敗しました";
        }
		
		if (! _cookieStore.getCookies().isEmpty()) {
			_isLogin = true;
			return "ログインが成功しました";
		}else{
			_isLogin = false;
			return "ログインに失敗しました";
		}
	}
	public String getLoginCookie(CookieStore cookieStore){
		if ( cookieStore != null ) {
			List<Cookie> cookies = cookieStore.getCookies();
			if (!cookies.isEmpty()) {
				for (int i = 0; i < cookies.size(); i++) {
					if(isNicoVideoUserSession(cookies.get(i))){
						Cookie cookie = cookies.get(i);
						return cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
					}
				}
			}
		}
		return null;
	}
	private boolean isNicoVideoUserSession(Cookie cookie){
		if (cookie != null) {
			if (cookie.getDomain().equals(".nicovideo.jp") && cookie.getName().equals("user_session")){
				return true;
			}
		}
		return false;
	}
	
	public String loginAlert(String mail, String password){
		String ticket = null;
		
		try{
			
			DefaultHttpClient client = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, schemeRegistry), httpParams);
			
			//Uri.Builder uriBuilder = new Uri.Builder();
			//uriBuilder.path(_nicoPath);
			//uriBuilder.appendQueryParameter("site","nicolive_antenna");
			HttpPost post = new HttpPost(_nicoPath + "?site=nicolive_antenna");//uriBuilder.build().toString());

			// POST データの設定
			List<BasicNameValuePair> postParams = new ArrayList<BasicNameValuePair>();
			postParams.add(new BasicNameValuePair("mail", mail));
			postParams.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));

			HttpResponse response = client.execute(new HttpHost(_nicoHost, 443, "https"), post);
			
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				//
				if (response.getEntity().isStreaming()){
					ticket = nicoMessage.getNodeValue(getInputStream(response), "ticket");
				}
			}
			
			if(ticket != null){
				client = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, schemeRegistry), httpParams);
				//uriBuilder = new Uri.Builder();
				//uriBuilder.path(_getalertstatus);
				post = new HttpPost(_getalertstatus);//uriBuilder.build().toString());
				postParams = new ArrayList<BasicNameValuePair>();
				postParams.add(new BasicNameValuePair("ticket", ticket));
				post.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
				response = client.execute(new HttpHost(_apiHost, 80, "http"), post);
			}
			
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				Document doc = nicoMessage.getDocument(getInputStream(response));
				this._alertaddr = nicoMessage.getNodeValue(doc, "addr");
				this.set_alertport(nicoMessage.getNodeValue(doc, "port"));
				this._alertthread = nicoMessage.getNodeValue(doc, "thread");
				this._isLoginAlert = true;
				return "アラートログインしました";
			}
			
		}catch (Exception e){
			this._isLoginAlert = false;
        	return "errer " + e.getMessage();//"ログインに失敗しました";
        }
		
		this._isLoginAlert = false;
		return "アラートログインに失敗しました";
	}
	
	private InputStream getInputStream(HttpResponse response) throws IllegalStateException, IOException{
		return response.getEntity().getContent();
	}
	
	private String getResponceContents(HttpResponse response){
		try {
			return EntityUtils.toString( response.getEntity(), "UTF-8" );
		} catch (ParseException e) {
			e.getMessage();
		} catch (IOException e) {
			e.getMessage();
		}
		
		return null;
	}

	public String getAlertAddress() {
		return _alertaddr;
	}
	public int getAlertPort() {
		return _alertport;
	}
	public String getAlertThread() {
		return _alertthread;
	}

	public PlayerStatusData getPlayerStatus(String lv) {
		
		try {
			
			DefaultHttpClient client = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, schemeRegistry), httpParams);
			client.setCookieStore(_cookieStore);
		
			//Uri.Builder uriBuilder = new Uri.Builder();
			//uriBuilder.path(_getplayerstatus);
			//uriBuilder.appendQueryParameter("v", lv);
			HttpGet get = new HttpGet(_getplayerstatus + "?v=" + lv);//uriBuilder.build().toString());

			HttpResponse response = client.execute(new HttpHost(_apiHost, 80, "http"), get);
		
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				Document doc = nicoMessage.getDocument(getInputStream(response));
				this._playerStatusData  = nicoMessage.getPlayerStatusData(doc);
				this._addr = nicoMessage.getNodeValue(doc, "addr");
				this.set_port(nicoMessage.getNodeValue(doc, "port"));
				this._thread = nicoMessage.getNodeValue(doc, "thread");
				this._community_id = nicoMessage.getNodeList(doc, "community_id");
			}
			
		}catch (Exception e){
        	return null; //e.getMessage();
        }
		
		return this._playerStatusData;
	}

	public String getAddress() {
		return _addr;
	}
	public int getPort() {
		return _port;
	}
	private void set_port(String port) {
		this._port = Integer.parseInt(port);
	}
	private void set_alertport(String port) {
		this._alertport = Integer.parseInt(port);
	}
	public String getThread() {
		return _thread;
	}
}
