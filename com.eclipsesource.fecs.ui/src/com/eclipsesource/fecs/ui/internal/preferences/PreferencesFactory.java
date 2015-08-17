/*******************************************************************************
 * Copyright (c) 2012 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package com.eclipsesource.fecs.ui.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

import com.eclipsesource.fecs.ui.internal.Activator;

public class PreferencesFactory {

	// ProjectScope表示项目在eclipse首选项层级，可以用来寻找首选项的值
	// getNode找到关于指定项目指定插件的首选项，这里应该是fecs的首选项
	public static Preferences getProjectPreferences(IProject project) {
		return new ProjectScope(project).getNode(Activator.PLUGIN_ID);
	}

	@SuppressWarnings("deprecation")
	public static Preferences getWorkspacePreferences() {
		// InstanceScope.INSTANCE does not yet exist in Eclipse 3.6
		return new InstanceScope().getNode(Activator.PLUGIN_ID);
	}

}
