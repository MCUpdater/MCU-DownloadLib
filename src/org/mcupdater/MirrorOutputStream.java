package org.mcupdater;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MirrorOutputStream extends OutputStream {

	FileOutputStream out1;
	FileOutputStream out2;
	
	public MirrorOutputStream(File file1, File file2) throws FileNotFoundException{
		this.out1 = new FileOutputStream(file1);
		this.out2 = new FileOutputStream(file2);
	}

	@Override
	public synchronized void write(int arg0) throws IOException {
		this.out1.write(arg0);
		this.out2.write(arg0);
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		this.out1.write(b);
		this.out2.write(b);
	}
	
	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		this.out1.write(b,off,len);
		this.out2.write(b,off,len);
	}
	
	@Override
	public void close() throws IOException {
		this.out1.close();
		this.out2.close();
	}
	
	@Override
	public void flush() throws IOException {
		this.out1.flush();
		this.out2.flush();
	}
}
