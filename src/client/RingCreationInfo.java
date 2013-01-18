package client;

public class RingCreationInfo {
	public final int RingItemId;
	
	public final int FirstPartnerId;
	public final String FirstPartnerName;
	
	public final int SecondPartnerId;
	public final String SecondPartnerName;

	public RingCreationInfo(int ringItemId, int firstId, String firstName, int secondId, String secondName) {
		this.RingItemId = ringItemId;
		this.FirstPartnerId = firstId;
		this.FirstPartnerName = firstName;
		this.SecondPartnerId = secondId;
		this.SecondPartnerName = secondName;
	}
}