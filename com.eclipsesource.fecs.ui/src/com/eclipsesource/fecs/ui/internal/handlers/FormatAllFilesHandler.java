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
package com.eclipsesource.fecs.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.eclipsesource.fecs.Fecs;
import com.eclipsesource.fecs.ui.internal.preferences.FecsPreferences;
import com.eclipsesource.fecs.ui.internal.preferences.ResourceSelector;

public class FormatAllFilesHandler extends AbstractHandler {
    // private final formatter;
    private Fecs fecs;

    // 这个留着，可以用
    private ResourceSelector selector;

    /**
     * The constructor.
     */
    public FormatAllFilesHandler() {
    }

    /**
     * the command has been executed, so extract extract the needed information from the application context.
     */
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        // MessageDialog.openInformation(window.getShell(), "JSHint Eclipse
        // Integration", "Hello world");

        // 主要是遍历project，然后边过滤边格式化文件
        // 获取选中的资源
        // 选中的可能是多个资源
        IResource resource = getCurrentProject(window);

        if (resource == null) {
            MessageDialog.openInformation(window.getShell(), "JSHint Eclipse Integration", "请选中需要格式化的内容！");
            return null;
        }

        selector = new ResourceSelector(resource.getProject());

        try {
            fecs = createFecs(resource.getProject());
        } catch (CoreException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            if (formatAllFiles(resource)) {
                MessageDialog.openInformation(window.getShell(), "JSHint Eclipse Integration", "格式化完毕！");
            }
            ;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Fecs createFecs(IProject project) throws CoreException {
        // System.out.println( new ConfigLoader( project ).getConfiguration() );
        FecsPreferences preferences = new FecsPreferences();
        String dir;
        String fecsDir;
        if (preferences.getUseCustomLib()) {
            dir = preferences.getCustomNodeDir();
        } else {
            dir = "";
        }
        fecsDir = preferences.getCustomFecsDir();

        return new Fecs(dir, fecsDir);
    }

    // 获取当前选中的内容
    public static IResource getCurrentProject(IWorkbenchWindow window) {
        ISelectionService selectionService = window.getSelectionService();

        ISelection selection = selectionService.getSelection();

        IResource resource = null;
        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();

            if (element instanceof IResource) {
                resource = ((IResource) element);
            }
        }
        return resource;
    }

    // 递归format文件
    public boolean formatAllFiles(IResource resource) throws InterruptedException {

        if (resource instanceof IFile) {
            if (selector.allowVisitFile(resource)) {

                fecs.format((IFile) resource, null);

            }
        } else if (resource instanceof IFolder) {
            if (selector.allowVisitFolder(resource)) {
                IResource[] members;
                try {
                    members = ((IFolder) resource).members();
                    int len = members.length;
                    for (int i = 0; i < len; i++) {
                        formatAllFiles(members[i]);
                    }
                } catch (CoreException e) {
                    e.printStackTrace();
                    return false;
                }

            }
        } else if (resource instanceof IProject) {
            if (selector.allowVisitProject()) {
                IResource[] members;
                try {
                    members = ((IProject) resource).members();
                    int len = members.length;
                    for (int i = 0; i < len; i++) {
                        formatAllFiles(members[i]);
                    }
                } catch (CoreException e) {
                    e.printStackTrace();
                    return false;
                }

            }
        }
        return true;
    }
}
