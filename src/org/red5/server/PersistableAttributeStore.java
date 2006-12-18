package org.red5.server;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;

/**
 * Persistable attributes store. Server-side SharedObjects feature based on this class.
 */
public class PersistableAttributeStore extends AttributeStore implements
		IPersistable {

    /**
     * Persistence flag
     */
    protected boolean persistent = true;

    /**
     * Attribute store name
     */
    protected String name;

    /**
     * Attribute store type
     */
    protected String type;

    /**
     * Attribute store path (on local hard drive)
     */
    protected String path;

    /**
     * Last modified Timestamp
     */
    protected long lastModified = -1;

    /**
     * Store object that deals with save/load routines
     */
    protected IPersistenceStore store = null;

    /**
     * Creates persistable attribute store
     *
     * @param type             Attribute store type
     * @param name             Attribute store name
     * @param path             Attribute store path
     * @param persistent       Whether store is persistent or not
     */
    public PersistableAttributeStore(String type, String name, String path,
			boolean persistent) {
		this.type = type;
		this.name = name;
		this.path = path;
		this.persistent = persistent;
	}

    /**
     *  Set last modified flag to current system time
     */
    protected void modified() {
		lastModified = System.currentTimeMillis();
		if (store != null) {
			store.save(this);
		}
	}

    /**
     * Check whether object is persistent or not
     *
     * @return   true if object is persistent, false otherwise
     */
    public boolean isPersistent() {
		return persistent;
	}

    /**
     * Set for persistence
     * @param persistent
     */
    public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

    /**
     * Returns last modification time as timestamp
     * @return      Timestamp of last attribute modification
     */
    public long getLastModified() {
		return lastModified;
	}

    /**
     * Return store name
     * @return               Store name
     */
    public String getName() {
		return name;
	}

    /**
     *
     * @param name
     */
    public void setName(String name) {
		this.name = name;
	}

    /**
     *
     * @return
     */
    public String getPath() {
		return path;
	}

    /**
     *
     * @param path
     */
    public void setPath(String path) {
		this.path = path;
	}

    /**
     *
     * @return
     */
    public String getType() {
		return type;
	}

    /**
     * Serializes byte buffer output, storing them to attributes
     *
     * @param output
     * @throws IOException
     */
    public void serialize(Output output) throws IOException {
		Serializer serializer = new Serializer();
		Map<String, Object> persistentAttributes = new HashMap<String, Object>();
		for (String name : attributes.keySet()) {
			if (name.startsWith(IPersistable.TRANSIENT_PREFIX)) {
				continue;
			}

			persistentAttributes.put(name, attributes.get(name));
		}
		serializer.serialize(output, persistentAttributes);
	}

    /**
     * Deserializes data from input to attributes
     *
     * @param input              Input object
     * @throws IOException       I/O exception
     */
    public void deserialize(Input input) throws IOException {
		Deserializer deserializer = new Deserializer();
		Object obj = deserializer.deserialize(input);
		if (!(obj instanceof Map)) {
			throw new IOException("required Map object");
		}

		attributes.putAll((Map<String, Object>) obj);
	}

    /**
     * Load data from another persistent store
     *
     * @param store         Persistent store
     */
    public void setStore(IPersistenceStore store) {
		this.store = store;
		if (store != null) {
			store.load(this);
		}
	}

    /**
     * Return persistent store
     * @return               Persistence store
     */
    public IPersistenceStore getStore() {
		return store;
	}

    /**
     * Set attribute by name and return success as boolean
     *
     * @param name          Attribute name
     * @param value         Attribute value
     * @return              true if attribute was set, false otherwise
     */
    @Override
	synchronized public boolean setAttribute(String name, Object value) {
		boolean result = super.setAttribute(name, value);
		if (result && !name.startsWith(IPersistable.TRANSIENT_PREFIX)) {
			modified();
		}
		return result;
	}

    /**
     * Set attributes from Map
     *
     * @param values          Attributes as Map
     */
    @Override
	synchronized public void setAttributes(Map<String, Object> values) {
		super.setAttributes(values);
		modified();
	}

    /**
     * Bulk set of attributes from another attributes store
     *
     * @param values      Attributes store
     */
    @Override
	synchronized public void setAttributes(IAttributeStore values) {
		super.setAttributes(values);
		modified();
	}

    /**
     * Removes attribute
     * @param name          Attribute name
     * @return              true if attribute was removed, false otherwise
     */
    @Override
	synchronized public boolean removeAttribute(String name) {
		boolean result = super.removeAttribute(name);
		if (result && !name.startsWith(IPersistable.TRANSIENT_PREFIX)) {
			modified();
		}
		return result;
	}

    /**
     * Removes all attributes and sets modified flag
     */
    @Override
	synchronized public void removeAttributes() {
		super.removeAttributes();
		modified();
	}
}
