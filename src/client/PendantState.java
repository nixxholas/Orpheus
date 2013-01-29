package client;

import java.util.concurrent.ScheduledFuture;

import server.TimerManager;

public class PendantState {
	private boolean isActive;

	private ScheduledFuture<?> future; 
	private int multiplier;

	public PendantState() {
		this.isActive = false;

		this.future = null;
		this.multiplier = 0;
	}

	public int getMultiplier() {
		return multiplier;
	}

	public void activate(final Runnable action) {
		if (this.isActive) {
			throw new IllegalStateException("This pendant of spirit state instance is already active.");
		}
		
		// 1h = 3600000ms
		this.future = TimerManager.getInstance().register(new Runnable() {
			@Override
			public void run() {
				if (multiplier < 3) {
					multiplier++;
					action.run();
				} else {
					reset();
				}
			}
		}, 3600000); 
		
		this.isActive = true;
	}

	public void reset() {
		TimerManager.cancelSafely(this.future, false);

		this.isActive = false;
		this.future = null;
		this.multiplier = 0;
	}
}
