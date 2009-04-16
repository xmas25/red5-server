package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.mina.core.buffer.IoBuffer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates the second 1536 byte chunk in the RTMP handshake response for
 * compatibility with Flash 9,0,124,0. Clients that require this send a nonzero
 * value as the fifth byte of the handshake request.
 * 
 * <br/>
 * This class is based on the Ruby handshaking code from Takuma Mori.
 * <br />
 * 
 * @author Jacinto Shy II (jacinto.m.shy@ieee.org)
 * @author Steven Zimmer (stevenlzimmer@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPHandshake {

	protected static Logger log = LoggerFactory.getLogger(RTMPHandshake.class);	
	
	private static final byte[] HANDSHAKE_SERVER_BYTES = { (byte) 0x01,
			(byte) 0x86, (byte) 0x4f, (byte) 0x7f, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x6b, (byte) 0x04, (byte) 0x67,
			(byte) 0x52, (byte) 0xa2, (byte) 0x70, (byte) 0x5b, (byte) 0x51,
			(byte) 0xa2, (byte) 0x89, (byte) 0xca, (byte) 0xcc, (byte) 0x8e,
			(byte) 0x70, (byte) 0xf0, (byte) 0x06, (byte) 0x70, (byte) 0x0e,
			(byte) 0xd7, (byte) 0xb3, (byte) 0x73, (byte) 0x7f, (byte) 0x07,
			(byte) 0xc1, (byte) 0x72, (byte) 0xd6, (byte) 0xcb, (byte) 0x4c,
			(byte) 0xc0, (byte) 0x45, (byte) 0x0f, (byte) 0xf5, (byte) 0x4f,
			(byte) 0xec, (byte) 0xd0, (byte) 0x2f, (byte) 0x46, (byte) 0x2b,
			(byte) 0x76, (byte) 0x10, (byte) 0x92, (byte) 0x1b, (byte) 0x0e,
			(byte) 0xb6, (byte) 0xed, (byte) 0x71, (byte) 0x73, (byte) 0x45,
			(byte) 0xc1, (byte) 0xc6, (byte) 0x26, (byte) 0x0c, (byte) 0x69,
			(byte) 0x59, (byte) 0x7b, (byte) 0xbb, (byte) 0x53, (byte) 0xb9,
			(byte) 0x10, (byte) 0x4d, (byte) 0xea, (byte) 0xc1, (byte) 0xe7,
			(byte) 0x7b, (byte) 0x70, (byte) 0xde, (byte) 0xdc, (byte) 0xf8,
			(byte) 0x84, (byte) 0x90, (byte) 0xbf, (byte) 0x80, (byte) 0xe8,
			(byte) 0x85, (byte) 0xb2, (byte) 0x46, (byte) 0x2c, (byte) 0x78,
			(byte) 0xa1, (byte) 0x85, (byte) 0x01, (byte) 0x8f, (byte) 0x8b,
			(byte) 0x05, (byte) 0x3f, (byte) 0xa1, (byte) 0x0c, (byte) 0x1a,
			(byte) 0x78, (byte) 0x70, (byte) 0x8c, (byte) 0x8e, (byte) 0x77,
			(byte) 0x67, (byte) 0xbc, (byte) 0x19, (byte) 0x2f, (byte) 0xab,
			(byte) 0x26, (byte) 0xa1, (byte) 0x7e, (byte) 0x88, (byte) 0xd8,
			(byte) 0xce, (byte) 0x24, (byte) 0x63, (byte) 0x21, (byte) 0x75,
			(byte) 0x3a, (byte) 0x5a, (byte) 0x6f, (byte) 0xc2, (byte) 0xa1,
			(byte) 0x2d, (byte) 0x4f, (byte) 0x64, (byte) 0xb7, (byte) 0x7b,
			(byte) 0xf7, (byte) 0xef, (byte) 0xda, (byte) 0x45, (byte) 0xb2,
			(byte) 0x51, (byte) 0xfd, (byte) 0xcb, (byte) 0x74, (byte) 0x49,
			(byte) 0xfd, (byte) 0x63, (byte) 0x8b, (byte) 0x88, (byte) 0xfb,
			(byte) 0xde, (byte) 0x5a, (byte) 0x3b, (byte) 0xab, (byte) 0x7f,
			(byte) 0x75, (byte) 0x25, (byte) 0xbb, (byte) 0x35, (byte) 0x51,
			(byte) 0x03, (byte) 0x81, (byte) 0x12, (byte) 0xff, (byte) 0x66,
			(byte) 0x02, (byte) 0x3d, (byte) 0x88, (byte) 0xdc, (byte) 0x66,
			(byte) 0xa2, (byte) 0xfb, (byte) 0x09, (byte) 0x24, (byte) 0x9d,
			(byte) 0x86, (byte) 0xfd, (byte) 0xc4, (byte) 0x00, (byte) 0xc2,
			(byte) 0x8b, (byte) 0x6f, (byte) 0xb7, (byte) 0xb2, (byte) 0x15,
			(byte) 0x10, (byte) 0xc0, (byte) 0x1b, (byte) 0x71, (byte) 0xa8,
			(byte) 0x3e, (byte) 0x88, (byte) 0xeb, (byte) 0x7e, (byte) 0xf3,
			(byte) 0xb2, (byte) 0xe3, (byte) 0xe8, (byte) 0x3c, (byte) 0x00,
			(byte) 0x9b, (byte) 0x26, (byte) 0xba, (byte) 0xb4, (byte) 0x5f,
			(byte) 0x2c, (byte) 0x36, (byte) 0xf3, (byte) 0x4a, (byte) 0x59,
			(byte) 0x09, (byte) 0x1b, (byte) 0xe5, (byte) 0x00, (byte) 0x9d,
			(byte) 0xe4, (byte) 0x66, (byte) 0x4d, (byte) 0x05, (byte) 0x66,
			(byte) 0xd0, (byte) 0xd1, (byte) 0xd6, (byte) 0x94, (byte) 0x4f,
			(byte) 0x64, (byte) 0xa1, (byte) 0x2e, (byte) 0x8d, (byte) 0x2f,
			(byte) 0xb0, (byte) 0x06, (byte) 0x01, (byte) 0xb3, (byte) 0x00,
			(byte) 0x3d, (byte) 0x77, (byte) 0xcd, (byte) 0x1b, (byte) 0xdd,
			(byte) 0xcc, (byte) 0xbf, (byte) 0xe9, (byte) 0xcd, (byte) 0x1a,
			(byte) 0x6b, (byte) 0x68, (byte) 0xdd, (byte) 0x1c, (byte) 0x7b,
			(byte) 0xfd, (byte) 0x2e, (byte) 0xb1, (byte) 0x8b, (byte) 0x45,
			(byte) 0xfd, (byte) 0x5b, (byte) 0x48, (byte) 0x52, (byte) 0x03,
			(byte) 0x01, (byte) 0xe8, (byte) 0xf1, (byte) 0x0f, (byte) 0xe7,
			(byte) 0x27, (byte) 0xfc, (byte) 0x2a, (byte) 0x52, (byte) 0x7c,
			(byte) 0x14, (byte) 0x22, (byte) 0x8b, (byte) 0x74, (byte) 0xbd,
			(byte) 0xd9, (byte) 0x97, (byte) 0x63, (byte) 0xef, (byte) 0xfa,
			(byte) 0xa3, (byte) 0xd9, (byte) 0x21, (byte) 0x12, (byte) 0x0b,
			(byte) 0x04, (byte) 0x62, (byte) 0x02, (byte) 0x98, (byte) 0x41,
			(byte) 0xf2, (byte) 0xb4, (byte) 0xc3, (byte) 0xe3, (byte) 0xe2,
			(byte) 0x2b, (byte) 0x2a, (byte) 0xff, (byte) 0xca, (byte) 0xb4,
			(byte) 0x48, (byte) 0x1e, (byte) 0x82, (byte) 0x50, (byte) 0x90,
			(byte) 0x94, (byte) 0x37, (byte) 0x24, (byte) 0x7e, (byte) 0xa1,
			(byte) 0x03, (byte) 0x1a, (byte) 0xf0, (byte) 0x9f, (byte) 0x2b,
			(byte) 0xbe, (byte) 0x64, (byte) 0xe5, (byte) 0x53, (byte) 0xb9,
			(byte) 0xb6, (byte) 0x43, (byte) 0x8e, (byte) 0x26, (byte) 0x6c,
			(byte) 0x63, (byte) 0x72, (byte) 0x8d, (byte) 0xb7, (byte) 0x7c,
			(byte) 0xb8, (byte) 0x21, (byte) 0x8f, (byte) 0xbb, (byte) 0x1c,
			(byte) 0x2a, (byte) 0x4e, (byte) 0xc7, (byte) 0xec, (byte) 0xa7,
			(byte) 0xa9, (byte) 0xbc, (byte) 0x15, (byte) 0x10, (byte) 0xe9,
			(byte) 0x4c, (byte) 0x46, (byte) 0xa5, (byte) 0x60, (byte) 0xa9,
			(byte) 0x71, (byte) 0x41, (byte) 0xdd, (byte) 0x25, (byte) 0xf5,
			(byte) 0xc1, (byte) 0xf6, (byte) 0xbd, (byte) 0x75, (byte) 0x1f,
			(byte) 0xb0, (byte) 0x15, (byte) 0xe0, (byte) 0xed, (byte) 0xc2,
			(byte) 0x4b, (byte) 0xac, (byte) 0xf1, (byte) 0xc8, (byte) 0xef,
			(byte) 0xa3, (byte) 0x44, (byte) 0xbe, (byte) 0x90, (byte) 0xab,
			(byte) 0x77, (byte) 0x28, (byte) 0xbf, (byte) 0xc0, (byte) 0xe0,
			(byte) 0x63, (byte) 0xaf, (byte) 0xd9, (byte) 0x07, (byte) 0x9d,
			(byte) 0x93, (byte) 0x16, (byte) 0x90, (byte) 0x7a, (byte) 0xe2,
			(byte) 0xb4, (byte) 0xe8, (byte) 0xe2, (byte) 0x3e, (byte) 0x4b,
			(byte) 0x18, (byte) 0x5f, (byte) 0x3e, (byte) 0x87, (byte) 0x09,
			(byte) 0xbe, (byte) 0x36, (byte) 0xd0, (byte) 0x8f, (byte) 0x7c,
			(byte) 0x22, (byte) 0x13, (byte) 0x9f, (byte) 0xc5, (byte) 0x78,
			(byte) 0xe0, (byte) 0x54, (byte) 0x4c, (byte) 0xa7, (byte) 0x77,
			(byte) 0x3f, (byte) 0xdf, (byte) 0x87, (byte) 0x4a, (byte) 0x28,
			(byte) 0x7b, (byte) 0x47, (byte) 0x80, (byte) 0x6a, (byte) 0xf0,
			(byte) 0x50, (byte) 0xcc, (byte) 0xde, (byte) 0x4c, (byte) 0x44,
			(byte) 0x41, (byte) 0x74, (byte) 0x3d, (byte) 0x03, (byte) 0x37,
			(byte) 0x8b, (byte) 0xbf, (byte) 0x79, (byte) 0x5b, (byte) 0x8c,
			(byte) 0xb0, (byte) 0x2f, (byte) 0x6e, (byte) 0x9c, (byte) 0x98,
			(byte) 0x29, (byte) 0x22, (byte) 0x49, (byte) 0x2f, (byte) 0xc9,
			(byte) 0x6d, (byte) 0xf1, (byte) 0x08, (byte) 0xc4, (byte) 0x4f,
			(byte) 0xb1, (byte) 0x91, (byte) 0xb3, (byte) 0xee, (byte) 0x57,
			(byte) 0xc1, (byte) 0x17, (byte) 0x5d, (byte) 0xd0, (byte) 0xe8,
			(byte) 0x19, (byte) 0xfb, (byte) 0x9b, (byte) 0xd6, (byte) 0xa8,
			(byte) 0x56, (byte) 0x92, (byte) 0x04, (byte) 0x4c, (byte) 0x0e,
			(byte) 0xe0, (byte) 0x52, (byte) 0x93, (byte) 0x9a, (byte) 0xec,
			(byte) 0xed, (byte) 0xf3, (byte) 0xf7, (byte) 0xef, (byte) 0xd7,
			(byte) 0x33, (byte) 0xe3, (byte) 0xcd, (byte) 0xc7, (byte) 0x4b,
			(byte) 0xac, (byte) 0xb7, (byte) 0xa9, (byte) 0xa5, (byte) 0x13,
			(byte) 0x09, (byte) 0x6c, (byte) 0x94, (byte) 0x49, (byte) 0x72,
			(byte) 0x03, (byte) 0xf3, (byte) 0xcf, (byte) 0x15, (byte) 0x31,
			(byte) 0xbc, (byte) 0xb5, (byte) 0x68, (byte) 0xc2, (byte) 0x49,
			(byte) 0xe1, (byte) 0x6e, (byte) 0x7d, (byte) 0xcb, (byte) 0x4e,
			(byte) 0xec, (byte) 0xfc, (byte) 0xa7, (byte) 0xb7, (byte) 0xed,
			(byte) 0x1c, (byte) 0x02, (byte) 0x49, (byte) 0x0e, (byte) 0x7f,
			(byte) 0x25, (byte) 0xeb, (byte) 0xd1, (byte) 0x81, (byte) 0x81,
			(byte) 0xc0, (byte) 0xa7, (byte) 0x49, (byte) 0x32, (byte) 0x16,
			(byte) 0x11, (byte) 0x31, (byte) 0x59, (byte) 0x12, (byte) 0x43,
			(byte) 0xd3, (byte) 0xa6, (byte) 0x95, (byte) 0x4a, (byte) 0xc5,
			(byte) 0xfe, (byte) 0xdf, (byte) 0x14, (byte) 0xda, (byte) 0xa6,
			(byte) 0x5a, (byte) 0xc0, (byte) 0xd5, (byte) 0x6a, (byte) 0xaf,
			(byte) 0xb3, (byte) 0xde, (byte) 0x32, (byte) 0x2a, (byte) 0x13,
			(byte) 0x03, (byte) 0xd3, (byte) 0x10, (byte) 0x71, (byte) 0x0b,
			(byte) 0xc0, (byte) 0x1e, (byte) 0xcf, (byte) 0xdb, (byte) 0xaa,
			(byte) 0xcc, (byte) 0xa6, (byte) 0xb5, (byte) 0x65, (byte) 0x2e,
			(byte) 0xc4, (byte) 0x0b, (byte) 0x5c, (byte) 0xa7, (byte) 0x1c,
			(byte) 0x8b, (byte) 0x2d, (byte) 0x7f, (byte) 0xc0, (byte) 0x4c,
			(byte) 0x4a, (byte) 0xa4, (byte) 0x0b, (byte) 0xa0, (byte) 0x60,
			(byte) 0xc4, (byte) 0xcf, (byte) 0xb1, (byte) 0xbe, (byte) 0xe4,
			(byte) 0xe4, (byte) 0x50, (byte) 0xc9, (byte) 0xcc, (byte) 0xa0,
			(byte) 0xe8, (byte) 0x79, (byte) 0x12, (byte) 0xc4, (byte) 0xb4,
			(byte) 0x70, (byte) 0xf5, (byte) 0x84, (byte) 0x98, (byte) 0x83,
			(byte) 0xe2, (byte) 0xa9, (byte) 0x8f, (byte) 0xba, (byte) 0xff,
			(byte) 0x88, (byte) 0xa2, (byte) 0x21, (byte) 0xba, (byte) 0x00,
			(byte) 0x3d, (byte) 0xc4, (byte) 0x57, (byte) 0xe6, (byte) 0x6a,
			(byte) 0xf4, (byte) 0xdc, (byte) 0x01, (byte) 0x1e, (byte) 0xac,
			(byte) 0x0a, (byte) 0xcc, (byte) 0x49, (byte) 0xaf, (byte) 0x9c,
			(byte) 0xc7, (byte) 0xcd, (byte) 0xc1, (byte) 0x14, (byte) 0x6e,
			(byte) 0x12, (byte) 0x87, (byte) 0xf8, (byte) 0x22, (byte) 0xeb,
			(byte) 0xdf, (byte) 0x48, (byte) 0xda, (byte) 0x9f, (byte) 0xf2,
			(byte) 0x8b, (byte) 0xc1, (byte) 0xd2, (byte) 0x44, (byte) 0x94,
			(byte) 0xe4, (byte) 0x3e, (byte) 0xd0, (byte) 0x85, (byte) 0x56,
			(byte) 0xe4, (byte) 0x9a, (byte) 0xfd, (byte) 0xb9, (byte) 0xb3,
			(byte) 0x35, (byte) 0x38, (byte) 0x1d, (byte) 0x15, (byte) 0x4d,
			(byte) 0x28, (byte) 0xab, (byte) 0xb0, (byte) 0x17, (byte) 0xc0,
			(byte) 0x5b, (byte) 0x09, (byte) 0x86, (byte) 0x07, (byte) 0xfa,
			(byte) 0x69, (byte) 0xda, (byte) 0x65, (byte) 0xb8, (byte) 0xd9,
			(byte) 0x8f, (byte) 0xe6, (byte) 0xa1, (byte) 0x83, (byte) 0xab,
			(byte) 0x07, (byte) 0x98, (byte) 0x3c, (byte) 0x79, (byte) 0xf4,
			(byte) 0x59, (byte) 0x08, (byte) 0x8f, (byte) 0x83, (byte) 0x77,
			(byte) 0xbd, (byte) 0xa1, (byte) 0xa1, (byte) 0x76, (byte) 0x28,
			(byte) 0x9c, (byte) 0x0f, (byte) 0xcc, (byte) 0xdc, (byte) 0xce,
			(byte) 0x1f, (byte) 0x16, (byte) 0x02, (byte) 0x47, (byte) 0x98,
			(byte) 0x37, (byte) 0x96, (byte) 0x87, (byte) 0xb1, (byte) 0x70,
			(byte) 0x3a, (byte) 0xea, (byte) 0xa4, (byte) 0x65, (byte) 0x77,
			(byte) 0x98, (byte) 0x12, (byte) 0x27, (byte) 0x23, (byte) 0x47,
			(byte) 0xa8, (byte) 0x1b, (byte) 0x79, (byte) 0xc0, (byte) 0xec,
			(byte) 0x53, (byte) 0x32, (byte) 0xe6, (byte) 0xc1, (byte) 0x61,
			(byte) 0x7b, (byte) 0xa0, (byte) 0x98, (byte) 0x9f, (byte) 0xfc,
			(byte) 0x8d, (byte) 0xe8, (byte) 0x5c, (byte) 0xaf, (byte) 0xc6,
			(byte) 0xbf, (byte) 0x1f, (byte) 0xd1, (byte) 0x40, (byte) 0xdc,
			(byte) 0x28, (byte) 0x81, (byte) 0x34, (byte) 0x68, (byte) 0xb7,
			(byte) 0xda, (byte) 0x10, (byte) 0xf2, (byte) 0x63, (byte) 0x52,
			(byte) 0xcb, (byte) 0xe7, (byte) 0x18, (byte) 0x85, (byte) 0xd5,
			(byte) 0x99, (byte) 0x33, (byte) 0xee, (byte) 0x9a, (byte) 0x28,
			(byte) 0xfa, (byte) 0xdf, (byte) 0x6d, (byte) 0xcb, (byte) 0xc2,
			(byte) 0xce, (byte) 0x9d, (byte) 0xed, (byte) 0x9d, (byte) 0xbd,
			(byte) 0xfd, (byte) 0xd7, (byte) 0x0a, (byte) 0xe4, (byte) 0x89,
			(byte) 0xd3, (byte) 0x10, (byte) 0x9b, (byte) 0xdb, (byte) 0x6f,
			(byte) 0xd9, (byte) 0x37, (byte) 0x8b, (byte) 0x79, (byte) 0x9c,
			(byte) 0x94, (byte) 0xc2, (byte) 0x44, (byte) 0x31, (byte) 0x9f,
			(byte) 0x24, (byte) 0xef, (byte) 0x21, (byte) 0x1d, (byte) 0x5f,
			(byte) 0xd6, (byte) 0xf9, (byte) 0x99, (byte) 0x7b, (byte) 0xef,
			(byte) 0x59, (byte) 0xe6, (byte) 0xd6, (byte) 0xdd, (byte) 0x6a,
			(byte) 0x74, (byte) 0x82, (byte) 0xb8, (byte) 0xc5, (byte) 0xfb,
			(byte) 0x1d, (byte) 0xe8, (byte) 0xfc, (byte) 0x67, (byte) 0x4f,
			(byte) 0x4d, (byte) 0xb5, (byte) 0xcf, (byte) 0xa9, (byte) 0x52,
			(byte) 0x94, (byte) 0xc5, (byte) 0xb7, (byte) 0x32, (byte) 0xa0,
			(byte) 0x45, (byte) 0x0a, (byte) 0x35, (byte) 0x44, (byte) 0x59,
			(byte) 0x1e, (byte) 0x1c, (byte) 0x64, (byte) 0x89, (byte) 0x51,
			(byte) 0x80, (byte) 0x7b, (byte) 0x1f, (byte) 0x02, (byte) 0x77,
			(byte) 0x81, (byte) 0xfa, (byte) 0xe9, (byte) 0x26, (byte) 0x4c,
			(byte) 0x5f, (byte) 0xe2, (byte) 0x0d, (byte) 0x05, (byte) 0x55,
			(byte) 0xee, (byte) 0x71, (byte) 0x71, (byte) 0xfc, (byte) 0x35,
			(byte) 0x33, (byte) 0x22, (byte) 0x63, (byte) 0xf5, (byte) 0x36,
			(byte) 0x45, (byte) 0xf6, (byte) 0x2f, (byte) 0xd0, (byte) 0x13,
			(byte) 0xb7, (byte) 0x58, (byte) 0x4f, (byte) 0x35, (byte) 0x19,
			(byte) 0x59, (byte) 0x0a, (byte) 0xe5, (byte) 0xf8, (byte) 0x8a,
			(byte) 0x4c, (byte) 0x59, (byte) 0x32, (byte) 0xbf, (byte) 0xca,
			(byte) 0xb0, (byte) 0x06, (byte) 0xc2, (byte) 0x6c, (byte) 0xa9,
			(byte) 0x48, (byte) 0x5b, (byte) 0x4c, (byte) 0x76, (byte) 0x24,
			(byte) 0xae, (byte) 0x9d, (byte) 0x5b, (byte) 0x7b, (byte) 0x79,
			(byte) 0x38, (byte) 0x4e, (byte) 0x9e, (byte) 0x47, (byte) 0x12,
			(byte) 0x8a, (byte) 0xc6, (byte) 0xe0, (byte) 0x04, (byte) 0x37,
			(byte) 0x72, (byte) 0xdd, (byte) 0xaf, (byte) 0x3d, (byte) 0x0d,
			(byte) 0x68, (byte) 0x7e, (byte) 0xd8, (byte) 0x80, (byte) 0x7b,
			(byte) 0x07, (byte) 0x23, (byte) 0xce, (byte) 0x40, (byte) 0x4a,
			(byte) 0xed, (byte) 0x83, (byte) 0x55, (byte) 0x56, (byte) 0xfd,
			(byte) 0xdb, (byte) 0x95, (byte) 0xb3, (byte) 0x1c, (byte) 0x33,
			(byte) 0xf1, (byte) 0x43, (byte) 0xa8, (byte) 0x0e, (byte) 0x5e,
			(byte) 0x67, (byte) 0xd6, (byte) 0x3a, (byte) 0xd0, (byte) 0x89,
			(byte) 0x5e, (byte) 0x72, (byte) 0x77, (byte) 0x7f, (byte) 0x10,
			(byte) 0x3c, (byte) 0xc4, (byte) 0x7c, (byte) 0x9a, (byte) 0xa3,
			(byte) 0x55, (byte) 0xc5, (byte) 0xd3, (byte) 0x5b, (byte) 0x3a,
			(byte) 0xae, (byte) 0x12, (byte) 0x0c, (byte) 0x71, (byte) 0x73,
			(byte) 0xa0, (byte) 0x58, (byte) 0x90, (byte) 0x54, (byte) 0xa8,
			(byte) 0x1c, (byte) 0x31, (byte) 0x20, (byte) 0xdb, (byte) 0xde,
			(byte) 0xdd, (byte) 0x35, (byte) 0xb1, (byte) 0x09, (byte) 0xa2,
			(byte) 0xd0, (byte) 0x6e, (byte) 0x39, (byte) 0x39, (byte) 0xa5,
			(byte) 0x0a, (byte) 0x3d, (byte) 0x8a, (byte) 0x00, (byte) 0x4b,
			(byte) 0x95, (byte) 0x6f, (byte) 0x8c, (byte) 0x12, (byte) 0x41,
			(byte) 0xc6, (byte) 0x46, (byte) 0x10, (byte) 0x5e, (byte) 0x9d,
			(byte) 0x50, (byte) 0x85, (byte) 0x0e, (byte) 0x6b, (byte) 0x81,
			(byte) 0xa7, (byte) 0x3b, (byte) 0x35, (byte) 0xa6, (byte) 0x38,
			(byte) 0xf5, (byte) 0xc2, (byte) 0xba, (byte) 0x6c, (byte) 0x02,
			(byte) 0xda, (byte) 0x27, (byte) 0x29, (byte) 0x6e, (byte) 0xe9,
			(byte) 0x54, (byte) 0x41, (byte) 0xa4, (byte) 0x94, (byte) 0x75,
			(byte) 0xe8, (byte) 0x55, (byte) 0xc0, (byte) 0xe3, (byte) 0xc2,
			(byte) 0x91, (byte) 0x8a, (byte) 0x1d, (byte) 0xfb, (byte) 0x2b,
			(byte) 0xba, (byte) 0x43, (byte) 0xe7, (byte) 0x45, (byte) 0x85,
			(byte) 0xe8, (byte) 0x13, (byte) 0x07, (byte) 0x1d, (byte) 0x9c,
			(byte) 0x37, (byte) 0xa8, (byte) 0xf3, (byte) 0xca, (byte) 0xf4,
			(byte) 0x19, (byte) 0x77, (byte) 0xc4, (byte) 0x65, (byte) 0xd6,
			(byte) 0x18, (byte) 0x3e, (byte) 0x60, (byte) 0x08, (byte) 0x74,
			(byte) 0x49, (byte) 0xba, (byte) 0xc8, (byte) 0x86, (byte) 0x37,
			(byte) 0x8a, (byte) 0x0f, (byte) 0x79, (byte) 0x91, (byte) 0x53,
			(byte) 0x20, (byte) 0x23, (byte) 0x00, (byte) 0xb9, (byte) 0xc5,
			(byte) 0x1b, (byte) 0x01, (byte) 0xdd, (byte) 0x10, (byte) 0x34,
			(byte) 0x05, (byte) 0x42, (byte) 0xa0, (byte) 0x64, (byte) 0xab,
			(byte) 0x4d, (byte) 0x51, (byte) 0xf4, (byte) 0x53, (byte) 0x35,
			(byte) 0x18, (byte) 0xde, (byte) 0x20, (byte) 0x1f, (byte) 0xaa,
			(byte) 0xe2, (byte) 0x40, (byte) 0x0d, (byte) 0x6d, (byte) 0x77,
			(byte) 0x36, (byte) 0x1f, (byte) 0xee, (byte) 0x3a, (byte) 0x93,
			(byte) 0xdb, (byte) 0x1d, (byte) 0xd6, (byte) 0xa0, (byte) 0x23,
			(byte) 0xcc, (byte) 0xe6, (byte) 0xa8, (byte) 0x44, (byte) 0x8e,
			(byte) 0xae, (byte) 0x9c, (byte) 0xd7, (byte) 0x97, (byte) 0x6a,
			(byte) 0x99, (byte) 0xee, (byte) 0x40, (byte) 0x15, (byte) 0xd5,
			(byte) 0x5a, (byte) 0x6d, (byte) 0xf6, (byte) 0x9c, (byte) 0x2c,
			(byte) 0x52, (byte) 0xcd, (byte) 0xfa, (byte) 0xf4, (byte) 0xc8,
			(byte) 0x02, (byte) 0xee, (byte) 0xf2, (byte) 0x76, (byte) 0x8b,
			(byte) 0x49, (byte) 0x6d, (byte) 0x66, (byte) 0x83, (byte) 0x5f,
			(byte) 0xbe, (byte) 0x05, (byte) 0x8e, (byte) 0xf2, (byte) 0x27,
			(byte) 0x73, (byte) 0xdb, (byte) 0x00, (byte) 0xeb, (byte) 0x9a,
			(byte) 0xb4, (byte) 0xbf, (byte) 0x47, (byte) 0x9a, (byte) 0xbd,
			(byte) 0xf1, (byte) 0x4f, (byte) 0x70, (byte) 0xed, (byte) 0x33,
			(byte) 0xce, (byte) 0x31, (byte) 0x9d, (byte) 0x9f, (byte) 0x95,
			(byte) 0x80, (byte) 0x9e, (byte) 0x73, (byte) 0x11, (byte) 0x6c,
			(byte) 0x03, (byte) 0x7b, (byte) 0x6e, (byte) 0x62, (byte) 0x9c,
			(byte) 0xd0, (byte) 0xaa, (byte) 0xf6, (byte) 0x5d, (byte) 0xe0,
			(byte) 0xd8, (byte) 0x96, (byte) 0x94, (byte) 0x46, (byte) 0xd1,
			(byte) 0x10, (byte) 0x3c, (byte) 0x1b, (byte) 0x9d, (byte) 0x40,
			(byte) 0xdd, (byte) 0xab, (byte) 0xec, (byte) 0x8a, (byte) 0x5b,
			(byte) 0x1a, (byte) 0xb6, (byte) 0x19, (byte) 0x57, (byte) 0x99,
			(byte) 0x09, (byte) 0xe8, (byte) 0xec, (byte) 0x82, (byte) 0xdc,
			(byte) 0x06, (byte) 0x39, (byte) 0x86, (byte) 0x25, (byte) 0x3b,
			(byte) 0x67, (byte) 0xb5, (byte) 0x17, (byte) 0xc5, (byte) 0x6e,
			(byte) 0x6e, (byte) 0x1c, (byte) 0x6c, (byte) 0xea, (byte) 0xbe,
			(byte) 0xb8, (byte) 0xdd, (byte) 0x68, (byte) 0xf8, (byte) 0xf3,
			(byte) 0x18, (byte) 0xf2, (byte) 0x3c, (byte) 0x99, (byte) 0xdc,
			(byte) 0xa9, (byte) 0xd3, (byte) 0xb2, (byte) 0x7a, (byte) 0x40,
			(byte) 0x70, (byte) 0x4b, (byte) 0xc2, (byte) 0xd2, (byte) 0xa7,
			(byte) 0xb3, (byte) 0x42, (byte) 0x19, (byte) 0xff, (byte) 0x0b,
			(byte) 0xdf, (byte) 0x07, (byte) 0x0e, (byte) 0x6b, (byte) 0x8e,
			(byte) 0xef, (byte) 0x63, (byte) 0x92, (byte) 0xd6, (byte) 0x15,
			(byte) 0x57, (byte) 0x62, (byte) 0x12, (byte) 0x99, (byte) 0x96,
			(byte) 0x96, (byte) 0xa5, (byte) 0x34, (byte) 0x5a, (byte) 0x2c,
			(byte) 0x7c, (byte) 0xf6, (byte) 0xbc, (byte) 0x16, (byte) 0xb2,
			(byte) 0x90, (byte) 0xc3, (byte) 0x11, (byte) 0x5e, (byte) 0xba,
			(byte) 0x0e, (byte) 0xe4, (byte) 0x22, (byte) 0x84, (byte) 0x32,
			(byte) 0x50, (byte) 0xda, (byte) 0x1e, (byte) 0x37, (byte) 0x06,
			(byte) 0x5b, (byte) 0xef, (byte) 0x69, (byte) 0xb7, (byte) 0x6f,
			(byte) 0x10, (byte) 0xcb, (byte) 0xdc, (byte) 0x4d, (byte) 0xfd,
			(byte) 0xdb, (byte) 0xa3, (byte) 0xef, (byte) 0x54, (byte) 0xea,
			(byte) 0xda, (byte) 0x55, (byte) 0xba, (byte) 0x32, (byte) 0xf4,
			(byte) 0x86, (byte) 0x6b, (byte) 0xb1, (byte) 0xc8, (byte) 0xfc,
			(byte) 0x12, (byte) 0x9a, (byte) 0xfc, (byte) 0xda, (byte) 0xfd,
			(byte) 0x2a, (byte) 0xc2, (byte) 0x7f, (byte) 0x70, (byte) 0xce,
			(byte) 0x34, (byte) 0x38, (byte) 0xe6, (byte) 0x6a, (byte) 0x7d,
			(byte) 0x33, (byte) 0xa0, (byte) 0x16, (byte) 0xfb, (byte) 0xfd,
			(byte) 0xa7, (byte) 0xdf, (byte) 0x2e, (byte) 0xe3, (byte) 0x5f,
			(byte) 0x93, (byte) 0x39, (byte) 0xaa, (byte) 0x00, (byte) 0xc7,
			(byte) 0x38, (byte) 0x2e, (byte) 0x9c, (byte) 0xf3, (byte) 0xc4,
			(byte) 0x12, (byte) 0x46, (byte) 0xcf, (byte) 0x06, (byte) 0xfe,
			(byte) 0x0f, (byte) 0x82, (byte) 0x82, (byte) 0x74, (byte) 0x00,
			(byte) 0x71, (byte) 0xf8, (byte) 0x28, (byte) 0x2f, (byte) 0x9b,
			(byte) 0x3f, (byte) 0x9a, (byte) 0x42, (byte) 0x1b, (byte) 0x3e,
			(byte) 0xa6, (byte) 0x0e, (byte) 0x90, (byte) 0xa7, (byte) 0x45,
			(byte) 0xa6, (byte) 0xcd, (byte) 0x6e, (byte) 0x88, (byte) 0x94,
			(byte) 0x08, (byte) 0x3a, (byte) 0xe5, (byte) 0x56, (byte) 0x36,
			(byte) 0x77, (byte) 0x68, (byte) 0x2e, (byte) 0x39, (byte) 0xd3,
			(byte) 0x45, (byte) 0xee, (byte) 0x89, (byte) 0xf0, (byte) 0x71,
			(byte) 0x42, (byte) 0x2d, (byte) 0xe2, (byte) 0x1b, (byte) 0xf5,
			(byte) 0x11, (byte) 0xf0, (byte) 0xff, (byte) 0x05, (byte) 0x0c,
			(byte) 0x78, (byte) 0xa1, (byte) 0x65, (byte) 0xcf, (byte) 0x3c,
			(byte) 0x9e, (byte) 0xe3, (byte) 0x37, (byte) 0x72, (byte) 0x3a,
			(byte) 0x32, (byte) 0xcb, (byte) 0x1f, (byte) 0xfd, (byte) 0x9d,
			(byte) 0x4a, (byte) 0x0e, (byte) 0xf7, (byte) 0x0b, (byte) 0x2b,
			(byte) 0xaa, (byte) 0x57, (byte) 0x2c, (byte) 0x27, (byte) 0xb3,
			(byte) 0xa0, (byte) 0x2a, (byte) 0x0f, (byte) 0x85, (byte) 0x16,
			(byte) 0x6c, (byte) 0xe2, (byte) 0xe0, (byte) 0xa1, (byte) 0x48,
			(byte) 0x8e, (byte) 0x00, (byte) 0x8d, (byte) 0x6d, (byte) 0xc8,
			(byte) 0x10, (byte) 0xfd, (byte) 0x43, (byte) 0x96, (byte) 0x50,
			(byte) 0x07, (byte) 0x07, (byte) 0x9a, (byte) 0xbf, (byte) 0x50,
			(byte) 0x62, (byte) 0x76, (byte) 0x3e, (byte) 0xe1, (byte) 0xf7,
			(byte) 0x70, (byte) 0xc1, (byte) 0xb0, (byte) 0x79, (byte) 0x8e,
			(byte) 0x61, (byte) 0xe3, (byte) 0xfb, (byte) 0x05, (byte) 0x5f,
			(byte) 0xbb, (byte) 0x2d, (byte) 0x76, (byte) 0x69, (byte) 0x89,
			(byte) 0xf3, (byte) 0x1e, (byte) 0x62, (byte) 0xf6, (byte) 0x27,
			(byte) 0x3d, (byte) 0x3e, (byte) 0x41, (byte) 0x0f, (byte) 0xf5,
			(byte) 0x0f, (byte) 0xc7, (byte) 0xf3, (byte) 0x0e, (byte) 0x3b,
			(byte) 0xd5, (byte) 0xed, (byte) 0xcf, (byte) 0xef, (byte) 0x58,
			(byte) 0xfa, (byte) 0x39, (byte) 0xdf, (byte) 0x75, (byte) 0x85,
			(byte) 0x2b, (byte) 0x8b, (byte) 0xaa, (byte) 0x08, (byte) 0x72,
			(byte) 0x52, (byte) 0xa7, (byte) 0x98, (byte) 0x42, (byte) 0x95,
			(byte) 0x7b, (byte) 0xb7, (byte) 0xe7, (byte) 0x10, (byte) 0xfe,
			(byte) 0xdb, (byte) 0x54, (byte) 0x34, (byte) 0xfb, (byte) 0x91,
			(byte) 0x24, (byte) 0x1c, (byte) 0x07, (byte) 0xfb, (byte) 0x9c,
			(byte) 0xce, (byte) 0xd0, (byte) 0x46, (byte) 0xcf, (byte) 0xc4,
			(byte) 0x9d, (byte) 0x09, (byte) 0x49, (byte) 0x24, (byte) 0xec, };

	private static final byte[] SECRET_KEY = { (byte) 0x47, (byte) 0x65,
			(byte) 0x6e, (byte) 0x75, (byte) 0x69, (byte) 0x6e, (byte) 0x65,
			(byte) 0x20, (byte) 0x41, (byte) 0x64, (byte) 0x6f, (byte) 0x62,
			(byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6c, (byte) 0x61,
			(byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x4d, (byte) 0x65,
			(byte) 0x64, (byte) 0x69, (byte) 0x61, (byte) 0x20, (byte) 0x53,
			(byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
			(byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0xf0,
			(byte) 0xee, (byte) 0xc2, (byte) 0x4a, (byte) 0x80, (byte) 0x68,
			(byte) 0xbe, (byte) 0xe8, (byte) 0x2e, (byte) 0x00, (byte) 0xd0,
			(byte) 0xd1, (byte) 0x02, (byte) 0x9e, (byte) 0x7e, (byte) 0x57,
			(byte) 0x6e, (byte) 0xec, (byte) 0x5d, (byte) 0x2d, (byte) 0x29,
			(byte) 0x80, (byte) 0x6f, (byte) 0xab, (byte) 0x93, (byte) 0xb8,
			(byte) 0xe6, (byte) 0x36, (byte) 0xcf, (byte) 0xeb, (byte) 0x31,
			(byte) 0xae };

	//for old style handshake
	public static final byte[] HANDSHAKE_PAD_BYTES = new byte[Constants.HANDSHAKE_SIZE - 4];
	
	static {
		//get security provider
		Security.addProvider(new BouncyCastleProvider());		
		//fill pad bytes
		for (int b = 0; b < HANDSHAKE_PAD_BYTES.length; b++) {
			HANDSHAKE_PAD_BYTES[b] = (byte) 0x00;
		}
	}
	
	private Mac hmacSHA256;

	protected Random random = new Random();

	public RTMPHandshake() {
		try {
			hmacSHA256 = Mac.getInstance("HmacSHA256");
		} catch (SecurityException e) {
			log.error("Security exception when getting HMAC", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("HMAC SHA256 does not exist");
		}
	}

	public IoBuffer generateResponse(IoBuffer input) {
		IoBuffer output = IoBuffer.allocate((Constants.HANDSHAKE_SIZE * 2) + 1);
		input.mark();
		input.position(input.position() + 4);
		byte input4 = input.get();
		input.reset();
		input.mark();
		byte[] newKeyPart = getNewKeyPart(input);
		input.reset();
		byte[] newKey = calculateHMAC_SHA256(newKeyPart, SECRET_KEY);
		byte[] randBytes = new byte[Constants.HANDSHAKE_SIZE - 32];
		random.nextBytes(randBytes);
		byte[] hashedBytes = calculateHMAC_SHA256(randBytes, newKey);
		byte[] byteChunk = new byte[Constants.HANDSHAKE_SIZE];

		if (input4 != 0) {
			System.arraycopy(randBytes, 0, byteChunk, 0, randBytes.length);
			System.arraycopy(hashedBytes, 0, byteChunk, randBytes.length,
				hashedBytes.length);
		} else {
			input.get(byteChunk);
		}
		output.put((byte) 0x03);
		output.put(HANDSHAKE_SERVER_BYTES);
		output.put(byteChunk);
		output.flip();
		return output;
	}

	public byte[] calculateHMAC_SHA256(byte[] input, byte[] key) {
		byte[] output = null;
		try {
			hmacSHA256.init(new SecretKeySpec(key, "HmacSHA256"));
			output = hmacSHA256.doFinal(input);
		} catch (InvalidKeyException e) {
			log.error("Invalid key", e);
		}
		return output;
	}

	public static byte[] getHandshakeBytes() {
		return HANDSHAKE_SERVER_BYTES;
	}

	protected byte[] getNewKeyPart(IoBuffer input) {
		byte[] part = new byte[32];
		byte[] inputArray = new byte[input.remaining()];

		input.get(inputArray, 0, input.remaining());
		int index = ((inputArray[8]&0x0ff) + (inputArray[9]&0x0ff) + (inputArray[10]&0x0ff) + (inputArray[11]&0x0ff)) % 728 + 12;
		System.arraycopy(inputArray, index, part, 0, 32);
		return part;
	}
}
