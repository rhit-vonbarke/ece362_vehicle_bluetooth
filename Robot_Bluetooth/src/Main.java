import java.io.IOException;

import com.fazecast.jSerialComm.*;

public class Main {

	public static void main(String[] args) throws IOException {
		String dataCOM = "COM6";
		boolean endLoop = true;

		System.out.println("START");
		SerialPort robotPort = SerialPort.getCommPort(dataCOM);
		robotPort.openPort();
		robotPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);
		robotPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 0);
		if (robotPort.isOpen()) {
			while (endLoop) {
				if (System.in.read() == 83) { // "S"
					System.out.println("Got an S!");
					byte[] sInBinary = new byte[] {0, 1, 0, 1, 0, 0, 1, 1};
					//sInBinary = 01110011;
					robotPort.writeBytes(sInBinary, 1);
					endLoop = false;
				}
			}
			try {
				while (true) {
					byte[] buffer = new byte[1024];
					int message = robotPort.readBytes(buffer, buffer.length);
					System.out.println("Read " + message + " bytes");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			robotPort.closePort();
		}
	}
}
