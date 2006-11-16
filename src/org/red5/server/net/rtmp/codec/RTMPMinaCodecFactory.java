package org.red5.server.net.rtmp.codec;

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

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.protocol.SimpleProtocolCodecFactory;
import org.red5.server.net.protocol.SimpleProtocolDecoder;
import org.red5.server.net.protocol.SimpleProtocolEncoder;

public class RTMPMinaCodecFactory implements ProtocolCodecFactory,
		SimpleProtocolCodecFactory {

	protected Deserializer deserializer = null;

	protected Serializer serializer = null;

	protected RTMPMinaProtocolDecoder decoder;

	protected RTMPMinaProtocolEncoder encoder;

	public void init() {
		decoder = new RTMPMinaProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RTMPMinaProtocolEncoder();
		encoder.setSerializer(serializer);
	}

	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	public ProtocolDecoder getDecoder() {
		return decoder;
	}

	public ProtocolEncoder getEncoder() {
		return encoder;
	}

	public SimpleProtocolDecoder getSimpleDecoder() {
		return decoder;
	}

	public SimpleProtocolEncoder getSimpleEncoder() {
		return encoder;
	}

}