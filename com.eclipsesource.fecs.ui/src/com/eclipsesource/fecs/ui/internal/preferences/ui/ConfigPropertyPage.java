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

import static com.eclipsesource.fecs.ui.internal.util.IOUtil.readFileUtf8;
import static com.eclipsesource.fecs.ui.internal.util.IOUtil.writeFileUtf8;
import static com.eclipsesource.fecs.ui.internal.util.LayoutUtil.gridData;
import static com.eclipsesource.fecs.ui.internal.util.LayoutUtil.gridLayout;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

import com.eclipsesource.fecs.ui.internal.Activator;
import com.eclipsesource.fecs.ui.internal.builder.BuilderUtil;
import com.eclipsesource.fecs.ui.internal.builder.FecsBuilder;
import com.eclipsesource.fecs.ui.internal.preferences.OptionsPreferences;
import com.eclipsesource.fecs.ui.internal.util.JsonUtil;

// swt 的一些基本概念
// display shell

// 窗口小部件生命周期

// swt被创建时，对应的底层系统窗口小部件立即被创建。几乎所有窗口小部件状态信息请求都传递至系统窗口小部件。
// 绝大部分系统要求窗口小部件必须在一个特定的父部件中创建。
// 样式块 style bit由swt类中定义的int常量表示。
// 然后对样式进行或操作，作为另一个构造函数参数传递以创建窗口小部件的初始样式

// 抽象窗口小部件类，系统中所有UI对象由抽象类Widget和Control派生而来

// 最高级类，每个swt程序需要一个display和一个或多个shell（代表每一个窗口帧）

// 常用窗口小部件：标签label，
//				按钮button，
//				文本字段text，
//				列表list，
//				组合框combol(selector)
//				表tablecolumn，
//				树tree，左边项目那些就是树
//				容器（复合部件，作为其他窗口小部件的容器。后代是包含在边框内的窗口小部件composite
//					组group），
//				选项卡文件夹（tab folder，左边项目的tab）
//				菜单和其他

// 布局管理：填充布局（FillLayout）
//			行布局（RowLayout）
//			网格布局（GridLayout）
//			表单布局（FormLayout）

//经常要使用两个组合控件以及多种布局。

// 1 【Group 组】，这个组可以为我们生成一个带有线的框，这样可以把杂乱的控件放到一个规整的容器内。

// 2 【Composite 组合控件】，它是为了拼接一些简单的控件，形成具有复杂功能的整合控件。

// 指定项目配置页面类
public class ConfigPropertyPage extends AbstractPropertyPage {

	// 是否使能.fecsrc的checkbox
	private Button projectSpecificCheckbox;
	// 一个json的编辑器
	private ConfigEditor configEditor;
	// 原先的配置，指的是.fecsrc的配置
	private String origConfig;

	// 创建ConfigPropertyPage中的各个部件
	@Override
	protected Control createContents(Composite parent) {
		// 复合部件
		Composite composite = new Composite(parent, SWT.NONE);

		// checkbox，是否使能项目特定的配置项
		Control projectSpecificPart = createProjectSpecificPart(composite);

		// 创建label
		Control labelPart = createLabelPart(composite);

		// jshint插件作者自己弄的editor
		Control configTextPart = createConfigTextPart(composite);

		// import export按钮
		Control buttonsPart = createButtonsPart(composite);

		// 布局
		gridData(composite).fillBoth();
		gridLayout(composite).columns(2).spacing(3);
		gridData(projectSpecificPart);
		gridData(labelPart).span(2, 1).fillHorizontal().widthHint(360);
		gridData(configTextPart).fillBoth().sizeHint(360, 180);
		gridData(buttonsPart).align(SWT.BEGINNING, SWT.BEGINNING);

		loadPreferences();
		return composite;
	}

	// 创建checkbox
	private Control createProjectSpecificPart(Composite parent) {
		projectSpecificCheckbox = new Button(parent, SWT.CHECK);
		projectSpecificCheckbox.setText("Enable project specific configuration");
		projectSpecificCheckbox.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// checkbox改变触发函数，更改editor的状态
				prefsChanged();
			}
		});
		return projectSpecificCheckbox;
	}

	private Control createLabelPart(Composite parent) {
		Link link = new Link(parent, SWT.WRAP);
		link.setText(
				"The project specific configuration will be read from a file named .fecsrc" + " in the project root.");
		BrowserSupport.INSTANCE.enableHyperlinks(link);
		return link;
	}

	// 创建editor
	private Control createConfigTextPart(Composite parent) {
		configEditor = new ConfigEditor(parent) {
			@Override
			public void handleError(String message) {
				if (projectSpecificCheckbox.getSelection()) {
					setErrorMessage(message);
					setValid(message == null);
				}
			}
		};
		return configEditor.getControl();
	}

	// Import & Export
	private Control createButtonsPart(Composite parent) {
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

	// 恢复默认设置
	@Override
	protected void performDefaults() {
		super.performDefaults();
		// 取消项目允许默认配置
		projectSpecificCheckbox.setSelection(OptionsPreferences.DEFAULT_PROJ_SPECIFIC);
		// 将内容设置成{\n \n}，首选项配置项的内容
		configEditor.setText(OptionsPreferences.DEFAULT_CONFIG);
	}

	@Override
	public boolean performOk() {
		try {
			// checkbox是否改变了首选项状态
			boolean prefsChanged = storePrefs();

			// editor中的配置是否更改
			boolean configChanged = projectSpecificCheckbox.getSelection() && storeConfig();
			if (prefsChanged || configChanged) {
				// 两者之一都将触发构建器
				triggerRebuild();
			}
		} catch (CoreException exception) {
			String message = "Failed to store preferences";
			Activator.logError(message, exception);
			return false;
		}
		return true;
	}

	// 勾选checkbox所做的操作，主要是控制editor的状态
	private void prefsChanged() {
		boolean projectSpecific = projectSpecificCheckbox.getSelection();
		System.out.println(projectSpecific);
		configEditor.setEnabled(projectSpecific);
		if (projectSpecific) {
			configEditor.validate();
		} else {
			setErrorMessage(null);
			setValid(true);
		}
	}

	private void loadPreferences() {
		// 获取首选项的配置参数
		OptionsPreferences prefs = new OptionsPreferences(getPreferences());

		// 读取checkbox的状态
		projectSpecificCheckbox.setSelection(prefs.getProjectSpecific());
		// TODO 配置文件中的内容不是最新的，应该从node中获取
		// 读配置文件
		// origConfig = readConfigFile();
		origConfig = prefs.getCurrentConfig();
		
		// configEditor.setText(origConfig != null ? origConfig :
		// getDefaultConfig());
		configEditor.setText(origConfig);

		configEditor.setEnabled(prefs.getProjectSpecific());
	}

	private boolean storePrefs() throws CoreException {
		// 获取当前项目的首选项
		OptionsPreferences prefs = new OptionsPreferences(getPreferences());

		// 设置首选项的项目指定配置的key，默认是不存在的，如果value为true，则改变，false则返回
		prefs.setProjectSpecific(projectSpecificCheckbox.getSelection());

		prefs.setCurrentConfig(configEditor.getText());

		// 标记当前项目的首选项发生改变
		boolean changed = prefs.hasChanged();
		// if (changed) {
		// 保存并强刷
		savePreferences();
		// }
		return changed;
	}

	private boolean storeConfig() throws CoreException {
		// editor中的内容是否与.fecsrc的文件一致
		OptionsPreferences prefs = new OptionsPreferences(getPreferences());
		
		String config = configEditor.getText();
		String fecsrc = readConfigFile();
		boolean jsonChanged;
		if (fecsrc == null) {
			jsonChanged = true;
		} else {
			jsonChanged = !JsonUtil.jsonEquals(config, fecsrc);
		}
		System.out.println("property 重写.fecsrc");
		
		writeConfigFile(config);
		prefs.setFileConfig(config);
		prefs.getFileConfig();
		
//		origConfig = config;
		// 同时将副本
		return jsonChanged;
	}

	// 读配置文件
	private String readConfigFile() {
		IFile configFile = getConfigFile();
		if (checkExists(configFile)) {
			try {
				return readFileUtf8(configFile);
			} catch (Exception exception) {
				String message = "Failed to read config file";
				setErrorMessage(message);
				Activator.logError(message, exception);
			}
		}
		return null;
	}

	private void writeConfigFile(String content) throws CoreException {
		try {
			writeFileUtf8(getConfigFile(), content);
		} catch (CoreException exception) {
			String message = "Could not write to config file";
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message));
		}
	}

	// 获取配置文件
	private IFile getConfigFile() {
		return getResource().getProject().getFile(".fecsrc");
	}

	// 获取首选项的配置内容
//	private String getDefaultConfig() {
//		return new OptionsPreferences(getPreferences()).getConfig();
//	}

	// private String

	private void triggerRebuild() throws CoreException {
		IProject project = getResource().getProject();
		BuilderUtil.triggerClean(project, FecsBuilder.ID);
	}

	private static boolean checkExists(IFile file) {
		if (file.exists()) {
			return true;
		}
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException exception) {
			Activator.logError(exception.getLocalizedMessage(), exception);
		}
		return file.exists();
	}

}
