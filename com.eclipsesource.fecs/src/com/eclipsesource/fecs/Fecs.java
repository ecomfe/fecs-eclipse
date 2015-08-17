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
package com.eclipsesource.fecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.fecs.internal.ProblemImpl;

// 这个类应该叫做Fecs比较合适一些
// 包含的函数：check，format，parseConfig

// 现在实现了check，format

// 继续改进的地方：
// TODO
// 1 看看ignore选项能不能用

// => check和format的ignore可以通过property页面来更改

// 2 增加全局format功能，怎么增加全局format功能，在菜单栏和工具栏上面多增加一个command，然后bind一个快捷键，写一个handle

// => format可格式化选中的内容，只可选中一项（多选会默认取选中的第一项），如果选中project和folder，其内部文件（被过滤的除外），均会被format

// 3 工具栏的图标=。=

// => 暂时无所谓吧

// 4 parseConfig 还需要考虑一下，得有个地方来选择node路径！！！
//   check的cli options 有color——no
//						 debug——no
//						 format--no		内置--format json
//						 ignore——no		通过property page设置过滤
//						 lookup——no		用来决定是否允许读取.fecsrc文件，合并默认配置
//						 maxerr——yes
//						 maxsize——yes
//						 reporter——no	内置——baidu
//						 rule——no		内置——rule true
//						 silent——no		内置——silent false
//						 stream——no
//						 type——no		

//   format的cli options 有debug——no
//						  format——no
//						  ignore——no	通过property page设置过滤
//						  lookup——no	用来决定是否允许读取.fecsrc文件，合并默认配置
//						  output——no	不能选中output路径
//   					  replace——no	内置
//						  safe——yes
//						  silent——no	
//						  stream——no
//						  type——no

// 综上，需要修改的options只有三个。。。。

// =>

// 想到一个很刺激的方法，就是通过properties的配置创建.fecsrc文件...结果这个插件居然就是这么干的。。。
// preference 的话，配置则为每个项目创建.fecsrc，内容默认为.fecsrc的内容

// eclipse获得的PATH与shell的PATH不一样
// 通过命令行打开。。。我也是醉

// 怎么导出插件包
// => =.=

// 改进，FECS全部大写
// 将上面的options绝大部分弄成可配置 => 修改preference页
// 

public class Fecs {
	private final String dir;
	private final String fecsDir;

	public Fecs(String dir, String fecsDir) {
		this.dir = transformDir(dir);
		this.fecsDir = transformDir(fecsDir);
	}

	public String check(IFile resource, Text code, ProblemHandler handler) throws InterruptedException {
		try {
			// 获取文件的路径
			IPath path = resource.getRawLocation();
			String text = "";
			text += path;
			// TODO
			String command;
			// if (System.getProperty("os.name").contains("Window")) {
			// command = "fecs.cmd " + text + " --reporter baidu --rule true
			// --sort true --silent true --format json";
			// } else if (System.getProperty("os.name").contains("Mac")) {
			command = dir + "node " + fecsDir + "fecs " + text
					+ " --reporter baidu --rule true --sort true --silent true --format json";
			// } else {
			// command = "fecs " + text + " --reporter baidu --rule true --sort
			// true --silent true --format json";
			// }
			System.out.println(command);
			// String command = bin + "fecs " + text + " --reporter baidu --rule
			// true --sort true --silent true --format json";
			// 执行命令行
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();

			// 获取控制台输出
			BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getInputStream(), Charset.forName("utf-8")));

			// 将输出存入result中
			String result = "";
			String line = null;
			while ((line = br.readLine()) != null) {
				result += line;
			}

			// 检查失败则result没有变化，仍是""
			if (result != "") {
				handleProblems(handler, code, result);
			}

			// 如果没有错误，result是[]
			System.out.println(result);
			return result;

		} catch (IOException e) {
			return null;
		}
	}

	private void handleProblems(ProblemHandler handler, Text text, String result) {
		// 提取fecs返回的JSON
		JsonArray json = JsonArray.readFrom(result);
		JsonArray errors = null;
		if (!json.isEmpty()) {
			// 获取当前文件的全部错误信息
			errors = (JsonArray) (((JsonObject) json.get(0)).get("errors"));
			int length = errors.size();
			for (int i = 0; i < length; i++) {
				// 获取单条错误信息
				JsonObject error = (JsonObject) errors.get(i);
				if (error != null) {
					// 将单条错误信息转化为错误标记模板
					Problem problem = createProblem(error, text);
					// 标记错误信息
					handler.handleProblem(problem);
				}
			}
		}
	}

	ProblemImpl createProblem(JsonObject error, Text text) {
		String reason = error.get("message").asString();
		int line = error.get("line").asInt();
		int severity = error.get("severity").asInt();
		int character = error.get("column").asInt();
		String code = error.get("rule").asString();

		if (line <= 0 || line > text.getLineCount()) {
			line = -1;
			character = -1;
		} else if (character > 0) {
			character = visualToCharIndex(text, line, character);
		}

		String message = reason;
		return new ProblemImpl(line, character, message, code, severity);
	}

	/*
	 * JSHint reports "visual" character positions instead of a character index,
	 * i.e. the first character is 1 and every tab character is multiplied by
	 * the indent with.
	 *
	 * Example: "a\tb\tc"
	 *
	 * index: | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10| char: | a | » | b |
	 * » | c | visual: | a | » | b | » | c |
	 */
	int visualToCharIndex(Text text, int line, int character) {
		String string = text.getContent();
		int offset = text.getLineOffset(line - 1);
		int charIndex = 0;
		int visualIndex = 1;
		int maxCharIndex = string.length() - offset - 1;
		while (visualIndex != character && charIndex < maxCharIndex) {
			boolean isTab = string.charAt(offset + charIndex) == '\t';
			visualIndex += isTab ? 4 : 1;
			charIndex++;
		}
		return charIndex;
	}

	public String format(IFile resource, IProgressMonitor monitor) throws InterruptedException {
		try {
			IPath path = resource.getRawLocation();
			String text = "";
			text += path;

			String command;
			// if (System.getProperty("os.name").contains("Window")) {
			// command = "fecs.cmd format " + text + " --replace true";
			// } else if (System.getProperty("os.name").contains("Mac")) {
			command = dir + "node " + fecsDir + "fecs format " + text + " --replace true";
			// } else {
			// command = "fecs.cmd format " + text + " --replace true";
			// }

			// String[] command = new String[]{"/bin/zsh", "-c", "which npm"};
			System.out.println("在Fecs中dir:" + dir);
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();

			// 强制editor刷新文件
			try {
				// resource.DEPTH_ONE == 1
				resource.refreshLocal(1, monitor);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			return null;
		}
		return null;
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
		}
		;
		return result;
	}
}
