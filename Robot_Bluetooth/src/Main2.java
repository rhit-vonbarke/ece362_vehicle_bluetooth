import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.fazecast.jSerialComm.SerialPort;

public class Main2 {

	public static final byte[] BT_NEWLINE = new byte[] { (byte) 0x0A };
	static SerialPort chosenPort;
	static int x = 0;

	public static void main(String[] args) {

		// create and configure the window
		JFrame window = new JFrame();
		window.setTitle("Sensor Graph GUI");
		window.setSize(600, 400);
		window.setLayout(new BorderLayout());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// create a drop-down box and connect button, then place them at the top of the
		// window
		JComboBox<String> portList = new JComboBox<String>();
		JButton startButton = new JButton("Start");
		JPanel topPanel = new JPanel();
		topPanel.add(portList);
		topPanel.add(startButton);
		window.add(topPanel, BorderLayout.NORTH);

		// populate the drop-down box
		SerialPort[] portNames = SerialPort.getCommPorts();
		for (int i = 0; i < portNames.length; i++) {
			portList.addItem(portNames[i].getSystemPortName());
		}

		// create the line graph
		XYSeries series = new XYSeries("Current");
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createXYLineChart("Current Readings", "Time (seconds)", "Current (mA)",
				dataset);
		window.add(new ChartPanel(chart), BorderLayout.CENTER);

		// configure the connect button and use another thread to listen for data
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (startButton.getText().equals("Start")) {
					// attempt to connect to the serial port
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortParameters(115200, 8, 1, SerialPort.NO_PARITY);
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					while (true) {
						if (chosenPort.openPort()) {
							System.out.println("Port is Open");
							startButton.setText("Disconnect");
							portList.setEnabled(false);
//									try {
//										TimeUnit.SECONDS.wait(10);
//									} catch (InterruptedException e) {
//										e.printStackTrace();
//									}
							while (true) {
								try {
									if (System.in.read() == 83) { // If the console gets an "S"
										System.out.println("Got an S");
										byte[] sendS = new byte[] { (byte) 0x53 };
										chosenPort.writeBytes(sendS, 1);
										break;
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							break;
						} else {
							// Wait until open
						}
					}

					// create a new thread that listens for incoming text and populates the graph
					Thread thread = new Thread() {
						@Override
						public void run() {
							while (true) {
								System.out.println("Made it to Run");
								chosenPort.writeBytes(BT_NEWLINE, 1);
								try {
									TimeUnit.SECONDS.sleep(1);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								InputStream in = chosenPort.getInputStream();
								StringBuilder builder = new StringBuilder();
								char value = ' ';
								for (int i = 0; i < 100; i++) {
									try {
										value = (char) in.read();
									} catch (IOException e1) {
										e1.printStackTrace();
									}
									builder.append(value);
									if (value == '*') {
										break;
									}
								}
								System.out.println(builder.toString());
								// System.out.println("Has Next Line");
								// System.out.println(line);
								// int number = Integer.parseInt(line);
								// series.add(x++, 1023 - number);
								series.add(x++, 1023 - (Math.random() * 10));
								window.repaint();
								// chosenPort.writeBytes(BT_NEWLINE, 1);
								// System.out.println("Closing");
							}
						}
					};
					thread.start();
				} else {
					// disconnect from the serial port
					chosenPort.closePort();
					portList.setEnabled(true);
					startButton.setText("Start");
					series.clear();
					x = 0;
				}
			}
		});
		// show the window
		window.setVisible(true);
	}

	public static VehicleDataPoint processInput(String input, XYSeries series) {
		switch (input.charAt(0)) {
		case '#': // data
			String[] dataSeg = input.split(":");
			String milliStr = dataSeg[dataSeg.length - 1];
			int millis = Integer.valueOf(milliStr.substring(0, milliStr.length() - 1));
			return new VehicleDataPoint(dataSeg, millis);
		default:
			return null;
		}
	}

	public static class VehicleDataPoint {
		public double shuntVoltage;
		public double busVoltage;
		public double current;
		public double power;
		public double loadVoltage;
		public int millis;

		public VehicleDataPoint(String[] data, int millis) {
			this.shuntVoltage = Double.valueOf(data[0]);
			this.busVoltage = Double.valueOf(data[1]);
			this.current = Double.valueOf(data[2]);
			this.power = Double.valueOf(data[3]);
			this.loadVoltage = Double.valueOf(data[4]);
			this.millis = millis;
		}

	}

}
