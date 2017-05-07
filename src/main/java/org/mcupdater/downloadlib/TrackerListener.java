package org.mcupdater.downloadlib;

public interface TrackerListener {
	void onQueueFinished(DownloadQueue queue);
	void onQueueProgress(DownloadQueue queue);
	void printMessage(String msg);
}
