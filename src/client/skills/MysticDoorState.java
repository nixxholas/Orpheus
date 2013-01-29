package client.skills;

import java.util.ArrayList;
import java.util.List;

import server.TimerManager;
import server.maps.Door;

public class MysticDoorState {
	private List<Door> doors;
	private boolean canDoor;
	
	public MysticDoorState() {
		this.doors = new ArrayList<Door>();
		this.canDoor = true;
	}
	
	public void addDoor(Door door) {
		this.doors.add(door);
	}

	public boolean canDoor() {
		return this.canDoor;
	}
	
	public void clearDoors() {
		this.doors.clear();
	}

	public List<Door> getDoors() {
		return new ArrayList<Door>(this.doors);
	}
	
	public void disableDoor() {
		this.canDoor = false;
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				MysticDoorState.this.canDoor = true;
			}
		}, 5000);
	}
}
