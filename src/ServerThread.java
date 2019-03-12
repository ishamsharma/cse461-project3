package src;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ServerThread implements Runnable {

	private InputStream input;
	private OutputStream output;

	public ServerThread(InputStream input, OutputStream output) {
		if (input == null || output == null) {
			throw new IllegalArgumentException();
		}
		this.input = input;
		this.output = output;
	}

	public void run() {
		try {
			// pass on message from CLIENT to SERVER
			int messg = input.read();
			while (messg != -1) {

				output.write(messg);

				messg = input.read();
			}
			this.input.close();
			this.output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}

}
