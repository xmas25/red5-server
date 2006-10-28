package org.red5.server.stream;

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

import org.red5.server.api.IScope;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.IStreamCodecInfo;

public abstract class AbstractStream implements IStream {
	private String name;

	private IStreamCodecInfo codecInfo;

	private IScope scope;

	public String getName() {
		return name;
	}

	public IStreamCodecInfo getCodecInfo() {
		return codecInfo;
	}

	public IScope getScope() {
		return scope;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCodecInfo(IStreamCodecInfo codecInfo) {
		this.codecInfo = codecInfo;
	}

	public void setScope(IScope scope) {
		this.scope = scope;
	}

	protected IStreamAwareScopeHandler getStreamAwareHandler() {
		if (scope != null) {
			IScopeHandler handler = scope.getHandler();
			if (handler instanceof IStreamAwareScopeHandler) {
				return (IStreamAwareScopeHandler) handler;
			}
		}
		return null;
	}
}