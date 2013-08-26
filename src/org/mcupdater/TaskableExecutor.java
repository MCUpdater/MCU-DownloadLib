package org.mcupdater;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskableExecutor extends ThreadPoolExecutor {
	
	Runnable after;
	
	public TaskableExecutor(int threads, Runnable after){
		super(0,threads,0L,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		this.after = after;
	}

	@Override
	public void afterExecute(Runnable task, Throwable thrown) {
		super.afterExecute(task, thrown);
		if (this.getQueue().size() == 0){
			new Thread(after).start();
		}
	}
	
	
}
