package jp.tao.nico.live;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicommunityData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4080468317896146114L;
	public String CommunityID;
	public String CommunityName;
	public String CommunityOwner;

	public NicommunityData(String communityID, String communityName, String communityOwner) {
		CommunityID = communityID;
		CommunityName = communityName;
		CommunityOwner = communityOwner;
	}
	
	@Override
	public String toString(){
		return String.format("%1$s : %2$s : %3$s", CommunityID, CommunityName, CommunityOwner);
	}

	public static class NicommunityDataList extends ArrayList<Map.Entry> {
		public NicommunityDataList(HashMap<String, NicommunityData> nicommunityData){
			super(nicommunityData.entrySet());
		}
		public List<Map.Entry> sort(){
			Collections.sort(this, new ComparatorList());
			return this;
		}
	}
	
	private static class ComparatorList implements Comparator<Map.Entry> {
		private final Pattern _nicoLiveComuPattern = Pattern.compile(".*co([0-9]+).*");
		private final Pattern _nicoLiveChannelPattern = Pattern.compile(".*ch([0-9]+).*");
		private final Integer MAX_CHANNEL_NUMBER = 100000;
		
		public int compare(Entry lhs, Entry rhs) {
			Integer e1 = getNumber(lhs);
			Integer e2 = getNumber(rhs);
			if (e1==-1 || e2==-1){
				return lhs.getKey().toString().compareTo(rhs.getKey().toString());
			}
			return e1.compareTo(e2);
		}
		private Integer getNumber(Map.Entry entry){
			Matcher matcher = _nicoLiveChannelPattern.matcher(entry.getKey().toString());
			if(matcher.matches()){
				return Integer.valueOf(matcher.group(1));
			}
			matcher = _nicoLiveComuPattern.matcher(entry.getKey().toString());
			if(matcher.matches()){
				return Integer.valueOf(matcher.group(1)) + MAX_CHANNEL_NUMBER;
			}
			
			return -1;
		}
	}
}
