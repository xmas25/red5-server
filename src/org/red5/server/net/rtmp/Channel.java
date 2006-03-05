package org.red5.server.net.rtmp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.InPacket;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.PacketHeader;
import org.red5.server.net.rtmp.message.Status;
import org.red5.server.service.Call;
import org.red5.server.stream.Stream;

public class Channel {

	protected static Log log =
        LogFactory.getLog(Channel.class.getName());
	
	private BaseConnection connection = null;
	private byte id = 0;
	//private Stream stream;
	private PacketHeader lastReadHeader = null;
	private PacketHeader lastWriteHeader = null;
	private InPacket inPacket = null;
	private OutPacket outPacket = null;
	private int source = 0;

	public Channel(BaseConnection conn, byte channelId){
		connection = conn;
		id = channelId;
	}
	
	public byte getId(){
		return id;
	}
	
	/*
	public Stream getStream() {
		return stream;
	}
	*/

	public InPacket getInPacket() {
		return inPacket;
	}

	public void setInPacket(InPacket incommingPacket) {
		this.inPacket = incommingPacket;
	}

	public OutPacket getOutPacket() {
		return outPacket;
	}

	public void setOutPacket(OutPacket outgoingPacket) {
		this.outPacket = outgoingPacket;
	}

	public PacketHeader getLastReadHeader() {
		return lastReadHeader;
	}

	public void setLastReadHeader(PacketHeader lastReadHeader) {
		this.lastReadHeader = lastReadHeader;
	}

	public PacketHeader getLastWriteHeader() {
		return lastWriteHeader;
	}

	public void setLastWriteHeader(PacketHeader lastWriteHeader) {
		this.lastWriteHeader = lastWriteHeader;
	}
	
	public void write(Message message){
		final Stream stream = connection.getStreamByChannelId(id);
		/*
		final int streamId = (
				stream==null || (
						message.getDataType() != Constants.TYPE_AUDIO_DATA && 
						message.getDataType() != Constants.TYPE_VIDEO_DATA )
				) ? 0 : stream.getStreamId();
				*/
		final int streamId = ( stream==null ) ? 0 : stream.getStreamId();
		write(message, message.getTimestamp(), streamId);
	}
	
	private void write(Message message, int timer, int streamId){
		
		final OutPacket packet = new OutPacket();
		final PacketHeader header = new PacketHeader();
		
		header.setChannelId(id);
		header.setTimer(timer);
		header.setStreamId(streamId);
		header.setDataType(message.getDataType());
		
		packet.setDestination(header);
		packet.setMessage(message);
		
		connection.write(packet);
		
	}

	public void sendStatus(Status status) {
		final Call call = new Call(null,"onStatus",new Object[]{status});
		final boolean andReturn = !status.getCode().equals(Status.NS_DATA_START);
		final Invoke invoke = (andReturn) ? new Invoke() : new Notify();
		invoke.setInvokeId(1);
		invoke.setCall(call);
		write(invoke);
	}

}
