package postilion.realtime.pynid;

import postilion.realtime.sdk.ipc.IClientEndpoint;
import postilion.realtime.sdk.ipc.IEndpoint;
import postilion.realtime.sdk.ipc.Sap;
import postilion.realtime.sdk.ipc.TcpClientSap;
import postilion.realtime.sdk.util.Processor;
import postilion.realtime.sdk.util.Queue;
import postilion.realtime.sdk.util.Timer;
import postilion.realtime.sdk.util.Timer.ICallbackUser;

//A processor for connecting to external payment engine
public class ComProcessor extends Processor implements ICallbackUser{
	
	private TcpClientSap sap;
	private IEndpoint endpoint = null;
	private static boolean is_connected = false;
	private static boolean is_connecting = false;
	private Object mutex=new Object();
	private byte[] receivedData=null;
	
	private String local_host;
	private String local_port;
	private String remote_host;
	private String remote_port;
	private Timer timer;
	private boolean isTimedout;
	
	public ComProcessor(String local_host, String local_port, String remote_host,  String remote_port)
	{
		super(new Queue());
		this.local_host = local_host;
		this.local_port = local_port;
		this.remote_host = remote_host;
		this.remote_port = remote_port;
		this.setDaemon(true);
		this.start();
	}
	
	public void connect()
	{
		this.sap = new TcpClientSap("To pyNID Server",getQueue(),null);
		this.sap.start();
		if(!is_connecting && !is_connected)
		{
			IClientEndpoint endpoint = 
				(IClientEndpoint)(sap.getEndpoint(
					local_host,
					Integer.parseInt(local_port),
					remote_host, 
					Integer.parseInt(remote_port)));

			endpoint.connect();
			is_connecting = true;
		}
	}
	
	public boolean processEvent( Object event )
	{
		if ( event instanceof Sap.ConnectEvent )
		{			
			this.processConnectEvent( ( Sap.ConnectEvent )event );
		}
		else if ( event instanceof Sap.DisconnectEvent )
		{
			this.processDisconnectEvent( ( Sap.DisconnectEvent )event );
		}
		else if ( event instanceof Sap.DataEvent )
		{
			this.processDataEvent( ( Sap.DataEvent )event );
		}
		return true;
	}
	
	private void processConnectEvent( Sap.ConnectEvent event )
	{
		is_connected = true;
		is_connecting = false;
		endpoint = event.endpoint;																				
	}
	
	// Automatic re-connect every 3 secs
	private void processDisconnectEvent( Sap.DisconnectEvent event )
	{
		is_connected = false;
		is_connecting = false;
		try
		{
			Thread.sleep(3000);
		}
		catch ( InterruptedException i )
		{
		}
		connect();
	}
	
	private void processDataEvent(Sap.DataEvent event)
	{	
		synchronized (mutex){
			receivedData = event.data;
			mutex.notifyAll();
		}
	}
	
	public synchronized void sendMessage(byte[] data) throws Exception
	{
		if (is_connected)
		{
			endpoint.send(data);
		}
		else
		{
			throw new Exception(PYSERVER_NOT_CONNECTED);
		}
	}
	
	public byte[] receiveMessage() throws Exception
	{
		if (!is_connected)
		{
			throw new Exception(PYSERVER_NOT_CONNECTED);
		}
		startTimer();
		receivedData = null;	
		isTimedout = false;
		while (receivedData == null && isTimedout == false){
			synchronized (mutex){
				try {
					mutex.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		if (isTimedout == true){
			throw new Exception(PYSERVER_TIMED_OUT);
		}
		timer.stop();
		return receivedData;
	}
	
	public void processCallbackTimerExpired(Object user_ref)
	{
		isTimedout = true;
		synchronized (mutex){
			mutex.notifyAll();
		}
	}
	
	private void startTimer(){
		timer = new Timer (this, 1000, null);
	}
	
	public static final String PYSERVER_NOT_CONNECTED = "pyNidServer is not connected";
	public static final String PYSERVER_TIMED_OUT = "pyNidServer timed out";
}
