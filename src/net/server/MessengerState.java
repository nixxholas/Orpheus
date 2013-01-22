package net.server;


public class MessengerState {
	
	private int id;
	private int position;
	
	private Messenger messenger = null;
	
	public MessengerState() {
		this.id = 0;
		this.position = 4;
	}
	
	public int getId() {
		if (this.messenger == null) {
			throw new IllegalStateException("This messenger state instance is not active.");
		}
		
		return this.id;
	}
	
	public int getPosition() {
		if (this.messenger == null) {
			throw new IllegalStateException("This messenger state instance is not active.");
		}
		
		return this.position;
	}
	
	public boolean hasCapacity() {
		if (this.messenger == null) {
			throw new IllegalStateException("This messenger state instance is not active.");
		}
		
		int size = this.messenger.getMembers().size();
		return size < 3;
	}
	
	public void accept(Messenger messenger, int position) {
		this.messenger = messenger;
		
		this.id = messenger.getId();
		this.position = position;
	}
	
	public void reset() {
		this.messenger = null;
		
		this.id = 0;
		this.position = 4;
	}
	
	public boolean isActive() {
		return this.messenger != null;
	}
}
