package client.skills;

import java.util.concurrent.ScheduledFuture;

import server.TimerManager;

public class BerserkState {
	private boolean isActive;
	private ScheduledFuture<?> future;
	
	public BerserkState() {
		this.isActive = false;
		this.future = null;
	}
	
	public void activate(Runnable action) {
		if (this.isActive) {
			throw new IllegalStateException("Berserk is already active for this instance.");
		}
		
		this.isActive = true;
		this.future = TimerManager.getInstance().register(action, 5000, 3000);
	}
	
	public void reset() {
		TimerManager.cancelSafely(this.future, false);
		this.future = null;
		this.isActive = false;
	}
}
