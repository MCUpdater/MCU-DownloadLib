package org.mcupdater.downloadlib;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class TaskableExecutor extends ThreadPoolExecutor {
	
	final Runnable after;
	private boolean taskCompleted = false;

	public TaskableExecutor(int threads, Runnable after){
		super(0,threads,0L,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		this.after = after;
		if (after == null) taskCompleted = true;
	}

	@Override
	public void afterExecute(Runnable task, Throwable thrown) {
		super.afterExecute(task, thrown);
		System.out.println("Checking for post-download action thread.");
		if (this.getQueue().size() == 0){
			System.out.println("Starting thread.");
			Thread taskThread = new Thread(after);
			taskThread.start();
			while (taskThread.isAlive()) {
				try {
					sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("DEBUG - afterExecute() completed");
			taskCompleted = true;
		}
	}

	public boolean isTaskCompleted() { return taskCompleted; }

	@Override
	public boolean isShutdown(){
		return super.isShutdown() && taskCompleted;
	}
	
}
