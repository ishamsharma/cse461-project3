package src;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyThread implements Runnable {

	private static final String CLOSE = "Connection: close";
	private static final String CLOSE_PROXY = "Proxy-connection: close";
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
			BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = request.readLine();
			String messg = "";

			// read the message that arrives until it is completed
			while (line != null && line.length() > 1) {
				System.err.println(line);
				messg += line + "\n";
				line = request.readLine();
			}
			if (messg.contains("CONNECT")) {
				prepareHeader(messg, true);

			} else {
				prepareHeader(messg, false);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void prepareHeader(String messg, boolean isConnect) {
		String host = "";
		int port = -1;

		if (!isConnect) {
			// if not a connection, we want to replace the message
			messg = messg.replace("Connection: keep-alive", CLOSE);
			messg = messg.replace("Proxy-connection: keep-alive", CLOSE_PROXY);
			messg = messg.replace("HTTP/1.1", "HTTP/1.0");
		}

		messg = messg.replace("\r\n", "\n");
		String[] messgParts = messg.split("\n");
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date();
		System.out.println(formatter.format(date) + " - >>> " + messgParts[0]);

		// set host
		for (int i = 0; i < messgParts.length; i++) {
			// get first string "word", see if it is host
			if (messgParts[i].split(" ")[0].toLowerCase().equals("host:")) {
				String hostPort = messgParts[i].split(" ")[1];
				String[] hostParts = hostPort.split(":");
				host = hostParts[0];
				if (hostParts.length > 1) {
					port = Integer.parseInt(hostParts[1]);
				}

			}
		}

		// set port
		if (port == -1) {
			if (messg.contains("https://")) {
				port = 443;
			} else {
				port = 80;
			}
		}

		String newRequest = messg.replace(host, "");
		newRequest = newRequest.replace("http://", "");
		newRequest = newRequest.replace("https://", "");
		InetAddress fullHostname = null;
		try {
			fullHostname = InetAddress.getAllByName(host)[0];
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Socket sock = null;
		try {
			sock = new Socket(fullHostname, port);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!isConnect) {
			try {
				System.err.println("not connect");
				OutputStreamWriter writer = (new OutputStreamWriter(sock.getOutputStream()));
				writer.write(newRequest);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			sendNonConnect(sock);
		} else {
			try {

				System.err.println("connect");
				OutputStreamWriter writer = (new OutputStreamWriter(socket.getOutputStream()));
				writer.write("HTTP 200 OK");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			sendConnect(sock);
		}
	}

	public void sendConnect(Socket s) {
		System.err.println("in here!");
		while (true) {
			boolean flag1 = false;
			boolean flag2 = false;
			while (true) {
				try {
					DataInputStream input = new DataInputStream(socket.getInputStream());
					byte[] receive = new byte[8072];
					input.read(receive);

					if (receive.length == 0) {
						break;
					}
					DataOutputStream output = new DataOutputStream(s.getOutputStream());
					output.write(receive);
					flag1 = true;

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// now, try receive from server and send bak to client
			while (true) {
				try {
					DataInputStream input = new DataInputStream(s.getInputStream());
					byte[] receive = new byte[8072];
					input.read(receive);

					if (receive.length == 0) {
						break;
					}
					DataOutputStream output = new DataOutputStream(s.getOutputStream());
					output.write(receive);
					flag2 = true;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (!flag1 && !flag2) {
				try {
					s.close();
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				break;
			}
		}

	}

	public void sendNonConnect(Socket s) {

		while (true) {
			try {
				// receive from client
				DataInputStream input = new DataInputStream(socket.getInputStream());
				byte[] receive = new byte[8072];
				input.read(receive);

				if (receive.length == 0) {
					break;
				}
				DataOutputStream output = new DataOutputStream(s.getOutputStream());
				output.write(receive);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String header = "";
		while(true) {
			DataInputStream input;
			try {
				input = new DataInputStream(s.getInputStream());
				byte[] receive = new byte[8072];
				input.read(receive);
				header += receive;
				if (header.contains("\r\n\r\n")|| header.contains("\n\n") || header.contains("\n\r\n")) {
					break;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		header = header.replace("Connection: keep-alive", CLOSE);
		header = header.replace("Proxy-connection: keep-alive", CLOSE_PROXY);
		
		try {
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			output.write(header.getBytes());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		// now, try receive from server and send bak to client
		while (true) {
			try {
				DataInputStream input = new DataInputStream(s.getInputStream());
				byte[] receive = new byte[8072];
				input.read(receive);

				if (receive.length == 0) {
					break;
				}
				DataOutputStream output = new DataOutputStream(s.getOutputStream());
				output.write(receive);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			s.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
