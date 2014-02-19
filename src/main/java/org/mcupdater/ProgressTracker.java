package org.mcupdater;

public class ProgressTracker {

	private long total;
	private long current;
	private DownloadQueue queue;

	public DownloadQueue getQueue() {
		return queue;
	}

	public void setQueue(DownloadQueue queue) {
		this.queue = queue;
	}

	public long getCurrent() {
		return current;
	}

	public void setCurrent(long current) {
		this.current = current;
		if (current > this.total) this.total = current;
		if (this.queue != null) this.queue.updateProgress(); 
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
		if (this.queue != null) this.queue.updateProgress(); 
	}

	public void addProgress(long amount) {
		setCurrent(getCurrent() + amount);
	}

	public float getProgress() {
		if (this.total == 0L) return 0.0F;
		return (float)this.current / (float)this.total;
	}
}
