package org.mcupdater.downloadlib;

import java.io.IOException;
import java.util.Properties;

public class Version
{
	public static final int MAJOR_VERSION;
	public static final int MINOR_VERSION;
	public static final int BUILD_VERSION;
	public static final String BUILD_BRANCH;
	public static final String BUILD_LABEL;
	static {
		Properties prop = new Properties();
		try {
			prop.load(Version.class.getResourceAsStream("/version.properties"));
		} catch (IOException e) {
		}
		int major;
		int minor;
		int build;
		String branch;
		try {
			major = Integer.valueOf(prop.getProperty("major", "0"));
			minor = Integer.valueOf(prop.getProperty("minor", "0"));
			build = Integer.valueOf(prop.getProperty("build_version", "0"));
			branch = prop.getProperty("git_branch", "unknown");
		} catch (Exception e) {
			major = 1;
			minor = 3;
			build = 0;
			branch = "develop";
		}
		MAJOR_VERSION = major;
		MINOR_VERSION = minor;
		BUILD_VERSION = build;
		BUILD_BRANCH = branch;
		if( BUILD_BRANCH.equals("unknown") || BUILD_BRANCH.equals("master") ) {
			BUILD_LABEL = "";
		} else {
			BUILD_LABEL = " ("+BUILD_BRANCH+")";
		}
	}
	
	public static final String API_VERSION = MAJOR_VERSION + "." + MINOR_VERSION;
	public static final String VERSION = "v"+MAJOR_VERSION+"."+MINOR_VERSION+"."+BUILD_VERSION;
}
