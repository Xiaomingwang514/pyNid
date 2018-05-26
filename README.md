######## 
pyNid
########

A Postilion Realtime NID (Node Integration Driver) to branch financial transactions for external processing such as pystilion.

########
Prerequisites & Build
########

pyNid has been built with Postilion Realtime v5.5.
It should work with Realtime v5.x.

######## 
Configuration
######## 

1. After building the Java files, copy the class folder to Realtime class folder i.e. (C:\Program Files\Postilion\realtime\java\classes\postilion\realtime)
2. Adding the NID to cfg_custom_classes table. i.e. ("NODE INTEGRATION:PYNID", "NODE INTEGRATION","PYNID","postilion.realtime.pynid.PyNid","NULL","PYNID")
3. Config the NID for required source or sink interchanges with following space separated parameters
	a) local host or IP address
	b) local port
	c) remote host or IP address
	d) remote port

######## 
Funcations
########

The NID would forward the ISO8583 Postilion transactions to configured remote entity for processing.
It can be placed at either source side or sink side.
Before sending transaction out to remote entity, it marks following tags in Postilion structure data
	a) "PYNID_NAME" = the name of the interchange
	b) "PYNID_DIRECTION" = the direction of the transaction, which can be either "FROM_REMOTE" or "FROM_TM"

After transaction being sent out, the NID wait for one second to receive response.
The response message from external entity can contain an action tag in structure data to instruct further processing.
The tag is
	a) "PYNID_ACTION" = "TO_TM" or "TO_REMOTE"
The Nid will send the transaction according to the tag returned.
If no tag is included in the structure data, the NID will forward the transaction based on the original route.


########
To Do
########

Comments in the code.
Testing cases.