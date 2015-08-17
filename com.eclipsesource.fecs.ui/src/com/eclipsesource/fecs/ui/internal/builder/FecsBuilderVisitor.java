/*******************************************************************************
 * Copyright (c) 2015 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    huangfengtao - initial implementation and API
 ******************************************************************************/
package com.eclipsesource.fecs.ui.internal.builder;

//
import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;

import com.eclipsesource.fecs.Fecs;
import com.eclipsesource.fecs.ProblemHandler;
import com.eclipsesource.fecs.Text;
import com.eclipsesource.fecs.ui.internal.Activator;
import com.eclipsesource.fecs.ui.internal.builder.FecsBuilder.CoreExceptionWrapper;
import com.eclipsesource.fecs.ui.internal.preferences.EnablementPreferences;
import com.eclipsesource.fecs.ui.internal.preferences.FecsPreferences;
import com.eclipsesource.fecs.ui.internal.preferences.PreferencesFactory;
import com.eclipsesource.fecs.ui.internal.preferences.ResourceSelector;

// 监测的增量需要过滤，借用了原作者的过滤函数

class FecsBuilderVisitor implements IResourceVisitor, IResourceDeltaVisitor {

	// private final JSHint checker;
	private final Fecs fecs;

	// 这个留着，可以用
	private final ResourceSelector selector;
	private final IProgressMonitor monitor;

	// 构造函数
	public FecsBuilderVisitor(IProject project, IProgressMonitor monitor) throws CoreException {
		Preferences node = PreferencesFactory.getProjectPreferences(project);
		new EnablementPreferences(node);
		selector = new ResourceSelector(project);
		fecs = selector.allowVisitProject() ? createFecs(project) : null;
		this.monitor = monitor;
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		return visit(resource);
	}

	public boolean visit(IResource resource) throws CoreException {
		boolean descend = false;
		if (resource.exists() && selector.allowVisitProject() && !monitor.isCanceled()) {
			if (resource.getType() != IResource.FILE) {
				descend = selector.allowVisitFolder(resource);
			} else {
				clean(resource);
				if (selector.allowVisitFile(resource)) {
					check((IFile) resource);
				}
				descend = true;
			}
		}
		return descend;
	}

	private Fecs createFecs(IProject project) throws CoreException {
//		System.out.println( new ConfigLoader( project ).getConfiguration() );

		FecsPreferences preferences = new FecsPreferences();
		
		String dir;
		String fecsDir;
		if (preferences.getUseCustomLib() && preferences.getUseCustomFecs()) {
			dir = preferences.getCustomNodeDir();
			fecsDir = preferences.getCustomFecsDir();
			
		} else if (preferences.getUseCustomLib() == false && preferences.getUseCustomFecs() == true) {
			dir = "";
			fecsDir = preferences.getCustomFecsDir();
			
		} else if (preferences.getUseCustomLib() == true && preferences.getUseCustomFecs() == false) {
			dir = preferences.getCustomNodeDir();
			fecsDir = dir;
			
		} else {
			dir = "";
			fecsDir = dir;
			
		}
		
		return new Fecs(dir, fecsDir);
	}



	private void check(IFile file) throws CoreException {
		Text code = readContent(file);
		ProblemHandler handler = new MarkerHandler(new MarkerAdapter(file), code);
		try {

			try {
//				 fecs.format(file, monitor);
				fecs.check(file, code, handler);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// checker.check( code, handler );

		} catch (CoreExceptionWrapper wrapper) {
			throw (CoreException) wrapper.getCause();
		} catch (RuntimeException exception) {
			String message = "Failed checking file " + file.getFullPath().toPortableString();
			throw new RuntimeException(message, exception);
		}
	}

	private static void clean(IResource resource) throws CoreException {
		new MarkerAdapter(resource).removeMarkers();
	}

	// private static InputStream getCustomLib() throws FileNotFoundException {
	// JSHintPreferences globalPrefs = new JSHintPreferences();
	// if( globalPrefs.getUseCustomLib() ) {
	// File file = new File( globalPrefs.getCustomLibPath() );
	// return new FileInputStream( file );
	// }
	// return null;
	// }
	//

	private static Text readContent(IFile file) throws CoreException {
		try {
			InputStream inputStream = file.getContents();
			String charset = file.getCharset();
			return readContent(inputStream, charset);
		} catch (IOException exception) {
			String message = "Failed to read resource";
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, exception));
		}
	}

	private static Text readContent(InputStream inputStream, String charset)
			throws UnsupportedEncodingException, IOException {
		Text result;
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
		try {
			result = new Text(reader);
		} finally {
			reader.close();
		}
		return result;
	}

}