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
package com.eclipsesource.fecs.ui.internal.preferences.ui;

import static com.eclipsesource.fecs.ui.internal.util.JsonUtil.jsonEquals;
import static com.eclipsesource.fecs.ui.internal.util.LayoutUtil.gridData;
import static com.eclipsesource.fecs.ui.internal.util.LayoutUtil.gridLayout;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.eclipsesource.fecs.ui.internal.Activator;
import com.eclipsesource.fecs.ui.internal.builder.BuilderUtil;
import com.eclipsesource.fecs.ui.internal.builder.FecsBuilder;
import com.eclipsesource.fecs.ui.internal.preferences.OptionsPreferences;
import com.eclipsesource.fecs.ui.internal.preferences.PreferencesFactory;

// 首选项应用在各个项目中，若每个项目有各自的property配置，则如果使能项目指定配置，则覆盖首选项
public class ConfigPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private ConfigEditor configEditor;
	private String origConfig;

	public ConfigPreferencePage() {
		setDescription("General configuration for FECS");
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		// 创建ConfigPreferencePage中的部件
		// 组合空间
		Composite composite = new Composite(parent, SWT.NONE);

		// 创建label部分
		Control labelPart = createLabelPart(composite);

		// 创建配置文本内容
		Control configTextPart = createConfigTextPart(composite);

		// 创建import export按钮
		Control buttonsPart = createButtonsPart(composite);

		// 布局
		gridData(composite).fillBoth();
		gridLayout(composite).columns(2).spacing(3);
		gridData(labelPart).span(2, 1).fillHorizontal().widthHint(360);
		gridData(configTextPart).fillBoth().sizeHint(360, 180);
		gridData(buttonsPart).align(SWT.BEGINNING, SWT.BEGINNING);

		// 读取首选项
		loadPreferences();
		return composite;
	}

	private Control createLabelPart(Composite parent) {
		Link link = new Link(parent, SWT.WRAP);
		link.setText("General .fecsrc file for FECS");

		// 允许打开eclipse的内置浏览器
		BrowserSupport.INSTANCE.enableHyperlinks(link);
		return link;
	}

	private Control createConfigTextPart(Composite parent) {
		// jshint插件的作者自己扩展了editor
		configEditor = new ConfigEditor(parent) {
			@Override
			public void handleError(String message) {
				setErrorMessage(message);
				setValid(message == null);
			}
		};
		return configEditor.getControl();
	}

	private Control createButtonsPart(Composite parent) {
		// 包装组合部件类，composite将按钮包装
		ButtonBar buttonBar = new ButtonBar(parent, SWT.NONE);
		buttonBar.addButton("I&mport", new Listener() {
			public void handleEvent(Event event) {
				configEditor.importConfig();
			}
		});
		buttonBar.addButton("E&xport", new Listener() {
			public void handleEvent(Event event) {
				configEditor.exportConfig();
			}
		});
		return buttonBar;
	}

	// 加载首选项
	private void loadPreferences() {
		// 操作首选项参数的类
		OptionsPreferences optionsPreferences = new OptionsPreferences(getPreferences());
		// 获得原来的首选项配置参数
		origConfig = optionsPreferences.getConfig();
		// 显示原来的配置参数
		configEditor.setText(origConfig);
	}

	// 恢复默认参数
	@Override
	protected void performDefaults() {
		super.performDefaults();
		configEditor.setText(OptionsPreferences.DEFAULT_CONFIG);
	}

	// 应用首选项并激活各个项目的构造器
	@Override
	public boolean performOk() {
		try {
			storePreferences();

			if (!jsonEquals(configEditor.getText(), origConfig)) {
				triggerRebuild();
			}
		} catch (CoreException exception) {
			// TODO revise error handling
			String message = "Failed to store settings";
			Activator.logError(message, exception);
			return false;
		}
		return true;
	}

	// 保存首选项的值
	private void storePreferences() throws CoreException {
		// 首选项参数的自定义类
		OptionsPreferences optionsPreferences = new OptionsPreferences(getPreferences());

		// 保存首选项的配置，根据内容是否变化更改内部flag
		optionsPreferences.setConfig(configEditor.getText());

		// 内容改变了则保存首选项
		if (optionsPreferences.hasChanged()) {
			savePreferences();
		}
	}

	// 保存首选项
	private void savePreferences() throws CoreException {
		// 获取当前页面的偏好设置节点
		Preferences node = getPreferences();
		try {
			// 强制刷新
			// Forces any changes in the contents of this node and its
			// descendants to the persistent store
			node.flush();
		} catch (BackingStoreException exception) {
			String message = "Failed to store preferences";
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, exception);
			throw new CoreException(status);
		}
	}

	// 激活每个项目的构建器 rebuild其实就是CLEAN_BUILD：clean任务 + FULL_BUILD
	private void triggerRebuild() throws CoreException {
		for (IProject project : getProjects()) {
			if (project.isAccessible()) {
				BuilderUtil.triggerClean(project, FecsBuilder.ID);
			}
		}
	}

	// 获取当前工作区全部项目
	IProject[] getProjects() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

	// 获得指定项目指定插件的首选项
	Preferences getPreferences() {
		return PreferencesFactory.getWorkspacePreferences();
	}

}
