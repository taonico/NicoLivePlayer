package jp.tao.nico.live;

public interface OnReceiveListener {
    public void onReceive(String received);
    public void setSenderID(int senderID);
    public int getSenderID();
}
