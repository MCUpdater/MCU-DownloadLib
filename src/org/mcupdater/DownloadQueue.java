package org.mcupdater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DownloadQueue {

	private final Queue<Downloadable> processQueue = new ConcurrentLinkedQueue<Downloadable>();
	private final List<Downloadable> fullList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<Downloadable> failureList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<Downloadable> successList = Collections.synchronizedList(new ArrayList<Downloadable>());
	private final List<ProgressTracker> trackers = Collections.synchronizedList(new ArrayList<ProgressTracker>());

	public void updateProgress() {
		// TODO Auto-generated method stub
		
	}
}
