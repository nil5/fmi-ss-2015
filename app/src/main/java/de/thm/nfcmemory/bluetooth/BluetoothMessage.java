package de.thm.nfcmemory.bluetooth;

public abstract class BluetoothMessage {
	public static final byte OK = 10;
	public static final byte HELLO = 11;
	
	public static final byte START = 20;
	public static final byte PAUSE = 21;
	public static final byte STOP = 22;
	
	public static final byte DISCONNECT = 80;
}
