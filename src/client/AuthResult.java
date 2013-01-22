package client;

public enum AuthResult {
	SUCCESS(0),
	DELETED_OR_BLOCKED(3),
	INCORRECT_PASSWORD(4),
	NOT_REGISTERED(5),
	ALREADY_LOGGED_IN(7),
	SYSTEM_ERROR(9),
	TOO_MANY_CONNECTIONS(10),
	RESTRICTED_LOGIN(13),
	TERMS_OF_SERVICE(23);
	
	private final int code;
	
	private AuthResult(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;
	}
	
	public boolean is(AuthResult other) {
		return other != null && this.code == other.code;
	}
	
	/* Possible values for <code>reason</code>:<br>
	 * 3: ID deleted or blocked<br>
	 * 4: Incorrect password<br>
	 * 5: Not a registered id<br>
	 * 6: System error<br>
	 * 7: Already logged in<br>
	 * 8: System error<br>
	 * 9: System error<br>
	 * 10: Cannot process so many connections<br>
	 * 11: Only users older than 20 can use this channel<br>
	 * 13: Unable to log on as master at this ip<br>
	 * 14: Wrong gateway or personal info and weird korean button<br>
	 * 15: Processing request with that korean button!<br>
	 * 16: Please verify your account through email...<br>
	 * 17: Wrong gateway or personal info<br>
	 * 21: Please verify your account through email...<br>
	 * 23: License agreement<br>
	 * 25: Maple Europe notice =[ FUCK YOU NEXON<br>
	 * 27: Some weird full client notice, probably for trial versions<br>
	 */
}
