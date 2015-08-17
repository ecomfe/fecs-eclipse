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

import static com.eclipsesource.fecs.ui.internal.util.JsonUtil.prettyPrint;

import org.osgi.service.prefs.Preferences;

public class OptionsPreferences {
	
	private static final String KEY_PROJ_SPECIFIC = "projectSpecificOptions";
	
	private static final String KEY_GLOBALS = "globals";
	private static final String KEY_OPTIONS = "options";
	
	private static final String KEY_CONFIG = "config";
	
	// 用于存放项目property编辑框里的内容
	private static final String KEY_CURRENT_CONFIG = "currentConfig";
	
	// 用于存放项目.fecsrc文件里的内容
	private static final String KEY_FILE_CONFIG = "fileConfig";
	
	private static final String KEY_IMPORT = "import";

	public static final boolean DEFAULT_PROJ_SPECIFIC = false;
	// TODO set sensible default config
	// 默认配置
	public static final String DEFAULT_CONFIG = "{\n  \n}";

	// fecs首选项节点
	private final Preferences node;
	private boolean changed;

	public OptionsPreferences(Preferences node) {
		this.node = node;
	}

	public Preferences getNode() {
		return node;
	}

	public boolean getProjectSpecific() {
		// key 和 value(布尔)连接，返回value，value不存在或不是boolean则返回null
		// (key, value)
		return node.getBoolean(KEY_PROJ_SPECIFIC, DEFAULT_PROJ_SPECIFIC);
	}

	public void setProjectSpecific(boolean value) {
		if (value != node.getBoolean(KEY_PROJ_SPECIFIC, DEFAULT_PROJ_SPECIFIC)) {
			// false
			if (value == DEFAULT_PROJ_SPECIFIC) {
				// 移除key
				node.remove(KEY_PROJ_SPECIFIC);
			} else {
				// 绑定key value，没有返回值
				node.putBoolean(KEY_PROJ_SPECIFIC, value);
			}
			changed = true;
		}
	}

	public void setImport(boolean value) {
		node.putBoolean(KEY_IMPORT, value);
	}
	
	public boolean getImport() {
		boolean config = node.getBoolean(KEY_IMPORT, false);
		return config;
	}
	
	public String getCurrentConfig() {
		// KEY_CONFIG "config"
		// get(key, def) 返回key的值，若不存在则返回def
		String config = node.get(KEY_CURRENT_CONFIG, DEFAULT_CONFIG);
		return config;
	}
	
	public void setCurrentConfig(String value) {
		// 设置config选项的值
		if (!value.equals(node.get(KEY_CURRENT_CONFIG, DEFAULT_CONFIG))) {
			node.put(KEY_CURRENT_CONFIG, value);
		}
	}
	
	private String getOldConfig() {
		String options = node.get(KEY_OPTIONS, "");
		String globals = node.get(KEY_GLOBALS, "");
		// =.=
		return prettyPrint(OptionParserUtil.createConfiguration(options, globals));
	}

	public String getFileConfig() {
		// KEY_CONFIG "config"
		// get(key, def) 返回key的值，若不存在则返回def
		String config = node.get(KEY_FILE_CONFIG, DEFAULT_CONFIG);
		return config;
	}
	
	public void setFileConfig(String value) {
		// 设置config选项的值
		if (!value.equals(node.get(KEY_FILE_CONFIG, DEFAULT_CONFIG))) {
			node.put(KEY_FILE_CONFIG, value);
		}
	}
	
	public String getConfig() {
		// KEY_CONFIG "config"
		// get(key, def) 返回key的值，若不存在则返回def
		String config = node.get(KEY_CONFIG, DEFAULT_CONFIG);
		return config != null ? config : getOldConfig();
	}
	
	public void setConfig(String value) {
		// 设置config选项的值
		if (!value.equals(node.get(KEY_CONFIG, null))) {
			node.put(KEY_CONFIG, value);
			changed = true;
		}
	}

	public boolean hasChanged() {
		return changed;
	}

	public void clearChanged() {
		changed = false;
	}

}
