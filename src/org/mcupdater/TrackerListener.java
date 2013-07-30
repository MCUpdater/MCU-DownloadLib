package org.mcupdater;

public abstract interface TrackerListener {
	public abstract void onQueueFinished(DownloadQueue queue);
	public abstract void onQueueProgress(DownloadQueue queue);
}
