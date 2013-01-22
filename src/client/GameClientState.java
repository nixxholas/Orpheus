package client;

public enum GameClientState {
	LOGIN_NOTLOGGEDIN(0),
	LOGIN_SERVER_TRANSITION(1),
	LOGIN_LOGGEDIN(2);
	
	private final int code;
	
	private GameClientState(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;
	}
	
	public boolean is(GameClientState other) {
		return other != null && this.code == other.code;
	}
	
	public static GameClientState fromCode(int code) {
		for (GameClientState value : GameClientState.values()) {
			if (code == value.code) {
				return value;
			}
		}
		
		return null;
	}
}
