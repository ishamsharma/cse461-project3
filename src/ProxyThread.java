package src;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class ProxyThread implements Runnable {

	private static final String HTTP_END_LINE = "\r\n";
	private static final String EMPTY_LINE = "";
	private static final String CLOSE = "Connection: close";
	private static final String CONNECTION = "connection:";
	private static final String PROXY = "Proxy-connection:";
	private static final String CLOSE_PROXY = "Proxy-connection: close";
	private static final String HOST = "host:";
	private static final String HOST_CAPS = "Host: ";
	private Socket socket;

	public ProxyThread(Socket socket) {
		if (socket == null) {
			throw new IllegalArgumentException();
		}
		this.socket = socket;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			String host = "";
			int port = 80; // default for http, 443 is default for https

			// Http request: client --> server
			BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String curLine = request.readLine();

			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			StringBuffer buffer = new StringBuffer();
			// first traverse thru message
			for (int lines = 1; curLine != null && curLine.length() >= 0; lines++) {
				// print first line
				if (lines == 1) {
					System.out.println(curLine);
				}

				// trim leading/trailing whitespace, split
				curLine = curLine.trim();
				String[] parts = curLine.split(" ");
				String requestLine = "";
				// handle the header change to HTML/1.0, this is request line
				if (parts.length == 3 && parts[2].substring(0, 5).equals("HTTP/")) {
					curLine = parts[0] + parts [1] + parts[2].substring(0, 5) + "1.0";
					requestLine = parts[1];
					
					// check, turn off keepalive, replace anything that is a connection to clsoe
				} else if (parts[0].toLowerCase().equals(CONNECTION)) {
					curLine = CLOSE;
				}
				else if (parts[0].toLowerCase().equals(PROXY)) {
					curLine = CLOSE_PROXY;
				}else if (parts[0].toLowerCase().equals(HOST)) {
					host = "";
					// figure out the host to send to
					for (int i = 1; i < parts.length; i++)
						host += parts[i];

					// get port
					String[] hostParts = host.split(":");
					if (hostParts.length == 2) {
						port = Integer.valueOf(hostParts[1]).intValue();
					}else if(requestLine != null && requestLine.toLowerCase().contains("https://")) {
						port = 443;
					} // otherwise the port remains as 80, default which is already set

					curLine = HOST_CAPS + host; // now we have the host
				} 
				 

				// read next line
				appendHTTP(buffer, curLine);
				curLine = request.readLine();

			}
			appendHTTP(buffer, EMPTY_LINE);
			if (host != null) {
				// --> server, then server --> client
				request(buffer, output, host, port);
			}
			request.close();

		} catch (NumberFormatException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	// adds HTTP line info
	public void appendHTTP(StringBuffer output, String val) {
		if (output == null || val == null) {
			throw new IllegalArgumentException();
		}
		if (val.equals(EMPTY_LINE)) {
			output.append(HTTP_END_LINE);
		}else {
			output.append(val + HTTP_END_LINE);
		}
	}
	
	public void request(StringBuffer buff, DataOutputStream out, 
			  String host, int port) {
		try {
			// bind socket to the specified host and port
			Socket sock = new Socket(host, port);
			InputStream serverResponse = sock.getInputStream();
			
			
			
		}catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

}
