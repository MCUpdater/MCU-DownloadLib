package org.mcupdater.downloadlib;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadQueue {

	private final Queue<Downloadable> processQueue = new ConcurrentLinkedQueue<>();
	private final List<Downloadable> fullList = Collections.synchronizedList(new ArrayList<>());
	private final List<Downloadable> failureList = Collections.synchronizedList(new ArrayList<>());
	private final List<Downloadable> successList = Collections.synchronizedList(new ArrayList<>());
	private final List<ProgressTracker> trackers = Collections.synchronizedList(new ArrayList<>());
	private final TrackerListener listener;
	private final String name;
	private final AtomicInteger threadPoolRemain = new AtomicInteger();
	private final File basePath;
	private final File cachePath;
	private final String parent;
	private final Logger logger;
	private String mcUser = null;
	private TaskableExecutor executor = null;
	private DownloadQueue self;
	private QueueStatus status;

	public DownloadQueue(String name, String parent, TrackerListener listener, Collection<Downloadable> queue, File basePath, File cachePath, String mcUser, Logger logger) {
		this(name,parent,listener,queue,basePath,cachePath, logger);
		this.mcUser  = mcUser;
		this.self = this;
	}
	public DownloadQueue(String name, String parent, TrackerListener listener, Collection<Downloadable> queue, File basePath, File cachePath, Logger logger) {
		this.name = name;
		this.parent = parent;
		this.listener = listener;
		this.status = QueueStatus.NOT_STARTED;
		if (queue != null) {
			addToQueue(queue);
		}
		this.basePath = basePath;
		this.cachePath = cachePath;
		this.logger = logger;
		this.self = this;
	}
	
	private void addToQueue(Collection<Downloadable> queue) {
		if (this.status != QueueStatus.NOT_STARTED) {
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

	protected void iterateQueue() {
		Downloadable entry;
		while ((entry = this.processQueue.poll()) != null) {
			this.listener.printMessage("Downloading: " + entry.getFriendlyName());
			try {
				entry.download(basePath, cachePath);
				synchronized (this.successList) {
					this.successList.add(entry);					
				}
				//this.listener.printMessage("Download success");
			} catch (Exception e) {
				this.listener.printMessage(entry.getFriendlyName() + " failed: " + e.getMessage());
				this.failureList.add(entry);
			}
		}
/*
		if (this.threadPoolRemain.decrementAndGet() <= 0){
			System.out.println("DownloadQueue - No more threads");
			this.listener.onQueueFinished(this);
		}
 */
		//this.listener.printMessage(this.parent + " - " + this.name + " - Remaining threads: " + this.threadPoolRemain.get());
	}

	public void updateProgress() {
		this.listener.onQueueProgress(this);
	}

	@Deprecated
	public boolean isFinished() {
		return (this.status == QueueStatus.FINISHED);
		//return (this.processQueue.isEmpty()) && (this.threadPoolRemain.get() == 0) && this.executor.isShutdown();
	}

	public QueueStatus getStatus() {
		return this.status;
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
		if (this.status == QueueStatus.POSTPROCESSING || this.status == QueueStatus.FINISHED) {
			return 1.0F;
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
	
	public List<Downloadable> getFailures() {
		return this.failureList;
	}
	
	public List<Downloadable> getSuccesses() {
		return this.successList;
	}
	
	public int getTotalFileCount(){
		synchronized(this.fullList) {
			return this.fullList.size();
		}
	}
	
	public int getSuccessFileCount(){
		synchronized(this.successList) {
			return this.successList.size();
		}
	}
	
	public int getFailedFileCount(){
		synchronized(this.failureList) {
			return this.failureList.size();
		}
	}

	public File getCachePath() {
		return cachePath;
	}

	public String getParent() {
		return parent;
	}

	public String getMCUser() {
		return mcUser;
	}

	@Deprecated
	public void processQueue(ThreadPoolExecutor tpExecutor) {
		if (this.status != QueueStatus.NOT_STARTED){
			throw new IllegalStateException("Queue is already in progress");
		}
		this.status = QueueStatus.DOWNLOADING;
		if (tpExecutor instanceof TaskableExecutor) {
			this.executor = (TaskableExecutor) tpExecutor;
		}

		if (this.fullList.isEmpty()) {
			printMessage(parent + " - " + name + " - No files in queue");
			this.threadPoolRemain.set(1);
			//DownloadQueue.this.listener.printMessage(parent + " - " + name + " - Thread finished.");
			tpExecutor.submit(DownloadQueue.this::iterateQueue);
		} else {
			int maxPool = tpExecutor.getMaximumPoolSize();
			this.threadPoolRemain.set(maxPool);
			//this.listener.printMessage("Pool size: " + maxPool);
			for (int threadCount = 0; threadCount < maxPool; threadCount++) {
				//DownloadQueue.this.listener.printMessage(parent + " - " + name + " - Thread finished.");
				tpExecutor.submit(DownloadQueue.this::iterateQueue);
			}
		}
		if (executor != null) {
			Thread postMonitor = new Thread(() -> {
				System.out.println("DownloadQueue - postMonitor start - " + name);
				int count = 0;
				while (!executor.isTaskCompleted()) {
					try {
						if (executor.isDownloadComplete()) {
							this.status = QueueStatus.POSTPROCESSING;
						}
						Thread.sleep(10);
						count++;
						if (count >= 1000) {
							System.out.println("Waiting - " + name);
							count = 0;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("DownloadQueue - Finished - " + name);
				this.status = QueueStatus.FINISHED;
				listener.onQueueFinished(self);
			});
			postMonitor.start();
		} else {
			this.status = QueueStatus.FINISHED;
			listener.onQueueFinished(self);
		}
	}

	public void processQueue(int threads, Runnable postProcessingThread) {
		ThreadPoolExecutor tpe = new ThreadPoolExecutor(threads, threads,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		if (this.status != QueueStatus.NOT_STARTED){
			throw new IllegalStateException("Queue is already in progress");
		}
		this.status = QueueStatus.DOWNLOADING;
		tpe.submit(DownloadQueue.this::iterateQueue);
		tpe.shutdown();
		Thread monitor = new Thread(() -> {
			try {
				while (!tpe.awaitTermination(30, TimeUnit.SECONDS)) {
					logger.finer(String.format("%s: Waiting for queue to finish", this.name));
				}
			} catch (InterruptedException e) {
				logger.log(Level.WARNING, String.format("%s: Download queue interrupted!", this.name), e);
				return;
			}
			this.status = QueueStatus.POSTPROCESSING;
			listener.onQueueProgress(self);
			postProcessingThread.run();
			this.status = QueueStatus.FINISHED;
			listener.onQueueFinished(self);
		});
		monitor.start();
	}
}
