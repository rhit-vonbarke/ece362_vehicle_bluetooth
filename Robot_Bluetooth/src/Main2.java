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
	public static boolean flag = false;
	public static boolean sPressed = false;
	public static boolean kPressed = false;
	public static double totalEnergy = 0; // use THIS for displaying total energy usage. this is not graphed

	public static void main(String[] args) {

		// create and configure the window
		JFrame window = new JFrame();
		window.setTitle("Energy Graph GUI");
		window.setLayout(new BorderLayout());
		window.setExtendedState(JFrame.MAXIMIZED_BOTH);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// create a drop-down box and connect button, then place them at the top of the
		// window
		JComboBox<String> portList = new JComboBox<String>();
		JButton connectButton = new JButton("Connect");
		JButton sButton = new JButton("Start Robot");
		JButton dButton = new JButton("Debug Robot");
		JButton kButton = new JButton("Kill Robot");
		JPanel topPanel = new JPanel();
		topPanel.add(portList);
		topPanel.add(connectButton);
		topPanel.add(sButton);
		topPanel.add(dButton);
		topPanel.add(kButton);
		window.add(topPanel, BorderLayout.NORTH);

		// populate the drop-down box
		SerialPort[] portNames = SerialPort.getCommPorts();
		for (int i = 0; i < portNames.length; i++) {
			portList.addItem(portNames[i].getSystemPortName());
		}

		// create the line graph
		XYSeries series = new XYSeries("Energy");
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createXYLineChart("Energy Readings", "Time (seconds)", "Energy (mJ)", dataset);
		window.add(new ChartPanel(chart), BorderLayout.CENTER);

		// configure the connect button and use another thread to listen for data
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (connectButton.getText().equals("Connect")) {
					// attempt to connect to the serial port
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortParameters(115200, 8, 1, SerialPort.NO_PARITY);
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					if (!sPressed) {
						while (true) {
							if (chosenPort.openPort()) {
								System.out.println("Port is Open");
								connectButton.setText("Disconnect");
								portList.setEnabled(false);
								break;
							} else {
								// Wait until open
							}
						}
					}

					// create a new thread that listens for incoming text and populates the graph
					Thread thread = new Thread() {
						@Override
						public void run() {
							while (true) {
								if (kPressed) {
									System.out.println("Killing Robot");
									chosenPort.closePort();
									break;
								}
								System.out.println("Made it to Run");
								chosenPort.writeBytes(BT_NEWLINE, 1);

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
								VehicleDataPoint newestPoint = processInput(builder.toString());
								if (newestPoint != null) {
									System.out.println(newestPoint.energy);
									if (!flag) {
										series.add(0, newestPoint.energy);
										flag = true;
									} else {
										series.add(newestPoint.millis / 1000, newestPoint.energy);
									}
									totalEnergy += newestPoint.energy;
								} else {
									System.out.println("NULL");
								}
								window.repaint();
							}
						}
					};
					thread.start();
				} else {
					// disconnect from the serial port
					chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setText("Connect");
					series.clear();
				}
			}
		});

		sButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				byte[] sendS = new byte[] { (byte) 0x53 };
				chosenPort.writeBytes(sendS, 1);
				sPressed = true;
			}
		});

		kButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				byte[] sendK = new byte[] { (byte) 0x4B };
				chosenPort.writeBytes(sendK, 1);
				kPressed = true;
			}
		});
		// show the window
		window.setVisible(true);
	}

	public static VehicleDataPoint processInput(String input) {
		switch (input.charAt(0)) {
		case '#': // data
			String[] dataSeg = input.split(":");
			return new VehicleDataPoint(dataSeg);
		case '!': // Command, car is starting or stopping
			System.out.println("Car is Starting or Stopping");
			return null;
		case '?': // Getting information
			System.out.println("Car is Sending Information");
			return null;
		default:
			System.out.println(input.charAt(0));
			return null;
		}
	}

	public static class VehicleDataPoint {
		public double shuntVoltage;
		public double busVoltage;
		public double current;
		public double power;
		public double loadVoltage;
		public double millis; // this is the timestamp used for graphing
		public double ellapsedMillis; // this is calculated at instantiation to be the elapsed time
		public double energy;

		public static double lastMillis = 0;

		public VehicleDataPoint(String[] data) {
			this.shuntVoltage = Double.valueOf(data[1]);
			this.busVoltage = Double.valueOf(data[2]);
			this.current = Double.valueOf(data[3]);
			this.power = Double.valueOf(data[4]);
			this.loadVoltage = Double.valueOf(data[5]);
			this.millis = Double.valueOf(data[6]);
			this.ellapsedMillis = (ellapsedMillis == 0) ? 500 : millis - lastMillis;
			lastMillis = millis;
			this.energy = this.energyCalculation();
		}

		// power * ellapsed time
		public double energyCalculation() {
			return (power * ellapsedMillis) / 1000;
		}
	}

}
