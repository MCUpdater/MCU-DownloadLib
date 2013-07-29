package org.mcupdater;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DownloadQueue {

	private final Queue<Downloadable> processQueue = new ConcurrentLinkedQueue<Downloadable>();

	public void update() {
		// TODO Auto-generated method stub
		
	}
}
