package client.skills;

import java.util.concurrent.ScheduledFuture;

import server.TimerManager;

public class BeholderState {
	private boolean isAuraActive, isHexActive;
	private ScheduledFuture<?> aura, hex;

	public BeholderState() {
		this.aura = null;
		this.isAuraActive = false;

		this.hex = null;
		this.isHexActive = false;
	}
	
	public void activateAura(Runnable action, int interval) {
		if (this.isAuraActive) {
			throw new IllegalStateException("Aura of Beholder is already active for this instance.");
		}
		
		if (action != null) {
			this.aura = TimerManager.getInstance().register(action, interval, interval);
		}
	}
	
	public void activateHex(Runnable action, int interval) {
		if (this.isHexActive) {
			throw new IllegalStateException("Hex of Beholder is already active for this instance.");
		}
		
		if (action != null) {
			this.hex = TimerManager.getInstance().register(action, interval, interval);
		}
	}
	
	public void reset() {
		TimerManager.cancelSafely(this.aura, false);
		this.aura = null;
		this.isAuraActive = false;
		
		TimerManager.cancelSafely(this.hex, false);
		this.hex = null;
		this.isHexActive = false;		
	}
}
