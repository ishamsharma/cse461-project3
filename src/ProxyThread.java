package src;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyThread implements Runnable {

	private final Socket socket;

	public ProxyThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			InputStream in = socket.getInputStream();
			BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
			OutputStream out = socket.getOutputStream();

			String data;
			String host = null;
			String hostLine = null;

			boolean isConnect = false;
			String answer = "";

			while (true) {
				data = buffer.readLine();

				if (data == null) {
					return;
				} else if (data.isEmpty()) {
					break;
				} else if (data.contains("CONNECT")) {
					isConnect = true;
				}

				if (isConnect || data.contains("GET") || data.contains("PUT") || data.contains("POST")) {
					SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					Date date = new Date();
					

					String[] words = data.split(" ");
					if(words[0].equals("GET") || words[0].equals("CONNECT") || words[0].equals("PUT") || words[0].equals("POST")) {
						System.out.println(formatter.format(date) + " - >>> " + data);
					}
					host = words[1];
					answer += words[0] + " " + host + " HTTP/1.0";

					if (!host.toLowerCase().contains("http")) {
						host = "http://" + host;
					}
				} else {
					String lower_data = data.toLowerCase();
					if (lower_data.contains("proxy-connection")) {
						answer += "Proxy-Connection: close";
					} else if (lower_data.contains("connection")) {
						answer += "Connection: close";
					} else {
						if (lower_data.contains("host")) {
							hostLine = data;
						}
						answer += data;
					}
				}
				answer += "\r\n";
			}

			int port = getPort(host, hostLine);
			// Is it possible to replace these two lines?
			InetAddress address = InetAddress.getByName(new URL(host).getHost());
			String addString = address.getHostAddress();

			if (isConnect) {
				sendConnect(addString, port, in, out);
			} else {
				sendNonConnect(addString, port, answer, out);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendNonConnect(String addString, int port, String answer, OutputStream clientOut)
			throws MalformedURLException, UnknownHostException {

		try {
			Socket proxSox = new Socket(addString, port);
			byte[] bytes = new byte[8027];
			PrintWriter out = new PrintWriter(proxSox.getOutputStream(), true);
			InputStream inStream = proxSox.getInputStream();
			out.println(answer);
			inStream.read(bytes);
			clientOut.write(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendConnect(String addString, int port, InputStream in, OutputStream out) throws Exception {

		try {
			Socket proxSox = new Socket(addString, port);
			out.write("HTTP 200 OK\r\n\r\n".getBytes());
			try {
				ServerThread servThread = new ServerThread(in, proxSox.getOutputStream());
				Thread clientThread = new Thread(servThread);
				clientThread.start();
				InputStream servInput = proxSox.getInputStream();
				int byteRead = servInput.read();
				while (byteRead != -1) {
					out.write(byteRead);
					byteRead = servInput.read();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			out.write("HTTP 502 Bad Gateway".getBytes());
			e.printStackTrace();
		}
	}

	private int getPort(String host, String hostLine) {
		int pos;
		try {
			if (hostLine != null) {
				String[] parts = hostLine.split(" ")[1].split(":");
				pos = parts.length - 1;
				if (Integer.parseInt(parts[pos]) != -1) {
					return Integer.parseInt(parts[pos]);
				}
			}
			if (host != null) {
				String[] parts2 = host.split(":");
				pos = parts2.length - 1;
				if (Integer.parseInt(parts2[pos]) != -1) {
					return Integer.parseInt(parts2[pos]);
				}
			}
		} catch (NumberFormatException e) {
		}
		if (host.toLowerCase().contains("https")) {
			return 443;
		} else {
			return 80;
		}
	}

}
