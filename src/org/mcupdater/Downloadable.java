package org.mcupdater;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

public class Downloadable {
	private final String friendlyName;
	private final String filename;
	private final String md5;
	private long size;
	private final List<URL> downloadURLs;
	private final ProgressTracker tracker;
	
	public Downloadable(String friendlyName, String filename, String md5, long size, List<URL> downloadURLs) {
		this.friendlyName = friendlyName;
		this.filename = filename;
		this.md5 = md5;
		this.size = size;
		this.downloadURLs = downloadURLs;
		this.tracker = new ProgressTracker();
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public String getFilename() {
		return filename;
	}

	public String getMD5() {
		return md5;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public List<URL> getURLs() {
		return this.downloadURLs;
	}
	
	public ProgressTracker getTracker() {
		return this.tracker;
	}
	
	public void download(File basePath) throws IOException {
		String localMD5 = "";
		File resolvedFile = null;
		
		if (basePath != null && (!basePath.isDirectory())) {
			basePath.mkdirs();			
		}
		resolvedFile = new File(basePath, this.filename);
		printMessage(resolvedFile.getAbsolutePath());
		if (resolvedFile.isFile()) {
			localMD5 = getMD5(resolvedFile);
			printMessage(localMD5 + " - " + this.md5);
		}
		
		if (resolvedFile.isFile() && !resolvedFile.canWrite()) {
			throw new RuntimeException("No write permissions for " + resolvedFile.toString() + "!");
		}
		if (localMD5.equals(this.md5)) {
			printMessage("MD5 matches - No download");
			this.tracker.setCurrent(1);
			this.tracker.setTotal(1);
			//TODO Log entry: No download necessary
			return;
		}
		Iterator<URL> iteratorURL = downloadURLs.iterator();
		while (iteratorURL.hasNext()){
		try {
				URL localURL = iteratorURL.next();
				URLConnection conn = localURL.openConnection();
				conn.connect();
				if (conn.getContentLength() > 0) {
					this.tracker.setTotal(conn.getContentLength());
				}
				InputStream input = new TrackingInputStream(conn.getInputStream(), this.tracker);
				FileOutputStream output = new FileOutputStream(resolvedFile);
				IOUtils.copy(input, output);
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(output);
				localMD5 = getMD5(resolvedFile);
				if (localMD5.equals(this.md5)) {
					printMessage("Download finished");
					//TODO Log entry: Download successful
					return;
				}				
			} catch (IOException e) {
				//TODO Log warning: Error during connection
			}
		}
		throw new RuntimeException("Unable to download (" + this.friendlyName + ") - All known URLs failed.");
	}
	
	private void printMessage(String msg) {
		this.getTracker().getQueue().printMessage(msg);
	}

	public static String getMD5(File file) throws IOException {
		byte[] hash;
		InputStream is = new FileInputStream(file);
		hash = DigestUtils.md5(is);
		is.close();
		
		return new String(Hex.encodeHex(hash));		
	}
}
