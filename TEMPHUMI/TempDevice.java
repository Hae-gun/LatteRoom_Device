package arduino.device;

import java.awt.DisplayMode;
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

import arduino.device.TempDevice2.SerialListener;
import arduino.device.TempDevice2.ServerListener;
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

public class TempDevice2 extends Application implements TestClient {

	private static final String DEVICE_ID = "LATTE01";
	private static final String DEVICE_TYPE = "DEVICE"; // App : "USER"

	private static final String SENSOR_NO = "LA11050";

//	private static final String COMPORT_NAMES = "COM4";
	private static final String COMPORT_NAMES = "COM4";
//	private static final String SERVER_ADDR = "70.12.60.105";
	private static final String SERVER_ADDR = "70.12.60.99";
//	private static final String SERVER_ADDR = "192.168.35.103";
	private static final int SERVER_PORT = 55566;
	private static final String SENSOR_NO2 = "DEVICE021";
//	private static final int SERVER_PORT = 55577;

	private BorderPane root;
	private TextArea textarea;

	private ServerListener toServer = new ServerListener();
	private SerialListener toArduino = new SerialListener();
	private TempSharedObject sharedObject;

	private Sensor temp = new Sensor("TEMP", "TEMP");
	private Sensor heat = new Sensor("HEAT", "HEAT");
	private Sensor cool = new Sensor("COOL", "COOL");

//	private static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();
	private Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

	// ======================================================
	public void displayText(String msg) {
		Platform.runLater(() -> {
			textarea.appendText(msg + "\n");
		});
	}

//	public static Gson getGson() {
//		return gson;
//	}

//	public static String getDeviceId() {
//		return DEVICE_ID;
//	}
//	
//	public static String getDeviceType() {
//		return DEVICE_TYPE;
//	}

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
		sensorList.add(temp);
		sensorList.add(heat);
		sensorList.add(cool);
		return gson.toJson(sensorList);
	}

//	public static List<Sensor> getSensorList() {
//		List<Sensor> sensorList = new ArrayList<Sensor>();
//		sensorList.add(temp);
//		sensorList.add(heat);
//		sensorList.add(cool);
//		return sensorList;		
//	}

	// ======================================================
	@Override
	public void start(Stage primaryStage) throws Exception {

		// Logic
		toServer.connect();
		toArduino.initialize();

		// SharedObject
		sharedObject = new TempSharedObject(this, toServer, toArduino);
//		sharedObject = new TempSharedObject(this, toServer);

		// UI ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		root = new BorderPane();
		root.setPrefSize(700, 500);

		// Center ----------------------------------------------
		textarea = new TextArea();
		textarea.setEditable(false);
		root.setCenter(textarea);

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("DeviceTemp");
		primaryStage.setOnCloseRequest((e) -> {
			toServer.disconnect();
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

		public void connect() {

			executor = Executors.newFixedThreadPool(3);

			Runnable runnable = () -> {
				try {
					socket = new Socket();
					socket.connect(new InetSocketAddress(SERVER_ADDR, SERVER_PORT));
					serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					serverOut = new PrintWriter(socket.getOutputStream());
				} catch (IOException e) {
//					e.printStackTrace();
					disconnect();
					return;
				}

				//
//				send(getDeviceID());
//				send(getDeviceType());
//				send(gson.toJson(new Lattemessage(getDeviceID(), "CONNECT", null, null)));
//				sharedObject.setHopeStates(30);
				send(gson.toJson(new Lattemessage(SENSOR_NO2, "CONNECT", null, null)));
				String line = "";
				while (true) {
					try {
//						System.out.println("start");
						line = serverIn.readLine();
						System.out.println(line);
						displayText("FromServer] " + line);
						if (line == null) {
							displayText("server error. disconnected");
							throw new IOException();
						} else {
							displayText(line);

							if (line.contains("Control")) {
								try {
									System.out.println("line] " + line);
									Lattemessage lmsg = gson.fromJson(line, Lattemessage.class);
									System.out.println(lmsg.toString());
									System.out.println(lmsg.getCode1());
									String code1 = lmsg.getCode1();
									String code2 = lmsg.getCode2();
									System.out.println(lmsg.getJsonData());
									if ("Control".equals(code1) && "TEMP".equals(code2)) {
										SensorData data = gson.fromJson(lmsg.getJsonData(), SensorData.class);
										System.out.println("HopeTemp] "+data.getStates());
										
										sharedObject.setHopeStates(Integer.valueOf(data.getStates()));

									}

//									toArduino.send("COOLON");
//									displayText("jsonData] " + lmsg.getJsonData());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

						}
					} catch (IOException e) {
						e.printStackTrace();
						disconnect();
						break;
					}
				} // while()
			};
			System.out.println("1");
			executor.submit(runnable);
			System.out.println("2");
		} // startClient()

		public void disconnect() {
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
			displayText("sendMessage} " + msg);
		}

//		public void send(String sensorID, String states) {
////			Lattemessage message = new Lattemessage(new SensorData(sensorID, states));
//			Lattemessage message = new Lattemessage();
//			send(gson.toJson(message));
//		}

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
//			;

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
			serialOut.println(msg + "\n");
			serialOut.flush();
		}

		/**
		 * Handle an event on the serial port. Read the data and print it.
		 */
		public synchronized void serialEvent(SerialPortEvent oEvent) {
			if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				try {
					
					String inputLine = serialIn.readLine();
					
					
//					String humi = 
//					displayText("Serial ] " + inputLine);
//					float eventTemp = Float.parseFloat(inputLine);
//					displayText("Serial ] " + eventTemp);
//					String s = "";
//					int currentTemp = sharedObject.getStates();
//					
//					System.out.println("eventTemp] "+eventTemp+",currentTemp] "+currentTemp);
//					if (currentTemp == 1000) {
//						sharedObject.setStates((int) (eventTemp + 4));
//						temp.setRecentData(String.valueOf((int) (eventTemp + 4)));
//						displayText("tempObject: " + temp.getRecentData());
////						String lmsg = gson.toJson(new Message(DEVICE_ID, temp.getRecentData()));
////						displayText(lmsg);
////						toServer.send(lmsg);
////						displayText("!!!!!!!!!!!!");
//						return;
//					}
//
//					if (currentTemp + 0.8 < eventTemp) {
//						displayText("\t" + (currentTemp) + " < " + eventTemp);
//						currentTemp++;
//						temp.setRecentData(String.valueOf(currentTemp));
//						sharedObject.setStates(currentTemp);
//						SensorData data = temp.getRecentData();
//
//						String json = gson.toJson(data);
//						Lattemessage message = new Lattemessage(DEVICE_ID, "Update", "TEMP", json);
//						s = gson.toJson(message);
////						displayText("message] " + message.toString());
////						displayText("SensorData] - " + temp.getRecentData());
//						toServer.send(gson.toJson(message));
//
////						Message message = new Message(temp.getRecentData());
//
////						Lattemessage lmsg = new Lattemessage(clientNo, code1, code2, jsonData);
////						displayText(message.toString());
////						toServer.send(gson.toJson(message)+"!!!");
//					} else if (currentTemp - 0.2 > eventTemp) {
//						displayText("\t" + (currentTemp) + " > " + eventTemp);
//						currentTemp--;
//						temp.setRecentData(String.valueOf(currentTemp));
//						sharedObject.setStates(currentTemp);
//
////						Message message = new Message(temp.getRecentData());
//						SensorData data = temp.getRecentData();
//
//						String json = gson.toJson(data);
//						Lattemessage message = new Lattemessage(DEVICE_ID, "Update", "TEMP", json);
//						s = gson.toJson(message);
////						displayText("message] " + message.toString());
//						toServer.send(gson.toJson(s));
//					} else {
//						displayText("\t" + (currentTemp) + " / " + eventTemp);
//					}
//
//					displayText("recentData]  - " + temp.getRecentData().toString());
//					displayText("LatteMessage]  - " + s);
//					System.out.println("recentData]  - " + temp.getRecentData().toString());
				} catch (Exception e) {
//					displayText("error: " + e.toString());
					e.printStackTrace();
				}
			}
			// Ignore all the other eventTypes, but you should consider the other ones.
		}

	} // SerialListener

} // TempDevice

class TempSharedObject {
	// Temperature & Heat & Cool
	private int hopeStates = 23;
	private int states = 1000;
	private static final String DEVICE_ID = "LATTE01";
	private String heat = "OFF";
	private String cool = "OFF";

	private TestClient client;
	private ServerListener toServer;
	private SerialListener toArduino;
	private Gson gson = new Gson();
//	TempSharedObject(TestClient client, ServerListener toServer) {
//		this.client = client;
//		this.toServer = toServer;
//	}

	TempSharedObject(TestClient client, ServerListener toServer, SerialListener toArduino) {
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
		System.out.println("Set HopeData");
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
		System.out.println("hope- "+hopeStates+", state- "+states);
		Lattemessage lmsg = new Lattemessage(DEVICE_ID, "CONTROL", null, null);
		if (hopeStates > states) {
			if (cool.equals("ON")) {
				toArduino.send("COOLOFF");

//				Lattemessage lmsg = new Lattemessage();
				SensorData data = new SensorData();
				lmsg.setCode2("COOL");
				data.setStates("OFF");
				lmsg.setJsonData(gson.toJson(data));
				toServer.send(gson.toJson(lmsg));
				cool = "OFF";
			}

			if (heat.equals("OFF")) {
				toArduino.send("HEATON");
//				toServer.send("HEAT","ON");
				SensorData data = new SensorData();
				lmsg.setCode2("HEAT");
				data.setStates("ON");
				lmsg.setJsonData(gson.toJson(data));
				toServer.send(gson.toJson(lmsg));
				heat = "ON";
			}
		} else if (hopeStates < states) {
			if (heat.equals("ON")) {
				toArduino.send("HEATOFF");
//				toServer.send("HEAT", "OFF");
				SensorData data = new SensorData();
				lmsg.setCode2("HEAT");
				data.setStates("OFF");
				lmsg.setJsonData(gson.toJson(data));
				toServer.send(gson.toJson(lmsg));
				heat = "OFF";
			}

			if (cool.equals("OFF")) {
				toArduino.send("COOLON");
//				toServer.send("COOL", "ON");
				SensorData data = new SensorData();
				lmsg.setCode2("COOL");
				data.setStates("ON");
				lmsg.setJsonData(gson.toJson(data));
				toServer.send(gson.toJson(lmsg));
				cool = "ON";
			}
		} else {
			if (heat.equals("ON")) {
//				toArduino.send("BOTHOFF");
				toArduino.send("HEATOFF");
//				toServer.send("HEAT", "OFF");
				SensorData data = new SensorData();
				lmsg.setCode2("HEAT");
				data.setStates("OFF");
				lmsg.setJsonData(gson.toJson(data));
				toServer.send(gson.toJson(lmsg));
				heat = "OFF";
			}

			if (cool.equals("ON")) {
				toArduino.send("COOLOFF");
//				toServer.send("COOL","OFF");
				SensorData data = new SensorData();
				lmsg.setCode2("COOL");
				data.setStates("OFF");
				lmsg.setJsonData(gson.toJson(data));
				toServer.send(gson.toJson(lmsg));
				cool = "OFF";
			}
		}
//		System.out.println("HEAT: "+heat+"---"+"COOL: "+cool);
	}

}