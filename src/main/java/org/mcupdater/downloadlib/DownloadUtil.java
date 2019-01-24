package org.mcupdater.downloadlib;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUtil {
	public static boolean get(final URL url, final File dest) {
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", Version.USER_AGENT);
			conn.connect();
			FileUtils.copyInputStreamToFile(conn.getInputStream(), dest);
			return true;
		} catch( IOException e ) {
			return false;
		}
	}

	public static File getToTemp(final URL url, final String prefix, final String suffix) {
		try {
			File tmp = File.createTempFile(prefix, suffix);
			tmp.deleteOnExit();
			if( DownloadUtil.get(url, tmp) ) {
				return tmp;
			} else {
				return null;
			}
		} catch( IOException e ) {
			return null;
		}
	}
}
