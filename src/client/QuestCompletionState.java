package client;

public enum QuestCompletionState {
	
	UNDEFINED(-1), 
	NOT_STARTED(0), 
	STARTED(1), 
	COMPLETED(2);
	
	private final int id;

	private QuestCompletionState(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static QuestCompletionState getById(int id) {
		for (QuestCompletionState value : QuestCompletionState.values()) {
			if (value.id == id) {
				return value;
			}
		}
		return null;
	}
}