package com.eclipsesource.fecs.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.eclipsesource.fecs.Fecs;
import com.eclipsesource.fecs.ui.internal.preferences.FecsPreferences;
import com.eclipsesource.fecs.ui.internal.preferences.ResourceSelector;

import org.eclipse.jface.dialogs.MessageDialog;

// handler主要处理format，所以导入fecs包，

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class FormatFileHandler extends AbstractHandler {
	// private final formatter;
	private Fecs fecs;

	// 这个留着，可以用
	private ResourceSelector selector;

	/**
	 * The constructor.
	 */
	public FormatFileHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IWorkbenchPage page = window.getActivePage();

		// 取得当前处于活动状态的编辑器窗口
		IEditorPart part = page.getActiveEditor();

		if (part == null) {
			MessageDialog.openInformation(window.getShell(), "JSHint Eclipse Integration", "请打开需要格式化的文件！");
			return null;
		}

		IFile file = part.getEditorInput().getAdapter(IFile.class);

		// 获取项目以用来过滤
		IProject project = file.getProject();

		selector = new ResourceSelector(project);

		// 如果文件没有被过滤，则支持格式化
		try {
			fecs = selector.allowVisitProject() && selector.allowVisitFile(file) ? createFecs(project) : null;
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (fecs == null) {
			// 如果fecs为null，则不格式化该文件，并提示
			MessageDialog.openInformation(window.getShell(), "JSHint Eclipse Integration", "该文件不支持格式化！");
		} else {
			// 不为null则进行格式化。
			try {
				fecs.format(file, null);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return null;
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
}


