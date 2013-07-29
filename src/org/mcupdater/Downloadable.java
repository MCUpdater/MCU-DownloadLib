package org.mcupdater;

import java.net.URL;
import java.util.List;

public class Downloadable {
	private String friendlyName;
	private String filename;
	private String md5;
	private List<URL> downloadURLs;
	
	public Downloadable(String friendlyName, String filename, String md5, List<URL> downloadURLs) {
		setFriendlyName(friendlyName);
		setFilename(filename);
		setMD5(md5);
		this.downloadURLs = downloadURLs; 
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getMD5() {
		return md5;
	}

	public void setMD5(String md5) {
		this.md5 = md5;
	}
	
	public List<URL> getURLs() {
		return this.downloadURLs;
	}
}
