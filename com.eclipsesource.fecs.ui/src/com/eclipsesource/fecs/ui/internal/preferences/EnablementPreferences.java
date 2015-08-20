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

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.osgi.service.prefs.Preferences;

public class EnablementPreferences {

    private static final String KEY_USE_FECS = "useFecs";

    private static final String KEY_EXCLUDED = "excluded";
    private static final String KEY_INCLUDED = "included";
    private static final String DEF_EXCLUDED =
            "//*.m.css://*.m.htm://*.m.html://*.min.css://*.min.htm://*.min.html://*.min.js://*.tpl.htm://*.tpl.html";
    private static final String DEF_INCLUDED = "//*.css://*.html://*.js://*.less";

    private static final Boolean DEF_USE_FECS = false;

    private final Preferences node;
    private boolean changed;

    public EnablementPreferences(Preferences node) {
        this.node = node;
        changed = false;
    }

    public boolean getUseFecs() {

        return node.getBoolean(KEY_USE_FECS, DEF_USE_FECS);
    }

    public void setUseFecs(boolean useFecs) {
        if (useFecs != node.getBoolean(KEY_USE_FECS, DEF_USE_FECS)) {
            if (useFecs == DEF_USE_FECS) {
                node.remove(KEY_USE_FECS);
            }
            else {
                node.putBoolean(KEY_USE_FECS, useFecs);    
            }
            changed = true;
        }
    }

    public void setIncludePatterns(List<String> patterns) {
        String value = PathEncoder.encodePaths(patterns);
        if (!value.equals(node.get(KEY_INCLUDED, DEF_INCLUDED))) {
            if (DEF_INCLUDED.equals(value)) {
                node.remove(KEY_INCLUDED);
            } else {
                node.put(KEY_INCLUDED, value);
            }
            changed = true;
        }
    }

    public List<String> getIncludePatterns() {
        String value = node.get(KEY_INCLUDED, DEF_INCLUDED);
        return PathEncoder.decodePaths(value);
    }

    public void setExcludePatterns(List<String> patterns) {
        String value = PathEncoder.encodePaths(patterns);
        if (!value.equals(node.get(KEY_EXCLUDED, DEF_EXCLUDED))) {
            if (DEF_EXCLUDED.equals(value)) {
                node.remove(KEY_EXCLUDED);
            } else {
                node.put(KEY_EXCLUDED, value);
            }
            changed = true;
        }
    }

    public List<String> getExcludePatterns() {
        String value = node.get(KEY_EXCLUDED, DEF_EXCLUDED);
        return PathEncoder.decodePaths(value);
    }

    public boolean hasChanged() {
        return changed;
    }

    public void clearChanged() {
        changed = false;
    }

    public static String getResourcePath(IResource resource) {
        return resource.getProjectRelativePath().toPortableString();
    }

}
