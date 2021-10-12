// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.gogoclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;

public class GogoClient implements Runnable {
	private final static String GOGO_HISTORY = ".casr.gogo";

	public static void main(String[] args) throws Exception {
		GogoClient client = new GogoClient(args[0], args[1], args[2]);
		if (args.length == 4 && args[3].equals("noCompletion"))
			client._loaded = true;
	}

	private Socket socket;
	private ConsoleReader _console;
	private boolean _loaded;
	private List<String> _completions = new ArrayList<>();

	public GogoClient(String user, String server, String port) throws Exception {
		socket = new Socket(server, Integer.parseInt(port));
		new Thread(this).start();

		_console = new ConsoleReader();

		// console.setPrompt ("> ");
		FileHistory history;
		
		File historyPath = new File(System.getProperty("user.home") + File.separator + GOGO_HISTORY);
		if (! historyPath.getParentFile().exists()) {
			historyPath = new File(System.getProperty("java.io.tmpdir") + File.separator + GOGO_HISTORY);
		}
		_console.setHistory(history = new FileHistory(historyPath));

		String line = "help";

		while (line != null) {
			if (line.length() > 0) {
				// System.out.println ("----> "+line);
				socket.getOutputStream().write((line + "\n").getBytes("ascii"));
				socket.getOutputStream().flush();
				if (history.size() == 100) {
					history.removeFirst();
				}
				history.flush();
			}
			line = _console.readLine();
		}

	}

	public void run() {
		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ascii"));
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					// socket closed
					System.out.println("Closed");
					System.exit(0);
					return;
				}
				if (line.equals(".done")) {
					if (!_loaded) {
						_loaded = true;
						Collections.sort(_completions);
						_console.addCompleter(new StringsCompleter(_completions));
						System.out.println("Connected");
						continue;
					} else
						line = "***************";
				}
				if (!_loaded) {
					_completions.add(line);
					int sep = line.indexOf(':');
					if (sep != -1)
						_completions.add(line.substring(sep + 1));
					continue;
				}
				System.out.println(line);
				if (line.equals("CommandNotFoundException")) {
					_console.getHistory().removeLast();
					((FileHistory) _console.getHistory()).flush();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
