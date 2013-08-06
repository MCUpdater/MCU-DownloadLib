package org.mcupdater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadQueue {

	private final Queue<Downloadable> processQueue = new ConcurrentLinkedQueue<Downloadable>();
	private final List<Downloadable> fullList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<Downloadable> failureList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<Downloadable> successList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<ProgressTracker> trackers = Collections.synchronizedList(new ArrayList<ProgressTracker>());
	private final TrackerListener listener;
	private final String name;
	private final AtomicInteger threadPoolRemain = new AtomicInteger();
	private final File basePath;
	private boolean active;

	public DownloadQueue(String name, TrackerListener listener, Collection<Downloadable> queue, File basePath) {
		this.name = name;
		this.listener = listener;
		if (queue != null) {
			addToQueue(queue);
		}
		this.basePath = basePath;
	}
	
	private void addToQueue(Collection<Downloadable> queue) {
		if (this.active) {
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
		if (this.active){
			throw new IllegalStateException("Queue is already in progress");
		}
		this.active = true;
		
		if (this.fullList.isEmpty()) {
			//TODO Log entry: No files to download
			this.listener.onQueueFinished(this);
		} else {
			int maxPool = executor.getMaximumPoolSize();
			this.threadPoolRemain.set(maxPool);
			this.listener.printMessage("Pool size: " + maxPool);
			for (int threadCount = 0; threadCount < maxPool; threadCount++) {
				executor.submit(new Runnable(){
					@Override
					public void run() {
						DownloadQueue.this.iterateQueue();
						DownloadQueue.this.listener.printMessage("Thread finished.");
					}
				});
			}
		}
	}

	protected void iterateQueue() {
		Downloadable entry;
		while ((entry = this.processQueue.poll()) != null) {
			this.listener.printMessage("Downloading: " + entry.getFriendlyName());
			try {
				entry.download(basePath);
				this.successList.add(entry);
				this.listener.printMessage("Download success");
				// TODO Log entry: download success
			} catch (IOException e) {
				this.listener.printMessage(entry.getFriendlyName() + " failed: " + e.getMessage());
				// TODO Log error: download failure
				this.failureList.add(entry);
			}
		}
		if (this.threadPoolRemain.decrementAndGet() <= 0){
			this.listener.onQueueFinished(this);
		}
		this.listener.printMessage("Remaining threads: " + this.threadPoolRemain.get());
	}

	public void updateProgress() {
		this.listener.onQueueProgress(this);
	}
	
	public boolean isActive() {
		return this.active;
	}
	
	public boolean isFinished() {
		return this.active && (this.processQueue.isEmpty()) && (this.threadPoolRemain.get() == 0);
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

	public String getName() {
		return name;
	}

	public void printMessage(String msg) {
		this.listener.printMessage(msg);
	}
}
