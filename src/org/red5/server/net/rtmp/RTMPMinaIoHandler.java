package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;

public class RTMPMinaIoHandler extends IoHandlerAdapter {

	protected static Log log = LogFactory.getLog(RTMPMinaIoHandler.class
			.getName());

	protected IRTMPHandler handler;
	protected boolean mode = RTMP.MODE_SERVER;

	public void setHandler(IRTMPHandler handler) {
		this.handler = handler;
	}
	
	public void setMode(boolean mode) {
		this.mode = mode;
	}

	private ProtocolCodecFactory codecFactory = null;

	public void setCodecFactory(ProtocolCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	//	 ------------------------------------------------------------------------------

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		log.debug("Exception caught", cause);
	}

	@Override
	public void messageReceived(IoSession session, Object in) throws Exception {
		log.debug("messageRecieved");
		final RTMPMinaConnection conn = (RTMPMinaConnection) session
				.getAttachment();
		final ProtocolState state = (ProtocolState) session
				.getAttribute(ProtocolState.SESSION_KEY);

		if (in instanceof ByteBuffer) {
			rawBufferRecieved(state, (ByteBuffer) in, session);
			return;
		}

		handler.messageReceived(conn, state, in);
	}

	private void rawBufferRecieved(ProtocolState state, ByteBuffer in,
			IoSession session) {

		final RTMP rtmp = (RTMP) state;
		if (rtmp.getMode()==RTMP.MODE_SERVER) {
			if (rtmp.getState() != RTMP.STATE_HANDSHAKE) {
				log.warn("Raw buffer after handshake, something odd going on");
			}

			if (log.isDebugEnabled()){
				log.debug("Handshake 2nd phase");
				log.debug("handshake size:"+in.remaining());
			}
			ByteBuffer out = ByteBuffer.allocate((Constants.HANDSHAKE_SIZE*2)+1);
			out.put((byte)0x03);
			out.fill((byte)0x00,Constants.HANDSHAKE_SIZE);
			out.put(in);
			out.flip();
			//in.release();
			session.write(out); 
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Handshake 3d phase");
				log.debug("handshake size:"+in.remaining());
			}
			in.skip(1);
			ByteBuffer out = ByteBuffer.allocate(Constants.HANDSHAKE_SIZE);
			int limit=in.limit();
			in.limit(in.position()+Constants.HANDSHAKE_SIZE);
			out.put(in); 
			out.flip();
			in.limit(limit);
			in.skip(Constants.HANDSHAKE_SIZE);
			session.write(out);
		}
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		log.debug("messageSent");
		final RTMP rtmp = (RTMP) session.getAttribute(RTMP.SESSION_KEY);
		final RTMPMinaConnection conn = (RTMPMinaConnection) session
				.getAttachment();
		handler.messageSent(conn, message);
		if (mode == RTMP.MODE_CLIENT) {
			if (message instanceof ByteBuffer) {
				if (((ByteBuffer)message).limit() == Constants.HANDSHAKE_SIZE) {
					handler.connectionOpened((RTMPMinaConnection)session.getAttachment(), (RTMP)session.getAttribute(RTMP.SESSION_KEY));
				}
			}
		}
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		// TODO Auto-generated method stub

		SocketSessionConfig cfg = (SocketSessionConfig) session.getConfig();
		//cfg.setReceiveBufferSize(256);
		//cfg.setSendBufferSize(256);
		log.info("Is tcp delay enabled: " + cfg.isTcpNoDelay());
		cfg.setTcpNoDelay(true);
		super.sessionOpened(session);

		RTMP rtmp=(RTMP)session.getAttribute(RTMP.SESSION_KEY);
		if (rtmp.getMode()==RTMP.MODE_CLIENT) {
			if (log.isDebugEnabled()){
				log.debug("Handshake 1st phase");
			}
			ByteBuffer out = ByteBuffer.allocate(Constants.HANDSHAKE_SIZE+1);
			out.put((byte)0x03);
			out.fill((byte)0x00,Constants.HANDSHAKE_SIZE);
			out.flip();
			session.write(out);
		}
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		final RTMP rtmp = (RTMP) session
				.getAttribute(ProtocolState.SESSION_KEY);
		ByteBuffer buf = (ByteBuffer) session.getAttribute("buffer");
		if (buf != null) {
			buf.release();
		}
		final RTMPMinaConnection conn = (RTMPMinaConnection) session
				.getAttachment();
		this.handler.connectionClosed(conn, rtmp);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Session created");
		}

		// moved protocol state from connection object to rtmp object
		session.setAttribute(ProtocolState.SESSION_KEY, new RTMP(mode));

		session.getFilterChain().addFirst("protocolFilter",
				new ProtocolCodecFilter(this.codecFactory));
		if (log.isDebugEnabled()) {
			session.getFilterChain().addLast("logger", new LoggingFilter());
		}
		session.setAttachment(new RTMPMinaConnection(session));
	}

}