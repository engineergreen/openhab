package org.openhab.binding.cm11a.internal;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver for the CM11 X10 interface.
 *
 * 
 * @author anthony
 * @see <a href="http://www.heyu.org/docs/protocol.txt">CM11 Protocol specification</a>
 * @see <a href="http://www.rxtx.org">RXTX Serial API for JavaM</a>
 */
public class X10Interface extends Thread implements SerialPortEventListener  {

	private static final Logger log = LoggerFactory.getLogger(X10Interface.class);

	// X10 Function codes
	static final int FUNC_ALL_UNITS_OFF = 0x0;
	static final int FUNC_ALL_LIGHTS_ON = 0x1;
	static final int FUNC_ON = 0x2;
	static final int FUNC_OFF = 0x3;
	static final int FUNC_DIM = 0x4;
	static final int FUNC_BRIGHT = 0x5;
	static final int FUNC_ALL_LIGHTS_OFF = 0x6;
	static final int FUNC_EXTENDED = 0x7;
	static final int FUNC_HAIL_REQ = 0x8;
	static final int FUNC_HAIL_ACK = 0x9;
	static final int FUNC_PRESET_DIM_1 = 0xA;
	static final int FUNC_PRESET_DIM_2 = 0xB;
	static final int FUNC_EXT_DATA_TRANSFER = 0xC;
	static final int FUNC_STATUS_ON = 0xD;
	static final int FUNC_STATUS_OFF = 0xE;
	static final int FUNC_STATUS_REQ = 0xF;

	// Definitions for the header:code bits
	/**
	 * Bit mask for the bit that is always set in a header:code
	 */
	static final int HEAD = 0x04;
	/**
	 * Bit mask for Function/Address bit of header:code.  
	 */
	static final int HEAD_FUNC = 0x02;
	/**
	 * Bit mask for standard/extended transmission bit of header:code.
	 */
	static final int HEAD_EXTENDED = 0x01;


	/**
	 * Byte sent from PC to Interface to acknowledge the receipt of a correct checksum.  
	 * If the checksum was incorrect, the PC should retransmit.
	 */
	static final int CHECKSUM_ACK = 0x00;
	/**
	 * Byte send from Interface to PC to indicate it has sent the desired data over the
	 * X10/power lines.  
	 */
	static final int IF_READY = 0x55;

	/**
	 * Byte sent from Interface to PC to request that its clock is set.  
	 * Interface will send this to the PC repeatedly after a power-failure and will not respond to commands
	 * until its clock has been set.
	 */
	static final int CLOCK_SET_REQ =   0xA5;
	/**
	 * Byte sent from PC to interface to start a transmission that sets the interface clock.
	 */
	static final int CLOCK_SET_HEAD = 0x9B;

	/**
	 * Byte sent from interface to PC to indicate that it has X10 data pending transmission to the PC.  
	 */
	static final int DATA_READY_REQ = 0x5a;
	static final int DATA_READY_HEAD = 0xc3;


	/** 
	 * THe house code to be monitored.  Not sure what this means, but it is part of the clock set instruction.
	 * For the moment hardcoded here to be House 'E'.
	 */
	static final int MONITORED_HOUSE_CODE = 0x10;


	static final Map<Character, Integer> HOUSE_CODES;
	static final Map<Integer,Integer> DEVICE_CODES;

	static{
		HashMap<Character,Integer> houseCodes = new HashMap<Character, Integer>(16);
		houseCodes.put('A',(int) 0x60);
		houseCodes.put('B',(int) 0xE0);
		houseCodes.put('C',(int) 0x20);
		houseCodes.put('D',(int) 0xA0);
		houseCodes.put('E',(int) 0x10);
		houseCodes.put('F',(int) 0x90);
		houseCodes.put('G',(int) 0x50);
		houseCodes.put('H',(int) 0xD0);
		houseCodes.put('I',(int) 0x70);
		houseCodes.put('J',(int) 0xF0);
		houseCodes.put('K',(int) 0x30);
		houseCodes.put('L',(int) 0xB0);
		houseCodes.put('M',(int) 0x00);
		houseCodes.put('N',(int) 0x80);
		houseCodes.put('O',(int) 0x40);
		houseCodes.put('P',(int) 0xC0);

		HOUSE_CODES = Collections.unmodifiableMap(houseCodes);

		HashMap<Integer,Integer> deviceCodes = new HashMap<Integer, Integer>(16);
		deviceCodes.put(1,(int) 0x06);
		deviceCodes.put(2,(int) 0x0E);
		deviceCodes.put(3,(int) 0x02);
		deviceCodes.put(4,(int) 0x0A);
		deviceCodes.put(5,(int) 0x01);
		deviceCodes.put(6,(int) 0x09);
		deviceCodes.put(7,(int) 0x05);
		deviceCodes.put(8,(int) 0x0D);
		deviceCodes.put(9,(int) 0x07);
		deviceCodes.put(10,(int) 0x0F);
		deviceCodes.put(11,(int) 0x03);
		deviceCodes.put(12,(int) 0x0B);
		deviceCodes.put(13,(int) 0x00);
		deviceCodes.put(14,(int) 0x08);
		deviceCodes.put(15,(int) 0x04);
		deviceCodes.put(16,(int) 0x0C);

		DEVICE_CODES = Collections.unmodifiableMap(deviceCodes);
	}


	// Constants that control the interaction with the hardware
	static final int IO_PORT_OPEN_TIMEOUT = 5000;

	/**
	 * How long to wait between attempts to reconnect to the interface.  (ms)
	 */
	static final int IO_RECONNECT_INTERVAL = 5000;
	/**
	 * Maximum number of times to retry sending a bit of data when checksum errors occur.
	 */
	static final int IO_MAX_SEND_RETRY_COUNT = 5;


	// Hardware IO attributes
	protected CommPortIdentifier portId;
	protected SerialPort serialPort;
	protected boolean connected = false;
	protected DataOutputStream serialOutput;
	protected OutputStream serialOutputStr;
	protected DataInputStream serialInput;
	protected InputStream serialInputStr;

	/**
	 * Flag to indicate that background thread should be killed.  Used to deactivate plugin.
	 */
	protected boolean killThread = false; 


	// Scheduling attributes
	/**
	 * Queue of as-yet un-actioned requests.
	 */
	protected BlockingQueue<X10FunctionCall> callQueue = new ArrayBlockingQueue<X10FunctionCall>(256);

	/**
	 * 
	 * @param serialPort serial port device. e.g. /dev/ttyS0
	 * @throws NoSuchPortException 
	 * 
	 */
	public X10Interface(String serialPort) throws NoSuchPortException  {
		super();
		log.trace("Constructing X10Interface for serial port: " + serialPort);
		portId = CommPortIdentifier.getPortIdentifier(serialPort);

	}

	/**
	 * Establishes a serial connection to the hardware, if one is not already established.
	 */
	protected boolean connect(){
		if (!connected){
			if (serialPort != null){
				log.trace("Closing stale serialPort object before reconnecting");
				serialPort.close();
			}
			log.debug("Connecting to X10 hardware on serial port: " + portId.getName());
			try {
				serialPort = (SerialPort) portId.open("ChocoX10 Interface", IO_PORT_OPEN_TIMEOUT);
				serialPort.setSerialPortParams(4800, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
				
				serialOutputStr = serialPort.getOutputStream();
				serialOutput = new DataOutputStream(serialOutputStr);
				serialInputStr = serialPort.getInputStream();
				serialInput = new DataInputStream(serialInputStr);
				

				serialPort.addEventListener(this);
				connected = true;
				
				serialPort.notifyOnDataAvailable(true);
				serialPort.notifyOnRingIndicator(true);
				purgeInputStream();

			} catch (PortInUseException e) {
				log.error("Serial port " + portId.getName() + " is in use by another application (" +  e.currentOwner + ")");
			} catch (UnsupportedCommOperationException e) {
				log.error("Serial port " + portId.getName() + " doesn't support the required baud/parity/stopbits");
			} catch (IOException e) {
				log.error("IO Problem with serial port " + portId.getName() + ".  " + e.getMessage());
			} catch (TooManyListenersException e) {
				log.error("TooManyListeners error when trying to connect to serial port.  Interface is unlikely to work, raise a bug report.",e);
			}
		} else {
			log.trace("Already connected to hardware, skipping reconnection.");
		}

		return connected;
	}



	/**
	 * Tranmits a standard (non-extended) X10 function.
	 * 
	 * @param address
	 * @param function
	 * @param dims 0-22, number of dims/brights to send.
	 * @return 
	 * @throws InvalidAddressException 
	 * @throws IOException 
	 */
	public void sendFunction(X10FunctionCall functionCall) throws InvalidAddressException, IOException{

		String address = functionCall.address;
		int function = functionCall.command;
		int dims = functionCall.dims;
		
		if (!validateAddress(address)) {
			throw new InvalidAddressException("Address " + address + " is not a valid X10 address");
		}


		int houseCode = HOUSE_CODES.get(address.charAt(0));
		int deviceCode = DEVICE_CODES.get(Integer.parseInt(address.substring(1)));


		int[] data = new int[2];

		synchronized (serialPort) {

			log.trace("Sending a standard X10 function to device: " + address);
			// First send address
			data[0] = HEAD ;
			data[1] = (int) (houseCode | deviceCode);
			sendData(data);

			// Now send function call
			data[0] = HEAD | HEAD_FUNC | (dims << 3); 
			data[1] = (int) (houseCode | function);
			sendData(data);
		}

	}

	/**
	 * Validates that the given string is a valid X10 address.  Returns true if this is the case.
	 * @param address
	 * @return
	 */
	public static boolean validateAddress(String address)  {
		log.trace("Validating device code: " + address + " length:" + address.length() + " House_code: '" + address.charAt(0) + "' Device code: '" + Integer.parseInt(address.substring(1)) + "'");
		return (! 
				(address.length() < 2 || address.length() > 3 
						|| !HOUSE_CODES.containsKey(new Character(address.charAt(0))) 
						|| !DEVICE_CODES.containsKey(Integer.parseInt(address.substring(1)))
						));
			
	}

	/**
	 * Queues a standard (non-extended) X10 function for transmission.
	 * 
	 * @param address
	 * @param function
	 * @param dims
	 * @return 
	 * @throws InvalidAddressException 
	 * @throws IOException 
	 */
	public void queueFunction(String address, int function) {
		queueFunction(address, function, (int)0);
	}

	/**
	 * Queues a standard (non-extended) X10 function for transmission.
	 * 
	 * @param address
	 * @param function
	 * @param dims
	 * @return 
	 * @throws InvalidAddressException 
	 * @throws IOException 
	 */
	public void queueFunction(String address, int function, int dims) { 
		if (!callQueue.offer(new X10FunctionCall(address, function, dims))) {
			log.error("X10 function call queue full.  Too many outstanding commands.  This command will be discarded");
		}
	}
	
	/**
	 * Sends data to the hardware and handles the checksuming and retry process.
	 * 
	 * <p>When applicanble, method blocks until the data has actually been sent over the powerlines using X10</p>
	 * 
	 * @param data Data to be sent.
	 * @throws IOException 
	 */
	protected void sendData(int[] data) throws IOException{

		int calcChecksum = 0;
		int checksumResponse = -1;

		// Calculate expected checksum:
		for (int i = 0; i < data.length; i++){
			// Note that ints are signed in Java, but the checksum uses unsigned ints.  
			// Hence some jiggery pokery to get an unsigned int from the data int.
			calcChecksum = (calcChecksum + (0x000000FF & ((int)data[i]))) & 0x000000FF;
			log.trace("Checksum calc: int " + i + " = " + Integer.toHexString(data[i]));
		}

		synchronized (serialPort) {

			// Stop background data listener as we want to have a dialogue with the interface here.
			serialPort.notifyOnDataAvailable(false);

			int retryCount = 0;
			while (checksumResponse != calcChecksum){
				retryCount++;
				for (int i = 0; i < data.length; i++){
					serialOutput.write(data[i]);
					serialOutput.flush();
				}
				checksumResponse = serialInput.readUnsignedByte();
				log.trace("Attempted to send data, try number: " + retryCount + 
						" Checksum expected: " + Integer.toHexString(calcChecksum) + " received: " + Integer.toHexString(checksumResponse));

				// On initial device power up, nothing works until we set the clock.  Check to see if that is what
				// is stopping the transmission.
				if (checksumResponse == CLOCK_SET_REQ){
					setClock();
				}
				if (retryCount > IO_MAX_SEND_RETRY_COUNT){
					log.error("Failed to send data to X10 hardware due to too many checksum failures");
					throw new IOException ("Max retries exceeded");
				}
			}

			log.trace("Data transmission to interface was successful, sending ACK.  X10 transmission over powerline will now commence.");
			serialOutput.write(CHECKSUM_ACK);
			serialOutput.flush();
			long startTime = System.currentTimeMillis();

			int response = serialInput.readUnsignedByte(); 
			if (response == IF_READY){
				log.trace("Interface has completed X10 transmission in " + Long.toString(System.currentTimeMillis() - startTime) + "ms");
			} else {
				log.warn("Expected IF_READY (" + Integer.toHexString((int)IF_READY & 0x00000FF) + ") response from hardware but received: " + Integer.toHexString((int)response & 0x00000FF) + " instead");
			}
			serialPort.notifyOnDataAvailable(true);	

		}
	}



	public void run(){
		
		log.trace("Starting background thread...");
		while(killThread == false){
			try {
				X10FunctionCall nextCall;
				log.trace("Getting next function call to be made");
				nextCall = callQueue.take();
				log.trace("Got a call.  Going to run it.");

				// Keep retrying to update this device until it is successful.
				boolean success = false;
				while (!success){
					try {
						if (connect()){
							sendFunction(nextCall);
							success = true;
						} else {
							Thread.sleep(IO_RECONNECT_INTERVAL);
						}
					} catch (IOException e) {
						connected = false;
						log.error("IO Exception when updating device.  Will retry shortly");
						Thread.sleep(IO_RECONNECT_INTERVAL);
					} catch (InvalidAddressException e) {
						log.error("Attempted to send an X10 Function call with invalid address.  Ignoring this.");
						success = true; // Pretend this was successful as retrying will be pointless.
					}
				}

			} catch (InterruptedException e1) {
				log.warn("Unexpected interrupt on X10 scheduling thread.  Ignoring and continuing anyway...");
			}
		}
		log.trace("Stopping background thread...");
		this.notifyAll();
	}

	public void serialEvent(SerialPortEvent event) {
		try {	
			if (event.getEventType() == event.DATA_AVAILABLE || event.getEventType() == event.RI){
				purgeInputStream();
			}
		} catch (IOException e) {
			log.error("IO Exception in serial port handler callback: " + e.getMessage());
		}
	}

	protected void purgeInputStream() throws IOException{

		synchronized (serialPort) {
			
			log.trace("Purging input stream");
			while (serialPort.isRI() && serialInput.available() <= 0){
				log.trace("Ring indicator is High but there is no data.  Waiting for data...");
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			log.trace("Purge:  getting data..");



			while (serialInput.available() > 0 ){
				int readint = serialInput.read();

				switch (readint){
				case CLOCK_SET_REQ:
					setClock();
					break;
				case DATA_READY_REQ:
					receiveData();
					break;
				default:
					log.warn("Unexpected data received from X10 interface: " + Integer.toHexString(readint));
				}
				
				// Wait a while before rechecking to give interface time to switch of Ring Indicator.
				while (serialPort.isRI() && serialInput.available() <= 0){
					log.trace("Ring indicator is High but there is no data.  Waiting for data...");
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			log.trace("Purging input stream complete.  Ring Indicator has cleared and no data is left to read.");

		}


	}

	/**
	 * Sets the internal clock on the X10 interface
	 * @throws IOException 
	 */
	private void setClock() throws IOException {
		log.debug("Setting clock in X10 interface");
		Calendar cal = Calendar.getInstance();

		int[] clockData = new int[7]; 
		clockData[0] = CLOCK_SET_HEAD;
		clockData[1] = cal.get(Calendar.SECOND);
		clockData[2] = cal.get(Calendar.MINUTE) + (cal.get(Calendar.HOUR) % 2) * 60;
		clockData[3] = cal.get(Calendar.HOUR_OF_DAY)/2;

		clockData[4] = cal.get(Calendar.DAY_OF_YEAR) & 256;
		clockData[5] = ((cal.get(Calendar.DAY_OF_YEAR) & 0x100) >> 1 ) | (0x80 >> cal.get(Calendar.DAY_OF_WEEK));

		// There are other flags in this final byte to do with clearing timer, battery timer and monitored status.  
		// I've no idea what they are, so have left them unset.
		clockData[6] = MONITORED_HOUSE_CODE;

		sendData(clockData);


	}

	/**
	 * Process data that the X10 interface is waiting to send to the PC
	 * @throws IOException
	 */
	private void receiveData() throws IOException {
		log.debug("Receiving X10 data from interface");

		// Send acknowledgement to interface
		serialOutput.write(DATA_READY_HEAD);
		
		int length = serialInput.read();

		for (int i = 0; i<length; i++){
			int recvByte = serialInputStr.read();
			log.debug("          Received X10 data: " + Integer.toHexString(recvByte));
		}


	}
	
	/**
	 * Disconnect from hardware
	 */
	public void close() {
		killThread = true;
		try {
			this.wait((IO_RECONNECT_INTERVAL + IO_PORT_OPEN_TIMEOUT) * 2);
		} catch (InterruptedException e) {
			// Nothing to do.
		}
		if (serialPort != null) {
			serialPort.close();
		}
	}

}
