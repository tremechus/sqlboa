package sqlboa.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOUtil {
	private static final int IO_BUFFER_SIZE = 8 * 1024;

    public static void write(Path path, String string) throws IOException {
    	PrintWriter out = null;
    	try {
    		out = new PrintWriter(Files.newOutputStream(path));
    		out.write(string);
    	} finally {
    		if (out != null) {
    			out.close();
    		}
    	}
    }
    
	public static String readAsString(Path path) throws IOException {
		InputStream in = null;
		try {
			in = Files.newInputStream(path);
			return new String(read(in));
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
	
	public static byte[] read(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		copy(in, out);
		out.close();
		
		return out.toByteArray();
	}
	
	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}
	
}
