package org.mcupdater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class DownloadQueue {

	private final Queue<Downloadable> processQueue = new ConcurrentLinkedQueue<Downloadable>();
	private final List<Downloadable> fullList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<Downloadable> failureList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<Downloadable> successList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<ProgressTracker> trackers = Collections.synchronizedList(new ArrayList<ProgressTracker>());
	private final TrackerListener listener;
	private final String name;
	private boolean inProgress;

	public DownloadQueue(String name, TrackerListener listener, Collection<Downloadable> queue) {
		this.name = name;
		this.listener = listener;
		if (queue != null) {
			addToQueue(queue);
		}
	}
	
	private void addToQueue(Collection<Downloadable> queue) {
		if (this.inProgress) {
			throw new IllegalStateException("Download queue already active");
		}
		this.fullList.addAll(queue);
		this.processQueue.addAll(queue);
		for (Downloadable entry : queue) {
			this.trackers.add(entry.getTracker());
			entry.getTracker().setTotal(100000L);
			entry.getTracker().setQueue(this);
		}
	}
	
	public void processQueue(ThreadPoolExecutor executor) {
		if (this.inProgress){
			throw new IllegalStateException("Queue is already in progress");
		}
		this.inProgress = true;
		
		if (this.fullList.isEmpty()) {
			//TODO Log entry: No files to download
			this.listener.onQueueFinished(this);
		} else {
			executor.submit(new Runnable(){
				@Override
				public void run() {
					DownloadQueue.this.iterateQueue();
				}
			});
		}
	}

	protected void iterateQueue() {
		// TODO Auto-generated method stub
		
	}

	public void updateProgress() {
		this.listener.onQueueProgress(this);
	}
	
	public float getProgress() {
		float current = 0.0F;
		float total = 0.0F;
		
		synchronized (this.trackers) {
			for (ProgressTracker tracker : this.trackers) {
				total += (float)tracker.getTotal();
				current += (float)tracker.getCurrent();
			}
		}
		
		float result = -1.0F;
		if (total > 0.0F) {
			result = current/total;
		}
		return result;
	}
}
