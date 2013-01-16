/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss <aaron@deviant-core.net>
    				Patrick Huy <patrick.huy@frz.cc>
					Matthias Butz <matze@odinms.de>
					Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class TimerManager implements TimerManagerMBean {
	private static TimerManager instance = new TimerManager();
	private ScheduledThreadPoolExecutor executor;

	private TimerManager() {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			mBeanServer.registerMBean(this, new ObjectName("server:type=TimerManger"));
		} catch (Exception e) {
		}
	}

	public static TimerManager getInstance() {
		return instance;
	}

	public void start() {
		if (executor != null && !executor.isShutdown() && !executor.isTerminated()) {
			return;
		}
		ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
			private final AtomicInteger threadNumber = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("TimerManager-Worker-" + threadNumber.getAndIncrement());
				return t;
			}
		});
		// this is a no-no, it actually does nothing..then why the fuck are you
		// doing it?
		stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		executor = stpe;
	}

	public void stop() {
		executor.shutdownNow();
	}

	public Runnable purge() {
		// Yay?
		return new Runnable() {
			
			@Override
			public void run() {
				executor.purge();
			}
		};
	}

	public ScheduledFuture<?> register(Runnable r, long repeatTime, long delay) {
		return executor.scheduleAtFixedRate(new LoggingSaveRunnable(r), delay, repeatTime, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> register(Runnable r, long repeatTime) {
		return executor.scheduleAtFixedRate(new LoggingSaveRunnable(r), 0, repeatTime, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> schedule(Runnable r, long delay) {
		return executor.schedule(new LoggingSaveRunnable(r), delay, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> scheduleAtTimestamp(Runnable r, long timestamp) {
		return schedule(r, timestamp - System.currentTimeMillis());
	}
	
	@Override
	public long getActiveCount() {
		return executor.getActiveCount();
	}

	@Override
	public long getCompletedTaskCount() {
		return executor.getCompletedTaskCount();
	}

	@Override
	public int getQueuedTasks() {
		return executor.getQueue().toArray().length;
	}

	@Override
	public long getTaskCount() {
		return executor.getTaskCount();
	}

	@Override
	public boolean isShutdown() {
		return executor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return executor.isTerminated();
	}

	private static class LoggingSaveRunnable implements Runnable {
		Runnable r;

		public LoggingSaveRunnable(Runnable r) {
			this.r = r;
		}

		@Override
		public void run() {
			try {
				r.run();
			} catch (Throwable t) {
			}
		}
	}
	
	public static void cancelSafely(final ScheduledFuture<?> future, final boolean arg0) {
		final ScheduledFuture<?> copy = future;
		if (copy != null) {
			copy.cancel(arg0);
		}
	}
}
