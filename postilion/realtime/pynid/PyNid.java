package postilion.realtime.pynid;

import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.StructuredData;
import postilion.realtime.sdk.node.AIntegrationDriver;
import postilion.realtime.sdk.node.AIntegrationDriverEnvironment;
import postilion.realtime.sdk.node.Action;

public class PyNid extends AIntegrationDriver
{
	protected String local_host;
	protected String local_port;
	protected String remote_host;
	protected String remote_port;
	protected String interchangeName;
	private ComProcessor com_processor;
	
	@Override
	public void init(
			AIntegrationDriverEnvironment nodeApplication,
			String integrationDriverParameters, 
			String customClassParameters)
	throws Exception
	{
		if (integrationDriverParameters == null
				|| BLANK_STR.equals(integrationDriverParameters))
		{
			return;
		}

		// Set the driver parameters.
		String[] splitParams = integrationDriverParameters.split(SPACE_DELIM);
		if (splitParams.length != _4_PARAMS_EXPECTED)
		{
			throw new Exception(
					String.format(
							PARAM_EXCEPTION_STR, 
							_4_PARAMS_EXPECTED, 
							splitParams.length));
		}
		else
		{
			interchangeName=nodeApplication.getName();
			local_host = splitParams[0];
			local_port = splitParams[1];
			remote_host = splitParams[2];
			remote_port = splitParams[3];
		}
		com_processor = new ComProcessor(local_host, local_port , remote_host, remote_port);
		com_processor.connect();
	}
	
	@Override
	public void processResyncCommand(
			AIntegrationDriverEnvironment nodeApplication,
			String integrationDriverParameters, 
			String customClassParameters)
	throws Exception
	{
		init(
			nodeApplication, 
			integrationDriverParameters,
			customClassParameters);
	}

	@Override
	public Action processMsgFromRemote(
			AIntegrationDriverEnvironment nodeApplication,
			Iso8583Post msgFromRemote) 
	throws Exception
	{
		Iso8583Post newMsg=new Iso8583Post();
		StructuredData structured_data;
		if (msgFromRemote.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA))
		{
			structured_data = msgFromRemote.getStructuredData();
		}
		else
		{
			structured_data = new StructuredData();
		}
		structured_data.put(SD_PYNID_ACTION, null);
		structured_data.put(SD_PYNID_NAME, interchangeName);
		structured_data.put(SD_PYNID_DIRECTION, SD_PYNID_DIRECTION_FROM_REMOTE);
		msgFromRemote.putStructuredData(structured_data);
		com_processor.sendMessage(msgFromRemote.toMsg());
		newMsg.fromMsg(com_processor.receiveMessage());
		Action action = overrideAction(newMsg);
		if (null != action)
		{
			return action;
		}
		return new Action(newMsg, null, null, null);
	}
	
	@Override
	public Action processMessageFromTranmgrSourceNode(
			AIntegrationDriverEnvironment node_application,
			Iso8583Post msg)
			throws Exception
	{
			return processMsgFromTM(node_application, msg);
	}
	
	@Override
	public Action processMessageFromTranmgrSinkNode(
			AIntegrationDriverEnvironment node_application,
			Iso8583Post msg)
			throws Exception
	{
			return processMsgFromTM(node_application, msg);
	}
	
	private Action processMsgFromTM(
			AIntegrationDriverEnvironment nodeApplication,
			Iso8583Post msgFromTM) 
	throws Exception
	{
		Iso8583Post newMsg=new Iso8583Post();
		StructuredData structured_data;
		if (msgFromTM.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA))
		{
			structured_data = msgFromTM.getStructuredData();
		}
		else
		{
			structured_data = new StructuredData();
		}
		structured_data.put(SD_PYNID_ACTION, null);
		structured_data.put(SD_PYNID_NAME, interchangeName);
		structured_data.put(SD_PYNID_DIRECTION, SD_PYNID_DIRECTION_FROM_TM);
		msgFromTM.putStructuredData(structured_data);
		com_processor.sendMessage(msgFromTM.toMsg());
		newMsg.fromMsg(com_processor.receiveMessage());
		Action action = overrideAction(newMsg);
		if (null != action)
		{
			return action;
		}
		return new Action(null, newMsg, null, null);
	}
	
	private Action overrideAction(Iso8583Post msg) throws Exception{
		StructuredData structured_data;
		String action;
		if (msg.isPrivFieldSet(Iso8583Post.PrivBit._022_STRUCT_DATA))
		{
			structured_data = msg.getStructuredData();
		}
		else {
			return null;
		}
		action = structured_data.get(SD_PYNID_ACTION);
		if (null == action){
			return null;
		}
		if (action.equals(SD_PYNID_ACTION_TO_TM)){
			return new Action(msg, null, null, null);
		}
		else if (action.equals(SD_PYNID_ACTION_TO_REMOTE)){
			return new Action(null, msg, null, null);
		}
		else
		{
			return null;
		}
	}
	
	public static final String BLANK_STR = "";
	public static final String SPACE_DELIM = " ";
	public static final int _4_PARAMS_EXPECTED = 4;
	public static final String PARAM_EXCEPTION_STR = "Invalid number of Integration Driver parameters. Expected [%s], " + "got [%s].";
	public static final String SD_PYNID_NAME = "PYNID_NAME";
	public static final String SD_PYNID_DIRECTION = "PYNID_DIRECTION";
	public static final String SD_PYNID_DIRECTION_FROM_REMOTE = "FROM_REMOTE";
	public static final String SD_PYNID_DIRECTION_FROM_TM = "FROM_TM";
	public static final String SD_PYNID_ACTION = "PYNID_ACTION";
	public static final String SD_PYNID_ACTION_TO_TM = "TO_TM";
	public static final String SD_PYNID_ACTION_TO_REMOTE = "TO_REMOTE";
}

