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
		MAJOR_VERSION = Integer.valueOf(prop.getProperty("major","0"));
		MINOR_VERSION = Integer.valueOf(prop.getProperty("minor","0"));
		BUILD_VERSION = Integer.valueOf(prop.getProperty("build_version","0"));
		BUILD_BRANCH = prop.getProperty("git_branch","unknown");
		if( BUILD_BRANCH.equals("unknown") || BUILD_BRANCH.equals("master") ) {
			BUILD_LABEL = "";
		} else {
			BUILD_LABEL = " ("+BUILD_BRANCH+")";
		}
	}
	
	public static final String API_VERSION = MAJOR_VERSION + "." + MINOR_VERSION;
	public static final String VERSION = "v"+MAJOR_VERSION+"."+MINOR_VERSION+"."+BUILD_VERSION;
}
