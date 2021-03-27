package org.mcupdater.downloadlib;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.XZInputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

public class Downloadable {
	
	public enum HashAlgorithm {
		MD5,SHA
	}
	
	private final String friendlyName;
	private final String filename;
	private final HashAlgorithm algo;
	private final String hash;
	private long size;
	private final List<URL> downloadURLs;
	private final ProgressTracker tracker;
	
	public Downloadable(String friendlyName, String filename, String md5, long size, List<URL> downloadURLs) {
		this(friendlyName,filename,HashAlgorithm.MD5,md5,size,downloadURLs);
	}
	
	public Downloadable(String friendlyName, String filename, HashAlgorithm algo, String hash, long size, List<URL> downloadURLs) {
		this.friendlyName = friendlyName;
		this.filename = filename;
		this.algo = algo;
		this.hash = hash;
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

	public String getHash() {
		return hash;
	}

	public HashAlgorithm getHashAlgorithm() {
		return algo;
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
		String localHash = "-";
		File resolvedFile;
		
		if (basePath != null && (!basePath.isDirectory())) {
			basePath.mkdirs();			
		}
		resolvedFile = new File(basePath, this.filename);
		resolvedFile.getParentFile().mkdirs();
		//printMessage(resolvedFile.getAbsolutePath());
		if (resolvedFile.isFile()) {
			localHash = getHash(this.algo, resolvedFile);
			//printMessage(localMD5 + " - " + this.md5);
		}
		
		if (resolvedFile.isFile() && !resolvedFile.canWrite()) {
			printMessage("PANIC! Can't Write!");
			throw new RuntimeException("No write permissions for " + resolvedFile.toString() + "!");
		}
		if (nullOrEmpty(this.hash) && resolvedFile.isFile()) {
			printMessage("No hash and file exists - No download");
			this.tracker.setCurrent(1);
			this.tracker.setTotal(1);
			return;
		}
		if (localHash.equals(this.hash)) {
			printMessage("Hash matches - No download");
			this.tracker.setCurrent(1);
			this.tracker.setTotal(1);
			//TODO Log entry: No download necessary
			return;
		}
		if (!(cache == null) && !nullOrEmpty(this.hash)) {
			File cacheFile = new File(cache, this.hash.toLowerCase()+".bin");
			if (cacheFile.exists()) {
				if (!this.hash.equalsIgnoreCase(getHash(this.algo, cacheFile))) {
					printMessage("Cache file is invalid! Redownloading");
				} else {
					printMessage("Cache hit for hash");
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
		}
		for (URL downloadURL : downloadURLs) {
			try {
				URL localURL = downloadURL;
				if (localURL.getFile().toLowerCase().contains(".pack")) {
					resolvedFile = new File(resolvedFile.getAbsolutePath().concat(".pack"));
				}
				if (localURL.getFile().toLowerCase().contains(".lzma")) {
					resolvedFile = new File(resolvedFile.getAbsolutePath().concat(".lzma"));
				}
				if (localURL.getFile().toLowerCase().contains(".xz")) {
					resolvedFile = new File(resolvedFile.getAbsolutePath().concat(".xz"));
				}
				URLConnection conn = redirectAndConnect(localURL, null);
				if (conn.getContentLength() > 0) {
					this.tracker.setTotal(conn.getContentLength());
				}
				InputStream input = new TrackingInputStream(conn.getInputStream(), this.tracker);
				OutputStream output;
				File cacheFile = nullOrEmpty(this.hash) ? null : new File(cache, this.hash.toLowerCase() + ".bin");
				if (!(cache == null) && !nullOrEmpty(this.hash)) {
					output = new MirrorOutputStream(resolvedFile, cacheFile);
				} else {
					output = new FileOutputStream(resolvedFile);
				}
				IOUtils.copy(input, output);
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(output);
				localHash = getHash(this.algo, resolvedFile);
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
					if (changed) {
						localHash = getHash(this.algo, resolvedFile);
						FileUtils.copyFile(resolvedFile, new File(cache, localHash.toLowerCase() + ".bin"));
					}
				}
				if (nullOrEmpty(this.hash) || localHash.equals(this.hash)) {
					printMessage("Download finished");
					//TODO Log entry: Download successful
					return;
				} else {
					printMessage("Hash mismatch after download!");
					if (cacheFile.exists()) {
						cacheFile.delete();
					}
					return;  // Warn about MD5 mismatches, delete bad cache files, allow the download regardless.
				}
			} catch (IOException e) {
				//TODO Log warning: Error during connection
				printMessage(e.getMessage());
				StringWriter stack = new StringWriter();
				e.printStackTrace(new PrintWriter(stack));
				printMessage(stack.toString());
			} catch (Exception e) {
				printMessage("Something happened! - " + e.getMessage());
			}
		}
		printMessage("Unable to download");
		throw new RuntimeException("Unable to download (" + this.friendlyName + ") - All known URLs failed.");
	}
	
	public URLConnection redirectAndConnect(URL target, URL referer) throws IOException {
		if (target.getProtocol().equals("file")) {
			URLConnection conn = target.openConnection();
			conn.connect();
			return conn;
		}
		if (target.getHost().toLowerCase().equals("www.dropbox.com")) {
			if (!target.toString().toLowerCase().contains("dl=1")) {
				if (target.toString().contains("?")) {
					target = new URL(target.toString().concat("&dl=1"));
				} else {
					target = new URL(target.toString().concat("?dl=1"));
				}
			}
		}
		//System.out.println("BEFORE: " + target.toExternalForm());
		target = new URL(escapeURL(target.toString()));
		System.out.println(" AFTER: " + target.toExternalForm());
		HttpURLConnection conn = (HttpURLConnection) target.openConnection();
		conn.setRequestProperty("User-Agent",Version.USER_AGENT);
		if (tracker.getQueue() != null && tracker.getQueue().getMCUser() != null) {
			conn.setRequestProperty("MC-User", tracker.getQueue().getMCUser());
		}
		if (referer != null) {
			conn.setRequestProperty("Referer", referer.toString());
		}
		if (target.getUserInfo() != null) {
			String basicAuth = "Basic " + new String(new Base64().encode(target.getUserInfo().getBytes()));
			conn.setRequestProperty("Authorization", basicAuth);
		}
		if (target.getHost().equals("www.mediafire.com")) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				IOUtils.copy(conn.getInputStream(), baos);
				byte[] bytes = baos.toByteArray();
				Reader in = new InputStreamReader(new ByteArrayInputStream(bytes));
				char[] buffer = new char[819200];
				StringBuilder content = new StringBuilder();
				while (in.read(buffer) != -1) {
					content.append(buffer);
				}
				in.close();
				int key = content.toString().indexOf("\"", content.toString().indexOf("kNO"));
				int after = content.toString().indexOf("\"", key + 1);
				String newUrl = content.toString().substring(key + 1, after);
				System.out.println("Mediafire response - " + newUrl);
				if (newUrl.toLowerCase().startsWith("http")) {
					return redirectAndConnect(new URL(newUrl), target);
				}
			} catch (java.io.IOException e1) {
				e1.printStackTrace();
			}
		}
		conn.setUseCaches(false);
		conn.setInstanceFollowRedirects(false);
		/* Uncomment for header testing
		System.out.println(target);
		for(Map.Entry<String,List<String>> field : conn.getHeaderFields().entrySet()) {
			System.out.println(field.getKey() + ": " + field.getValue().toString());
		}
		*/
		if (conn.getResponseCode() / 100 == 3) {
			String newTarget = conn.getHeaderField("Location");
			if (newTarget.startsWith("//")) { //Handling of schemeless URLs - protocol comes from context.
				newTarget = target.getProtocol() + ":" + newTarget;
			}
			return redirectAndConnect(new URL(newTarget),target);
		}
		String contentType = conn.getContentType();
		if (contentType == null) {
			printMessage("No content type found!  Download server may have issues.");
		} else if (contentType.toLowerCase().startsWith("text/html")) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtils.copy(conn.getInputStream(), baos);
			byte[] bytes = baos.toByteArray();
			Reader in = new InputStreamReader(new ByteArrayInputStream(bytes));
			char[] buffer = new char[819200];
			StringBuilder content = new StringBuilder();
			while (in.read(buffer) != -1) {
				content.append(buffer);
			}
			in.close();
			if (target.getHost().equals("adf.ly") && !target.toString().startsWith("https://adf.ly/go.php")) {
				int key = content.toString().indexOf("'", content.toString().indexOf("ysmm"));
				int after = content.toString().indexOf("'", key+1);
				String raw = content.toString().substring(key+1, after);
				StringBuilder forward = new StringBuilder();
				StringBuilder backward = new StringBuilder();
				for (int i=0; i < raw.length(); i++) {
					if (i % 2 == 0) {
						forward.append(raw.charAt(i));
					} else {
						backward.insert(0, raw.charAt(i));
					}
				}
				String rebuilt = forward.toString() + backward.toString();
				byte[] decode = Base64.decodeBase64(rebuilt);
				byte[] decode2 = new byte[decode.length-2];
				System.arraycopy(decode, 2, decode2, 0, decode.length - 2);
				String newUrl = new String(decode2);
				System.out.println("Adf.ly response - " + newUrl);
				if (newUrl.toLowerCase().startsWith("http")) {
					return redirectAndConnect(new URL(newUrl), target);
				}
			}
			if (target.getHost().equals("optifine.net") && !target.toString().contains("downloadx")) {
				int key = content.toString().indexOf("downloadx");
				int after = content.toString().indexOf("'", key);
				String raw = content.toString().substring(key, after);
				String newUrl = target.getProtocol() + "://optifine.net/" + raw;
				return redirectAndConnect(new URL(newUrl), target);
			}
			//Check for META redirect
			if (content.toString().toLowerCase().replaceAll(" ", "").contains("http-equiv=\"refresh\"")) {
				int key = content.toString().toLowerCase().indexOf("http", content.toString().toLowerCase().indexOf("url", content.toString().toLowerCase().indexOf("http-equiv")));
				if (key != -1) {
					String strDelim = (content.toString().substring(content.toString().toLowerCase().indexOf("url", content.toString().toLowerCase().indexOf("http-equiv")), key).contains("'")) ? "'" : "\"";
					String newUrl = content.toString().substring(key, content.toString().indexOf(strDelim, key));
					return redirectAndConnect(new URL(newUrl), target);
				}
			}
			printMessage("File is in html format.  This may be an issue if an html file is not expected.");
		}
		conn.connect();
		return conn;
	}

	public String escapeURL(String source) {
		return source
				.replace(" ","%20")
				.replace("'","%27")
				.replace("[","%5B")
				.replace("]","%5D")
		;
	}

	private boolean nullOrEmpty(String input) {
		return input == null || input.isEmpty();
	}

	private void printMessage(String msg) {
		try {
			this.getTracker().getQueue().printMessage(this.filename + " - " + msg);
		} catch (NullPointerException npe) {
			System.out.println(this.filename + " - " + msg);
		}
	}

	@SuppressWarnings("deprecation")
	public static String getHash(HashAlgorithm algo, File file) throws IOException {
		byte[] hash;
		InputStream is = new FileInputStream(file);
		if (algo == HashAlgorithm.MD5){
			hash = DigestUtils.md5(is);
		} else if (algo == HashAlgorithm.SHA){
			hash = DigestUtils.sha(is);
		} else {
			hash = new byte[0];
		}
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
