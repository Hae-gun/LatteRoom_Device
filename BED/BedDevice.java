package arduino.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import arduino.device.BedDevice2.SerialListener;
import arduino.device.BedDevice2.ServerListener;
import arduino.device.vo.*;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class BedDevice2 extends Application implements TestClient {
	////// ROOM 1 BED
	private static final String DEVICE_ID = "LATTE02";
	private static final String DEVICE_TYPE = "DEVICE"; // App : "USER"

	private static final String COMPORT_NAMES = "COM4";
//	private static final String SERVER_ADDR = "70.12.60.105";
	private static final String SERVER_ADDR = "70.12.60.99";
//	private static final String SERVER_ADDR = "localhost";
	private static final int SERVER_PORT = 55566;
	private static final String SENSOR_NO2="DEVICE021";
	private BorderPane root;
	private TextArea textarea;

	private ServerListener toServer = new ServerListener();
	private SerialListener toArduino = new SerialListener();
	private BedSharedObject2 sharedObject;

//	private Sensor temp = new Sensor(this, "TEMP", "TEMP");
//	private Sensor heat = new Sensor(this, "HEAT", "HEAT");

	private Sensor bedSensor = new Sensor("BED", "BED");

	private static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();

	// ======================================================
	public void displayText(String msg) {
		Platform.runLater(() -> {
			textarea.appendText(msg + "\n");
		});
	}

	public static Gson getGson() {
		return gson;
	}

	@Override
	public String getDeviceID() {
		// TODO Auto-generated method stub
		return DEVICE_ID;
	}

	@Override
	public String getDeviceType() {
		// TODO Auto-generated method stub
		return DEVICE_TYPE;
	}

	@Override
	public String getSensorList() {
		List<Sensor> sensorList = new ArrayList<Sensor>();
		sensorList.add(bedSensor);
		return gson.toJson(sensorList);
	}

	// ======================================================
	@Override
	public void start(Stage primaryStage) throws Exception {

		// Logic
		toServer.initialize();
		toArduino.initialize();

		// SharedObject
		sharedObject = new BedSharedObject2(this,toServer, toArduino);

		// UI ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		root = new BorderPane();
		root.setPrefSize(700, 500);

		// Center ----------------------------------------------
		textarea = new TextArea();
		textarea.setEditable(false);
		root.setCenter(textarea);

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("DeviceBed");
		primaryStage.setOnCloseRequest((e) -> {
			toServer.close();
			toArduino.close();
		});
		primaryStage.show();
	}// start()

	// ======================================================
	public static void main(String[] args) {
		launch(args);
	}

	// ======================================================
	class ServerListener {
		private Socket socket;
		private BufferedReader serverIn;
		private PrintWriter serverOut;
		private ExecutorService executor;

		public void initialize() {

			executor = Executors.newFixedThreadPool(1);

			Runnable runnable = () -> {
				try {
					socket = new Socket();
					socket.connect(new InetSocketAddress(SERVER_ADDR, SERVER_PORT));
					serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					serverOut = new PrintWriter(socket.getOutputStream());
				} catch (IOException e) {
//					e.printStackTrace();
					close();
					return;
				}

				//
//				send(getDeviceID());
//				send(getDeviceType());
//				send(new Message(getDeviceID()
//						, "SENSOR_LIST"
//						, getSensorList()));
				send(gson.toJson(new Lattemessage(SENSOR_NO2, "CONNECT", null, null)));
				
				String line = "";
				while (true) {
					try {
						line = serverIn.readLine();

						if (line == null) {
							displayText("server error. disconnected");
							throw new IOException();
						} else {
							displayText("Server ] " + line);

							try {
//								Lattemessage message = gson.fromJson(line, Lattemessage.class);
//								String code1 = message.getCode1();
//								String code2 = message.getCode2();
//								if ("CONTROL".equals(code1) && "BED".equals(code2)) {
//									SensorData sd = gson.fromJson(message.getJsonData(), SensorData.class);
//									System.out.println(sd.getStates());
//									String s = sd.getStates();
////									if(SENSOR_NO.equals(sd.getSensorNo()))
//										toArduino.send(s + "\n");
//								}
							} catch (Exception e2) {

							}
//							bedSensor.setRecentData(message.getJsonData());
//							toArduino.send(bedSensor.getStates());
//							sharedObject.setHopeStates(hopeTemp);

						}
					} catch (IOException e) {
						e.printStackTrace();
						close();
						break;
					}
				} // while()
			};
			executor.submit(runnable);
		} // startClient()

		public void close() {
			try {
				if (socket != null && !socket.isClosed()) {
					socket.close();
					if (serverIn != null)
						serverIn.close();
					if (serverOut != null)
						serverOut.close();
				}
				if (executor != null && !executor.isShutdown()) {
					executor.shutdownNow();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // stopClient()

		public void send(String msg) {
			serverOut.println(msg);
			serverOut.flush();
		}

		public void send(Message msg) {
			serverOut.println(gson.toJson(msg));
//			serverOut.println("ì„œë²„ì•¼ ì¢€ ë°›ì•„ë�¼~!");
			serverOut.flush();
			displayText("send Complete]  " + msg);
		}

	} // ServerListener

	// ======================================================
	class SerialListener implements SerialPortEventListener {

		SerialPort serialPort;

		private BufferedReader serialIn;
		private PrintWriter serialOut;
		private static final int TIME_OUT = 2000;
		private static final int DATA_RATE = 9600;

		public void initialize() {
			CommPortIdentifier portId = null;
			try {
				portId = CommPortIdentifier.getPortIdentifier(COMPORT_NAMES);
			} catch (NoSuchPortException e1) {
				e1.printStackTrace();
			}
			;

			if (portId == null) {
				System.out.println("Could not find COM port.");
				return;
			}

			try {
				// open serial port, and use class name for the appName.
				serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

				// set port parameters
				serialPort.setSerialPortParams(DATA_RATE, // 9600
						SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				// open the streams
				serialIn = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
				serialOut = new PrintWriter(serialPort.getOutputStream());

				// add event listeners
				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}

		/**
		 * This should be called when you stop using the port. This will prevent port
		 * locking on platforms like Linux.
		 */
		public synchronized void close() {
			if (serialPort != null) {
				serialPort.removeEventListener();
				serialPort.close();
			}
		}

		public synchronized void send(String msg) {
			serialOut.println(msg);
			serialOut.flush();
		}

		/**
		 * Handle an event on the serial port. Read the data and print it.
		 */
		public synchronized void serialEvent(SerialPortEvent oEvent) {
			if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				try {
					String inputLine = serialIn.readLine();
					displayText("get : " + inputLine);
					
					String[] s = inputLine.split(",");
					
					
//					displayText("Serial ] " + inputLine);
//					float eventTemp = Float.parseFloat(inputLine);
//					displayText("Serial ] " + eventTemp);

				} catch (Exception e) {
//					System.err.println(e.toString() + "  : prb de lecture");
				}
			}
			// Ignore all the other eventTypes, but you should consider the other ones.
		}

	} // SerialListener

} // TempDevice
//
class BedSharedObject2 {
	// Temperature & Heat & Cool
	private int hopeStates = 23;
	private int states = 1000;
	private TestClient client;
	private ServerListener toServer;
	private SerialListener toArduino;

//	BedSharedObject(TestClient client, ServerListener toServer) {
//		this.client = client;
//		this.toServer = toServer;
//	}

	BedSharedObject2(TestClient client, ServerListener toServer, SerialListener toArduino) {
		this.client = client;
		this.toServer = toServer;
		this.toArduino = toArduino;
	}

	public synchronized int getHopeStates() {
		return this.hopeStates;
	}

	public synchronized void setHopeStates(int hopeStates) {
//		if(hopeStates == 1000) {
//			this.hopeStates = hopeStates;
//		} else {
//			this.hopeStates = hopeStates;
//			control();
//		}
		this.hopeStates = hopeStates;
		control();
	}

	public synchronized int getStates() {
		return states;
	}

	public synchronized void setStates(int states) {
//		if(states == 1000) {
//			this.states = states;
//		} else {
//		}
		this.states = states;
		control();
	}

	private synchronized void control() {

		if (hopeStates > states) {
//			if(cool.equals("ON")) {
//				toArduino.send("COOLOFF");
//				toServer.send(new Message(client.getDeviceID(), "COOL", "OFF"));
//				cool = "OFF";
//			}
//			
//			if(heat.equals("OFF")) {
//				toArduino.send("HEATON");
////				toServer.send("HEAT","ON");
//				toServer.send(new Message(client.getDeviceID(), "HEAT", "ON"));
//				heat = "ON";
//			}
//		} else if (hopeStates < states) {
//			if(heat.equals("ON")) {
//				toArduino.send("HEATOFF");
////				toServer.send("HEAT", "OFF");
//				toServer.send(new Message(client.getDeviceID(), "HEAT", "OFF"));
//				heat = "OFF";
//			}
//			
//			if(cool.equals("OFF")) {
//				toArduino.send("COOLON");
////				toServer.send("COOL", "ON");
//				toServer.send(new Message(client.getDeviceID(), "COOL", "ON"));
//				cool = "ON";
//			}
//		} else {
//			if(heat.equals("ON")) {
////				toArduino.send("BOTHOFF");
//				toArduino.send("HEATOFF");
////				toServer.send("HEAT", "OFF");
//				toServer.send(new Message(client.getDeviceID(), "HEAT", "OFF"));
//				heat = "OFF";
//			}
//			
//			if(cool.equals("ON")) {
//				toArduino.send("COOLOFF");
////				toServer.send("COOL","OFF");
//				toServer.send(new Message(client.getDeviceID(), "COOL", "OFF"));
//				cool = "OFF";
//			}
		}
	}

}