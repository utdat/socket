

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class JavaHTTPServer implements Runnable {

	static final File WEB_ROOT = new File(".");
	static final String LOGIN_FILE = "login.html";
	static final String LOGIN_UNSUCCESS = "error404.html";
	static final String LOGIN_SUCCESS = "member.html";

	static final int PORT = 8080;

	static final boolean verbose = true;
	boolean check = true;

	private Socket connect;

	public JavaHTTPServer(Socket c) {
		connect = c;
	}

	public static String convertStreamToString(BufferedReader in) {
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while (!((line = in.readLine()).isEmpty())) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());

				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}

				Thread thread = new Thread(myServer);
				thread.start();
			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		String fileRequested = null;

		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));

			out = new PrintWriter(connect.getOutputStream());

			dataOut = new BufferedOutputStream(connect.getOutputStream());

			String input = in.readLine();

			StringTokenizer parse = new StringTokenizer(input);

			String method = parse.nextToken().toUpperCase();

			fileRequested = parse.nextToken().toLowerCase();

			if (method.equals("POST")) {
				String user = "";
				String pass = "";
				String str = "";
				
				StringBuilder payload = new StringBuilder();
				while (in.ready()) {
					payload.append((char) in.read());
				}
			
				str = payload.toString();
				String temp = str.split("username=")[1];
				
				if (temp.charAt(0) != '&' && !temp.endsWith("=")) {
					str = str.split("username=")[1];
				
					user = str.split("&")[0]; 

					pass = str.split("&")[1]; 
					pass = pass.split("=")[1];
				}
				if (user.equals("admin") && pass.equals("admin")) {
					fileRequested = "/" + LOGIN_SUCCESS;
				} else {
					fileRequested = "/" + LOGIN_UNSUCCESS;
				}

				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);

				byte[] fileData = readFileData(file, fileLength);

				out.println("HTTP/1.1 200 OK");
				out.println("Server: Java HTTP Server from SSaurel : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + content);
				out.println("Content-length: " + fileLength);
				out.println(); 
				out.flush(); 

				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				check = true;
			} else if (method.equals("GET")) {
				System.out.println(fileRequested);
				if (fileRequested.endsWith("/")) {
					fileRequested += LOGIN_FILE;
				}

				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);

				byte[] fileData = readFileData(file, fileLength);

				out.println("HTTP/1.1 200 OK");
				out.println("Server: Java HTTP Server from SSaurel : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + content);
				out.println("Content-length: " + fileLength);
				out.println();
				out.flush();

				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			} else {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}

				File file = new File(WEB_ROOT, LOGIN_SUCCESS);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";

				byte[] fileData = readFileData(file, fileLength);

				out.println("HTTP/1.1 501 Not Implemented");
				out.println("Server: Java HTTP Server from SSaurel : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println();
				out.flush();

				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			}

		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}

		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);

		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close();
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}

			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
	}

	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return fileData;
	}

	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
			return "text/html";
		else
			return "text/plain";
	}

	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, LOGIN_UNSUCCESS);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println();
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();

		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}

}