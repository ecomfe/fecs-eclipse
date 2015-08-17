/*******************************************************************************
 * Copyright (c) 2012, 2013 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package com.eclipsesource.fecs.ui.internal.preferences;

//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReadWriteLock;
//import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.eclipsesource.fecs.ui.internal.Activator;

/**
 * Instances of this class provide a copy of the global JSHint preferences. The
 * preference values are read from the preference store on creation. Modified
 * values will be written back to the store only when <code>save()</code> is
 * called.
 * <p>
 * Instances of this class can be accessed concurrently from multiple threads.
 * However, multiple instances should not be used to concurrently write values
 * to the backing store, otherwise updates may be lost.
 * </p>
 */

// 和ConfigPreference差不多
public class FecsPreferences {

	private static final String KEY_USE_CUSTOM_LIB = "useCustomLib";
	private static final String KEY_USE_CUSTOM_FECS = "useCustomFecs";

	private static final String KEY_CUSTOM_NODE_DIR = "customNodeDir";
	private static final String KEY_CUSTOM_FECS_DIR = "customFecsDir";

	private static final String KEY_ENABLE_ERROR_MARKERS = "enableErrorMarkers";

	private static final boolean DEF_USE_CUSTOM_LIB = false;
	private static final boolean DEF_USE_CUSTOM_FECS = false;
	private static final String DEF_CUSTOM_LIB_PATH = "";
	private static final boolean DEF_ENABLE_ERROR_MARKERS = false;

	private final Preferences node;

	private boolean useCustomLib;
	// #2
	private boolean useCustomFecs;

	// #1 增加node路径可选
	private String customNodeDir;
	// #2
	private String customFecsDir;

	private boolean enableErrorMarkers;
	private boolean dirty;

	public FecsPreferences() {
		// 获取首选项页
		node = PreferencesFactory.getWorkspacePreferences();

		useCustomLib = node.getBoolean(KEY_USE_CUSTOM_LIB, DEF_USE_CUSTOM_LIB);
		// #2
		useCustomFecs = node.getBoolean(KEY_USE_CUSTOM_FECS, DEF_USE_CUSTOM_FECS);

		// #1 增加路径可选
		customNodeDir = node.get(KEY_CUSTOM_NODE_DIR, DEF_CUSTOM_LIB_PATH);
		// #2
		customFecsDir = node.get(KEY_CUSTOM_FECS_DIR, DEF_CUSTOM_LIB_PATH);

		enableErrorMarkers = node.getBoolean(KEY_ENABLE_ERROR_MARKERS, DEF_ENABLE_ERROR_MARKERS);
		dirty = false;
	}

	// Node
	// UseCustomLib
	public boolean getUseCustomLib() {

		return useCustomLib;
	}

	public void setUseCustomLib(boolean useCustomLib) {
		if (useCustomLib != this.useCustomLib) {
			this.useCustomLib = useCustomLib;
			dirty = true;
		}
	}

	private void putUseCustomLib() {
		if (useCustomLib == DEF_USE_CUSTOM_LIB) {
			node.remove(KEY_USE_CUSTOM_LIB);
		} else {
			node.putBoolean(KEY_USE_CUSTOM_LIB, useCustomLib);
		}
	}

	// CustomNodeDir
	public String getCustomNodeDir() {
		return customNodeDir;
	}

	public void setCustomNodeDir(String customNodeDir) {
		if (!customNodeDir.equals(this.customNodeDir)) {
			this.customNodeDir = customNodeDir;
			dirty = true;
		}
	}

	private void putCustomNodeDir() {
		if (customNodeDir.equals(DEF_CUSTOM_LIB_PATH)) {
			node.remove(KEY_CUSTOM_NODE_DIR);
		} else {
			node.put(KEY_CUSTOM_NODE_DIR, customNodeDir);
		}
	}

	// FECS
	// UseCustomFecs
	public boolean getUseCustomFecs() {

		return useCustomFecs;
	}

	public void setUseCustomFecs(boolean useCustomFecs) {
		if (useCustomFecs != this.useCustomFecs) {
			this.useCustomFecs = useCustomFecs;
			dirty = true;
		}
	}

	private void putUseCustomFecs() {
		if (useCustomFecs == DEF_USE_CUSTOM_FECS) {
			node.remove(KEY_USE_CUSTOM_FECS);
		} else {
			node.putBoolean(KEY_USE_CUSTOM_FECS, useCustomFecs);
		}
	}
	
	// CustomFecsDir
	public String getCustomFecsDir() {
		return customFecsDir;
	}

	public void setCustomFecsDir(String customFecsDir) {
		if (!customFecsDir.equals(this.customFecsDir)) {
			this.customFecsDir = customFecsDir;
			dirty = true;
		}
	}
	
	private void putCustomFecsDir() {
		if (customFecsDir.equals(DEF_CUSTOM_LIB_PATH)) {
			node.remove(KEY_CUSTOM_FECS_DIR);
		} else {
			node.put(KEY_CUSTOM_FECS_DIR, customFecsDir);
		}
	}
	
	public void resetToDefaults() {

		setUseCustomLib(DEF_USE_CUSTOM_LIB);
		setUseCustomFecs(DEF_USE_CUSTOM_FECS);

		setCustomNodeDir(DEF_CUSTOM_LIB_PATH);
		setCustomFecsDir(DEF_CUSTOM_LIB_PATH);

		setEnableErrorMarkers(DEF_ENABLE_ERROR_MARKERS);
	}

	public boolean getEnableErrorMarkers() {

		return enableErrorMarkers;
	}

	public void setEnableErrorMarkers(boolean enableErrorMarkers) {
		if (enableErrorMarkers != this.enableErrorMarkers) {
			this.enableErrorMarkers = enableErrorMarkers;
			dirty = true;
		}
	}

	private void putEnableErrorMarkers() {
		if (enableErrorMarkers == DEF_ENABLE_ERROR_MARKERS) {
			node.remove(KEY_ENABLE_ERROR_MARKERS);
		} else {
			node.putBoolean(KEY_ENABLE_ERROR_MARKERS, enableErrorMarkers);
		}
	}

	public boolean hasChanged() {
		return dirty;
	}

	public void save() throws CoreException {
		putUseCustomLib();
		putUseCustomFecs();
		
		putCustomNodeDir();
		putCustomFecsDir();
		
		putEnableErrorMarkers();
		
		flushNode();

		dirty = false;

	}

	private void flushNode() throws CoreException {
		try {
			node.flush();
		} catch (BackingStoreException exception) {
			String message = "Failed to store preferences";
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, exception);
			throw new CoreException(status);
		}
	}

}
