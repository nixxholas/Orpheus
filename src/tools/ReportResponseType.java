package tools;

public enum ReportResponseType {
	SUCCESS(0),
	UNKNOWN_USER(1),
	MAXIMUM_REACHED(2),
	REPORTED(3),
	GENERAL_FAILURE(4);
	
	private byte type;
	
	private ReportResponseType(int type) {
		this.type = (byte)type;
	}
	
	public byte asByte() {
		return this.type;
	}
}