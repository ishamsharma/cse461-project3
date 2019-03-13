package src;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
	
	public static void start(int port) {
		ServerSocket servSock = null;
		try {
			servSock = new ServerSocket(port);
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			Date date = new Date();
			System.out.println(formatter.format(date) + " - >>> Proxy listening on " + port);
			while (true) {
				Socket sock = servSock.accept();
				Thread t = new Thread(new ProxyThread(sock));
				t.start();
				
			}
			
		}catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int port = 1000;
		if (args.length != 1) {
			System.out.println("Need to specify port number");
			
			System.exit(1);
		}
		try {
			port = Integer.valueOf(args[0]).intValue();
			//port = 46103;
			start(port);
		} catch (NumberFormatException e) {
			System.out.println("NumberFormat: " + e.getMessage());
		}

	}

}
