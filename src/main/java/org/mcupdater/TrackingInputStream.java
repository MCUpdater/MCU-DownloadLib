package org.mcupdater;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TrackingInputStream extends FilterInputStream {

	private final ProgressTracker tracker;

	protected TrackingInputStream(InputStream in, ProgressTracker tracker) {
		super(in);
		this.tracker = tracker;
	}

	  public int read() throws IOException
	  {
	    int result = this.in.read();

	    if (result >= 0) {
	      this.tracker.addProgress(1L);
	    }

	    return result;
	  }

	  public int read(byte[] buffer) throws IOException
	  {
	    int size = this.in.read(buffer);

	    if (size >= 0) {
	      this.tracker.addProgress(size);
	    }

	    return size;
	  }

	  public int read(byte[] buffer, int off, int len) throws IOException
	  {
	    int size = this.in.read(buffer, off, len);

	    if (size > 0) {
	      this.tracker.addProgress(size);
	    }

	    return size;
	  }

	  public long skip(long size) throws IOException
	  {
	    long skipped = super.skip(size);

	    if (skipped > 0L) {
	      this.tracker.addProgress(skipped);
	    }

	    return skipped;
	  }
}
