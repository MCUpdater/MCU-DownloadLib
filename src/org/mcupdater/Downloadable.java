package org.mcupdater;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.XZInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

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
	
	
	public void download(File basePath, File cache) throws IOException {
		printMessage("Started");
		String localMD5 = "-";
		File resolvedFile = null;
		
		if (basePath != null && (!basePath.isDirectory())) {
			basePath.mkdirs();			
		}
		resolvedFile = new File(basePath, this.filename);
		resolvedFile.getParentFile().mkdirs();
		//printMessage(resolvedFile.getAbsolutePath());
		if (resolvedFile.isFile()) {
			localMD5 = getMD5(resolvedFile);
			//printMessage(localMD5 + " - " + this.md5);
		}
		
		if (resolvedFile.isFile() && !resolvedFile.canWrite()) {
			printMessage("PANIC! Can't Write!");
			throw new RuntimeException("No write permissions for " + resolvedFile.toString() + "!");
		}
		if (nullOrEmpty(this.md5) && resolvedFile.isFile()) {
			printMessage("No MD5 and file exists - No download");
			this.tracker.setCurrent(1);
			this.tracker.setTotal(1);
			return;
		}
		if (localMD5.equals(this.md5)) {
			printMessage("MD5 matches - No download");
			this.tracker.setCurrent(1);
			this.tracker.setTotal(1);
			//TODO Log entry: No download necessary
			return;
		}
		if (!(cache == null) && !nullOrEmpty(this.md5)) {
			File cacheFile = new File(cache, this.md5.toLowerCase()+".bin");
			if (cacheFile.exists()) {
				printMessage("Cache hit for MD5");
				InputStream input = new TrackingInputStream(new FileInputStream(cacheFile), this.tracker);
				FileOutputStream output = new FileOutputStream(resolvedFile);
				IOUtils.copy(input, output);
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(output);
				while (resolvedFile.getName().toLowerCase().endsWith(".xz") || resolvedFile.getName().toLowerCase().endsWith(".lzma") || resolvedFile.getName().toLowerCase().endsWith(".pack")) {
					if (resolvedFile.getName().toLowerCase().endsWith(".xz")) {
						resolvedFile = extractXZ(resolvedFile);
						printMessage("Extracted: " + resolvedFile.getName());
					}
					if (resolvedFile.getName().toLowerCase().endsWith(".lzma")) {
						resolvedFile = extractLZMA(resolvedFile);
						printMessage("Extracted: " + resolvedFile.getName());
					}
					if (resolvedFile.getName().toLowerCase().endsWith(".pack")) {
						resolvedFile = unpack(resolvedFile);
						printMessage("Unpacked: " + resolvedFile.getName());
					}
				}
				return;
			}
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
				if (localURL.getFile().toLowerCase().contains(".pack")){
					resolvedFile = new File(resolvedFile.getAbsolutePath().concat(".pack"));
				}
				if (localURL.getFile().toLowerCase().contains(".lzma")){
					resolvedFile = new File(resolvedFile.getAbsolutePath().concat(".lzma"));
				}
				if (localURL.getFile().toLowerCase().contains(".xz")){
					resolvedFile = new File(resolvedFile.getAbsolutePath().concat(".xz"));
				}
				InputStream input = new TrackingInputStream(conn.getInputStream(), this.tracker);
				OutputStream output;
				File cacheFile = nullOrEmpty(this.md5) ? null : new File(cache, this.md5.toLowerCase()+".bin");
				if (!(cache == null) && !nullOrEmpty(this.md5)) {
					output = new MirrorOutputStream(resolvedFile, cacheFile);
				} else {
					output = new FileOutputStream(resolvedFile);
				}
				IOUtils.copy(input, output);
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(output);
				localMD5 = getMD5(resolvedFile);
				while (resolvedFile.getName().toLowerCase().endsWith(".xz") || resolvedFile.getName().toLowerCase().endsWith(".lzma") || resolvedFile.getName().toLowerCase().endsWith(".pack")) {
					boolean changed = false;
					if (resolvedFile.getName().toLowerCase().endsWith(".xz")) {
						resolvedFile = extractXZ(resolvedFile);
						changed = true;
						printMessage("Extracted: " + resolvedFile.getName());
					}
					if (resolvedFile.getName().toLowerCase().endsWith(".lzma")) {
						resolvedFile = extractLZMA(resolvedFile);
						changed = true;
						printMessage("Extracted: " + resolvedFile.getName());
					}
					if (resolvedFile.getName().toLowerCase().endsWith(".pack")) {
						resolvedFile = unpack(resolvedFile);
						changed = true;
						printMessage("Unpacked: " + resolvedFile.getName());
					}
					if (changed == true){
						localMD5 = getMD5(resolvedFile);
						FileUtils.copyFile(resolvedFile, new File(cache, localMD5.toLowerCase()+".bin"));
					}
				}
				if (nullOrEmpty(this.md5) || localMD5.equals(this.md5)) {
					printMessage("Download finished");
					//TODO Log entry: Download successful
					return;
				} else {
					printMessage("MD5 mismatch after download!");
					if (cacheFile.exists()) { cacheFile.delete(); }
					return;  // Warn about MD5 mismatches, delete bad cache files, allow the download regardless.
				}
			} catch (IOException e) {
				//TODO Log warning: Error during connection
				printMessage(e.getMessage());
			} catch (Exception e) {
				printMessage("Something happened! - " + e.getMessage());
			}
		}
		printMessage("Unable to download");
		throw new RuntimeException("Unable to download (" + this.friendlyName + ") - All known URLs failed.");
	}
	
	private boolean nullOrEmpty(String input) {
		if (input == null) {
			return true;
		} else if (input.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	private void printMessage(String msg) {
		this.getTracker().getQueue().printMessage(this.filename + " - " + msg);
	}

	public static String getMD5(File file) throws IOException {
		byte[] hash;
		InputStream is = new FileInputStream(file);
		hash = DigestUtils.md5(is);
		is.close();
		
		return new String(Hex.encodeHex(hash));		
	}

    public static File extractLZMA(File compressedFile) {
    	File unpacked = new File(compressedFile.getParentFile(), compressedFile.getName().replace(".lzma", "").replace(".LZMA", ""));
    	InputStream input = null;
    	OutputStream output = null;
    	try {
    		input = new LZMAInputStream(new FileInputStream(compressedFile));
    		output = new FileOutputStream(unpacked);
    		byte[] buf = new byte[65536];
    		
    		int read = input.read(buf);
    		while (read >= 1) {
    			output.write(buf,0,read);
    			read = input.read(buf);
    		}
    	} catch (Exception e) {
    		throw new RuntimeException("Unable to extract lzma: " + e.getMessage());
    	} finally {
    		IOUtils.closeQuietly(input);
    		IOUtils.closeQuietly(output);
    		compressedFile.delete();
    	}
    	return unpacked;
    }
    
    public static File extractXZ(File compressedFile) {
    	File unpacked = new File(compressedFile.getParentFile(), compressedFile.getName().replace(".xz", "").replace(".XZ", ""));
    	InputStream input = null;
    	OutputStream output = null;
    	try {
    		input = new XZInputStream(new FileInputStream(compressedFile));
    		output = new FileOutputStream(unpacked);
    		byte[] buf = new byte[65536];
    		
    		int read = input.read(buf);
    		while (read >= 1) {
    			output.write(buf,0,read);
    			read = input.read(buf);
    		}
    	} catch (Exception e) {
    		throw new RuntimeException("Unable to extract xz: " + e.getMessage());
    	} finally {
    		IOUtils.closeQuietly(input);
    		IOUtils.closeQuietly(output);
    		compressedFile.delete();
    	}
    	return unpacked;    	
    }
    
    public static File unpack(File compressedFile) {
    	File unpacked = new File(compressedFile.getParentFile(), compressedFile.getName().replace(".pack", "").replace(".PACK", ""));
    	JarOutputStream jarStream = null;
    	try {
    		jarStream = new JarOutputStream(new FileOutputStream(unpacked));
    		Pack200.newUnpacker().unpack(compressedFile, jarStream);
    	} catch (Exception e) {
    		throw new RuntimeException("Unable to unpack: " + e);
    	} finally {
    		IOUtils.closeQuietly(jarStream);
    		compressedFile.delete();
    	}
    	return unpacked;
    }
}
