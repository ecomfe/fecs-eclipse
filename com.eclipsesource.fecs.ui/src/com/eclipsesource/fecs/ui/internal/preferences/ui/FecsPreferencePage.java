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

import static com.eclipsesource.fecs.ui.internal.util.LayoutUtil.gridData;
import static com.eclipsesource.fecs.ui.internal.util.LayoutUtil.gridLayout;

import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
//import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

//import com.eclipsesource.fecs.JSHint;
import com.eclipsesource.fecs.ui.internal.Activator;
import com.eclipsesource.fecs.ui.internal.builder.BuilderUtil;
import com.eclipsesource.fecs.ui.internal.builder.FecsBuilder;
import com.eclipsesource.fecs.ui.internal.preferences.FecsPreferences;

// 首选项页

public class FecsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final FecsPreferences preferences;

	// Node
	private Button defaultLibRadio;
	private Button customLibRadio;
	private Text customLibPathText;
	private Button customLibPathButton;

	// FECS
	private Button defaultFecsRadio;
	private Button customFecsRadio;
	private Text customFecsPathText;
	private Button customFecsPathButton;

	private static final int NODE = 1;
	private static final int FECS = 2;

	private Button enableErrorsCheckbox;

	public FecsPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("General settings for FECS");

		preferences = new FecsPreferences();
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return null;
	}

	// 使用PreferencePage可以使用不同类型的字段编辑器，但必须自己做更多地工作，
	// 载入，验证和保存值
	// 创建Preference Page内容
	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		gridLayout(container).columns(1);

		// 选择node路径的组件
		Composite nodeComposite = new Composite(container, SWT.NONE);
		gridData(nodeComposite).fillHorizontal();
		gridLayout(nodeComposite).columns(3).spacing(3).marginTop(10);
		createCustomNodeArea(nodeComposite);

		// 选择fecs路径的组件
		Composite fecsComposite = new Composite(container, SWT.NONE);
		gridData(fecsComposite).fillHorizontal();
		gridLayout(fecsComposite).columns(3).spacing(3).marginTop(10);
		createCustomFecsArea(fecsComposite);

		// 是否允许在代码提示中显示错误
		createEnableErrorMarkersArea(fecsComposite);
		updateControlsFromPrefs();
		updateControlsEnabled(NODE);
		updateControlsEnabled(FECS);

		return container;
	}

	// 应用按钮
	@Override
	public boolean performOk() {
		try {
			System.out.println("是否改变首选项");
			System.out.println(preferences.hasChanged());

			if (preferences.hasChanged()) {
				preferences.save();
				triggerRebuild();
			}
		} catch (CoreException exception) {
			Activator.logError("Failed to save preferences", exception);
			return false;
		}
		return true;
	}

	// 恢复默认
	@Override
	protected void performDefaults() {
		preferences.resetToDefaults();
		updateControlsFromPrefs();
		updateControlsEnabled(NODE);
		updateControlsEnabled(FECS);
		super.performDefaults();
	}

	private void createCustomNodeArea(Composite parent) {

		Text customLibPathLabelText = new Text(parent, SWT.READ_ONLY | SWT.WRAP);
		customLibPathLabelText.setText("This dir must have node.");
		customLibPathLabelText.setBackground(parent.getBackground());
		gridData(customLibPathLabelText).fillHorizontal().span(3, 1).indent(25, 1);
		
		defaultLibRadio = new Button(parent, SWT.RADIO);
		// #1 使用默认的node bin路径
		defaultLibRadio.setText("Use default directory of node interpreter");

		// 布局
		gridData(defaultLibRadio).fillHorizontal().span(3, 1);

		// 选中即可选择node bin路径
		customLibRadio = new Button(parent, SWT.RADIO);
		customLibRadio.setText("Provide the directory of node interpreter");

		// 布局
		gridData(customLibRadio).fillHorizontal().span(3, 1);

		// 为选择node bin的radio按钮添加监听事件
		customLibRadio.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				preferences.setUseCustomLib(customLibRadio.getSelection());
				validate();
				// 让选择目录的按钮使能
				updateControlsEnabled(NODE);
			}
		});

		// 添加路径
		customLibPathText = new Text(parent, SWT.BORDER);
		gridData(customLibPathText).fillHorizontal().span(2, 1).indent(25, 0);
		customLibPathText.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				preferences.setCustomNodeDir(customLibPathText.getText());
				validate();
			}
		});

		// 选择文件的按钮
		customLibPathButton = new Button(parent, SWT.PUSH);
		customLibPathButton.setText("Select");
		customLibPathButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// selectFile();
				selectDir(customLibPathText);
			}
		});
	}

	private void createCustomFecsArea(Composite parent) {

		Text customFecsPathLabelText = new Text(parent, SWT.READ_ONLY | SWT.WRAP);
		customFecsPathLabelText.setText("This dir must have fecs.");
		customFecsPathLabelText.setBackground(parent.getBackground());
		gridData(customFecsPathLabelText).fillHorizontal().span(3, 1).indent(25, 1);
		
		// FECS
		defaultFecsRadio = new Button(parent, SWT.RADIO);
		// #1 使用默认的node bin路径
		defaultFecsRadio.setText("Use default directory of FECS interpreter.(与node bin相同路径)");

		// 布局
		gridData(defaultFecsRadio).fillHorizontal().span(3, 1);

		// 选中即可选择node bin路径
		customFecsRadio = new Button(parent, SWT.RADIO);
		customFecsRadio.setText("Provide the directory of FECS interpreter");

		// 布局
		gridData(customFecsRadio).fillHorizontal().span(3, 1);

		// 为选择node bin的radio按钮添加监听事件
		customFecsRadio.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				 preferences.setUseCustomFecs(customFecsRadio.getSelection());
				validate();
				// validatePrefs();
				// 让选择目录的按钮使能
				updateControlsEnabled(FECS);
			}
		});

		// 添加路径
		customFecsPathText = new Text(parent, SWT.BORDER);
		gridData(customFecsPathText).fillHorizontal().span(2, 1).indent(25, 0);
		customFecsPathText.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				 preferences.setCustomFecsDir(customFecsPathText.getText());
				validate();
				// validatePrefs();
			}
		});

		// 选择文件的按钮
		customFecsPathButton = new Button(parent, SWT.PUSH);
		customFecsPathButton.setText("Select");
		customFecsPathButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// selectFile();
				selectDir(customFecsPathText);
			}
		});

	}

	// checkbox，勾选后则允许使用error marker
	private void createEnableErrorMarkersArea(Composite parent) {
		enableErrorsCheckbox = new Button(parent, SWT.CHECK);
		enableErrorsCheckbox.setText("Enable FECS errors");
		enableErrorsCheckbox.setToolTipText("If unchecked, errors will be shown as warnings");
		gridData(enableErrorsCheckbox).fillHorizontal().span(3, 1);
		enableErrorsCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				preferences.setEnableErrorMarkers(enableErrorsCheckbox.getSelection());
				validate();
				// validatePrefs();
			}
		});
	}

	private void selectDir(Text text) {
		// 新建文件夹（目录）对话框
		DirectoryDialog folder = new DirectoryDialog(getShell(), SWT.OPEN);
		// 设置文件对话框的标题
		folder.setText("Select FECS bin directory");
		// 设置初始路径
		folder.setFilterPath("SystemDrive");
		// 设置对话框提示文本信息
		// folder.setMessage("请选择相应的文件夹");
		// 打开文件对话框，返回选中文件夹目录
		String selectedDir = folder.open();
		if (selectedDir != null) {
			text.setText(selectedDir);
		}
	}

	private void validate() {
		setErrorMessage(null);
		setValid(true);
		final Display display = getShell().getDisplay();
		Job validator = new Job("FECS preferences validation") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("checking preferences", 1);
					validatePrefs();
					display.asyncExec(new Runnable() {
						public void run() {
							setValid(true);
						}
					});
				} catch (final IllegalArgumentException exception) {
					display.asyncExec(new Runnable() {
						public void run() {
							setErrorMessage(exception.getMessage());
						}
					});
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		validator.schedule();
	}

	// #1
	private void validatePrefs() {
		String dir;
		String fecsDir;
		System.out.println(preferences.getUseCustomLib());
		System.out.println(preferences.getUseCustomFecs());
		if (preferences.getUseCustomLib() && preferences.getUseCustomFecs()) {
			dir = transformDir(preferences.getCustomNodeDir());
			fecsDir = transformDir(preferences.getCustomFecsDir());
			
		} else if (preferences.getUseCustomLib() == false && preferences.getUseCustomFecs() == true) {
			dir = transformDir("");
			fecsDir = transformDir(preferences.getCustomFecsDir());
			
		} else if (preferences.getUseCustomLib() == true && preferences.getUseCustomFecs() == false) {
			dir = transformDir(preferences.getCustomNodeDir());
			fecsDir = dir;
			
		} else {
			dir = transformDir("");
			fecsDir = dir;
			
		}
		validateDir(dir, fecsDir);
	}

	private String transformDir(String dir) {
		// TODO 为路径加/或者\\
		// 将路径中的空格转义
		String result = "";
		if (dir.equalsIgnoreCase("") || dir.equalsIgnoreCase("/")) {
			result = dir;
		}
		// windows的路径好像\\ / 都可以=。=|| 日狗
		else {
			result = dir + "/";
		};
		return result;
	}

	private static void validateDir(String dir, String fecsDir) {
		try {
			
			String command = dir + "node " + fecsDir + "fecs -v";
			
			System.out.println(command);
			Process process = Runtime.getRuntime().exec(command);
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getInputStream(), Charset.forName("utf-8")));
			String result = "";
			String line = null;
			while ((line = br.readLine()) != null) {
				result += line;
				System.out.println(line);
			}
			
			if (result == "") {
				throw new IllegalArgumentException("Please provide the directory of interpreter.");
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Please provide the directory of interpreter.");
		}
	}

	private void updateControlsFromPrefs() {
		customLibRadio.setSelection(preferences.getUseCustomLib());
		defaultLibRadio.setSelection(!customLibRadio.getSelection());
		customLibPathText.setText(preferences.getCustomNodeDir());

		customFecsRadio.setSelection(preferences.getUseCustomFecs());
		defaultFecsRadio.setSelection(!customFecsRadio.getSelection());
		customFecsPathText.setText(preferences.getCustomFecsDir());

		enableErrorsCheckbox.setSelection(preferences.getEnableErrorMarkers());
	}

	private void updateControlsEnabled(int kind) {
		switch (kind) {
		case NODE: {
			boolean enabled = customLibRadio.getSelection();
			customLibPathText.setEnabled(enabled);
			customLibPathButton.setEnabled(enabled);
			break;
		}
		case FECS: {
			boolean enabled = customFecsRadio.getSelection();
			customFecsPathText.setEnabled(enabled);
			customFecsPathButton.setEnabled(enabled);
			break;
		}
		default:
			break;
		}
	}

	private void triggerRebuild() throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (project.isAccessible()) {
				BuilderUtil.triggerClean(project, FecsBuilder.ID);
			}
		}
	}

}
