package com.nokia.as.util.test.player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

public class TestPlayer {

	protected static Map<String, TestPlayer> _extensions = new HashMap<String, TestPlayer>();
	protected static TestPlayer instance;
	protected static boolean _commented;
	protected static int _fileDepth = 0;
	protected static long _timeStarted = System.currentTimeMillis();
	protected static Hashtable<String, Integer> threads = new Hashtable<String, Integer>();

	protected static final ThreadLocal<String> prefix = new ThreadLocal<String>() {
		protected String initialValue() {
			return "";
		}
	};
	protected static final ThreadLocal<String> tab = new ThreadLocal<String>() {
		protected String initialValue() {
			return "";
		}
	};

	protected String getPrefix() {
		String p = prefix.get();
		return p.length() == 0 ? "" : p + ".";
	}

	public Boolean forked(String value) throws Exception {
		prefix.set(get(value));
		synchronized (threads) {
			threads.put(get(value), 0);
		}
		return true;
	}

	/***************** launching *************/

	public static void main(String[] args) throws Exception {
		instance = new TestPlayer();
		String directory = instance.get("$directory");
		if (directory.length() == 0)
			directory = "./";
		else if (!directory.endsWith("/"))
			instance.set("directory", directory + "/");
		instance.set("pause", "0");
		instance.set("log", "1");
		instance.set("on-ok", "");
		instance.set("on-ko", "");
		instance.ext("hate com.nokia.as.util.test.player.HatePlayer");
		instance.ext("http com.nokia.as.util.test.player.HttpPlayer");
		instance.ext("tcp com.nokia.as.util.test.player.TcpPlayer");
		instance.ext("enab com.nokia.as.util.test.player.SessionEnabPlayer");
		instance.ext("diameter com.nokia.as.util.test.player.DiameterPlayer");
		instance.ext("test com.nokia.as.util.test.player.TestPlayerTests");
		boolean success = false;
		try {
			for (String arg : args) {
				success |= instance.play("play: @" + arg);
			}
		} 
		
		catch (Exception e) {
			e.printStackTrace();
		}
		
		finally {
			instance.close();
			for (TestPlayer tp : _extensions.values())
				tp.close();
			if (! success) {
				System.err.println("Some test failed");
			}
			System.exit(success == true ? 0 : 1);
		}
	}

	public Boolean waitfor(String value) throws Exception {
		int i = 300; // 5mins
		synchronized (threads) {
			while (--i > 0) { // dont make it infinite
				if (value == null) {
					int running = 0;
					int failed = 0;
					for (Integer s : threads.values()) {
						if (s.equals(0)) {
							running++;
						} else if (s.equals(-1))
							failed++;
					}
					if (running == 0) {
						return failed == 0;
					}
				} else {
					if (threads.get(value).equals(1))
						return true;
				}
				threads.wait(1000);
			}
		}
		return false;
	}

	protected boolean playFile(File file) throws Exception {
		String initialTab = tab.get();
		boolean res = false;
		_fileDepth++;
		try {
			String scenario = getFile(file, false);
			return res = playScenario(scenario);
		} catch (Throwable t) {
			t.printStackTrace();
			return res = false;
		} finally {
			tab.set(initialTab);
			if (--_fileDepth == 0) {
				_commented = false;
				set("log", "1");
				res = waitfor(null) && res;
				if (res)
					play(get("$on-ok"));
				else
					play(get("$on-ko"));
			}
		}
	}

	protected boolean playScenario(String scenario) throws Exception {
		boolean res = true;
		BufferedReader reader = new BufferedReader(new StringReader(scenario));
		try {
			while (res) {
				String line = reader.readLine();
				if (line == null)
					break;
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				while (line.endsWith(">>")) {
					String next = null;
					while (true) {
						next = reader.readLine();
						if (next == null)
							break;
						next = next.trim();
						if (next.startsWith("#"))
							continue;
						break;
					}
					line = line + next;
				}
				if (line.endsWith(">>"))
					line = line.substring(0, line.length() - 2);
				res = playLine(line);
				if (getInt("$pause") > 0)
					Thread.sleep(getInt("$pause"));
			}
		} finally {
			reader.close();
		}
		return res;
	}

	protected boolean playLine(final String line) throws Exception {
		String[] toks = parseLine(line);
		String cmd = toks[0];
		final String value = toks[1];
		if (cmd.equals("uncomment")) {
			_commented = false;
			log(cmd, true);
			return true;
		}
		if (_commented)
			return true;
		if (cmd.equals("log")) {
			log(cmd + " : " + get(value), true);
			return true;
		}
		log(getPrefix() + "RUN\t" + line);
		if (cmd.equals("fork")) {
			final String tmp = tab.get();
			Runnable r = new Runnable() {
				public void run() {
					boolean res = false;
					try {
						tab.set(tmp);
						res = play("forked:" + value);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						synchronized (threads) {
							threads.put(prefix.get(), res ? 1 : -1);
							threads.notifyAll();
						}
					}
				}
			};
			new Thread(r).start();
			return true;
		}
		tab.set(tab.get() + "\t");
		int index = cmd.indexOf('.');
		final TestPlayer target;
		if (index != -1) {
			target = _extensions.get(cmd.substring(0, index));
			cmd = cmd.substring(index + 1);
		} else
			target = instance;
		Method method = target.getClass().getMethod(target.getMethod(cmd), String.class);
		boolean res = (Boolean) method.invoke(target, value);
		if (res) {
			logRes(res, log());
		} else {
			if (!log())
				log(getPrefix() + "RUN\t" + line, true);
			logRes(res, true);
		}
		String tmp = tab.get();
		tab.set(tmp.substring(0, tmp.length() - 1));
		return res;
	}

	protected String[] parseLine(String line) throws Exception {
		int i = line.indexOf(':');
		String cmd = i != -1 ? line.substring(0, i).trim() : line.trim();
		String value = i != -1 ? line.substring(i + 1).trim() : null;
		cmd = cmd.toLowerCase().replace("-", "");
		return new String[] { cmd, value };
	}

	public String getMethod(String cmd) {
		if (cmd.equals("if"))
			return "_if";
		if (cmd.equals("else"))
			return "_else";
		if (cmd.equals("for"))
			return "_for";
		return cmd;
	}

	/***************** common methods for superclasses *************/

	public TestPlayer() {
	}

	public void close() throws Exception {
	}

	public Boolean set(String name, Object v) throws Exception {
		String value = v != null ? v.toString() : null;
		if (name.startsWith("@")) {
			if (value != null)
				writeFile(name.substring(1), value);
			else
				newFile(name.substring(1)).delete();
		} else {
			if (value != null)
				System.setProperty(name, value);
			else
				System.getProperties().remove(name);
		}
		return true;
	}

	public String get(String name) throws Exception {
		return get(get(name, '$'), '@');
	}

	public String get(String name, char delim) throws Exception {
		int i = name.indexOf(delim);
		if (i > 0 && name.charAt(i - 1) == '\\') {
			String pre = i == 1 ? "" : name.substring(0, i - 1);
			String post = i == name.length() - 1 ? "" : name.substring(i + 1);
			return pre + delim + get(post, delim);
		}
		if (i == -1)
			return name;
		String pre = i == 0 ? "" : name.substring(0, i);
		String post = "";
		int j = name.length();
		if (name.charAt(i + 1) == '(') {
			i += 1;
			j = name.indexOf(')', i);
			if (j != name.length() - 1)
				post = name.substring(j + 1);
		} else {
			j = name.indexOf(' ', i);
			if (j == -1)
				j = name.length();
			else
				post = name.substring(j);
		}
		name = name.substring(i + 1, j);
		String resolved = delim == '$' ? System.getProperty(name) : getFile(name, name.endsWith(".tpl")); // tpl for
																											// template
		if (resolved == null)
			resolved = "";
		return pre + resolved + get(post, delim);
	}

	public int getInt(String name) throws Exception {
		name = get(name);
		if (name.length() == 0)
			return 0;
		return asInt(name);
	}

	public static int asInt(String s) {
		return Integer.parseInt(s);
	}

	public static String escape(String s) {
		return escape(escape(s, '$'), '@');
	}

	private static String escape(String s, char esc) {
		int i = s.indexOf(esc);
		if (i > -1) {
			String pre = (i > 0 ? s.substring(0, i) : "") + '\\' + esc;
			String post = i == s.length() - 1 ? "" : escape(s.substring(i + 1), esc);
			return pre + post;
		}
		return s;
	}

	public String[] split(String value) throws Exception {
		return split(value, -1);
	}

	public String[] split(String value, boolean resolve) throws Exception {
		return split(value, -1, resolve);
	}

	public String[] split(String value, int n) throws Exception {
		return split(value, n, true);
	}

	public String[] split(String value, int n, boolean resolve) throws Exception {
		if (value == null || value.length() == 0)
			return new String[0];
		List<String> toks = new ArrayList<String>();
		int count = 0;
		while (true) {
			int isp = value.indexOf(' ');
			int itab = value.indexOf('\t');
			if (isp == -1)
				isp = itab;
			if (itab == -1)
				itab = isp;
			int i = Math.min(isp, itab);
			if (i == -1) {
				toks.add(value);
				break;
			}
			toks.add(value.substring(0, i));
			value = value.substring(i + 1).trim();
			count++;
			if (count == n - 1) {
				toks.add(value);
				break;
			}
		}
		String[] res = new String[toks.size()];
		for (int i = 0; i < res.length; i++)
			res[i] = resolve ? get(toks.get(i)) : toks.get(i);
		return res;
	}

	public static String concatenate(String[] s, int from) {
		StringBuilder sb = new StringBuilder();
		for (int i = from; i < s.length; i++)
			sb.append(" " + s[i]);
		return sb.toString().trim();
	}

	public static String unquote(String s) {
		return quoted(s) ? s.substring(1, s.length() - 1) : s;
	}

	public static String quote(String s) {
		return "\"" + s + "\"";
	}

	public static boolean quoted(String s) {
		return s.startsWith("\"") && s.endsWith("\"") && s.length() > 1;
	}

	public Boolean play(String cmd, String value) throws Exception {
		return play(cmd + ":" + value);
	}

	protected boolean isOn(String name) throws Exception {
		return "1".equals(get(name));
	}

	protected boolean isOff(String name) throws Exception {
		return !isOn(name);
	}

	protected boolean log() throws Exception {
		return isOn("$log");
	}

	protected void logRes(boolean res, boolean doLog) throws Exception {
		if (isOn("$chrono"))
			log(res ? getPrefix() + "OK [" + getTime() + "]" : getPrefix() + "KO <-------------- [" + getTime() + "]",
					doLog);
		else
			log(res ? getPrefix() + "OK" : getPrefix() + "KO <--------------", doLog);
	}

	protected void log(Object s) throws Exception {
		log(s, log());
	}

	protected void log(Object s, boolean doLog) throws Exception {
		if (doLog)
			System.out.println(tab.get() + s.toString());
	}

	private double getTime() {
		return (System.currentTimeMillis() - _timeStarted) / 1000D;
	}

	/***************** core commands *************/

	public boolean play(String value) throws Exception {
		if (value.length() == 0)
			return true;
		String[] toks = split(value, false);
		if (toks[0].startsWith("@")) {
			toks[0] = toks[0].substring(1);
			File file = newFile(toks[0]);
			String regex = toks.length > 1 ? get(toks[1]) : ".*\\.txt";
			final Pattern pattern = Pattern.compile(regex);
			if (file.isDirectory()) {
				String[] files = file.list(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return pattern.matcher(name).matches();
					}
				});
				boolean res = true;
				Arrays.sort(files);
				for (String scenario : files)
					if (play("play: " + value + File.separator + scenario) == false)
						res = false;
				return res;
			} else {
				return playFile(file);
			}
		}
		return playScenario(value.replace(">>", "\n"));
	}

	public Boolean set(String value) throws Exception {
		String[] toks = split(value, 2, false);
		if (toks.length > 1)
			toks[1] = unquote(get(toks[1]));
		String name = toks[0];
		if (!name.startsWith("@"))
			toks[0] = get(toks[0]);
		if (toks.length == 1)
			set(toks[0], null);
		else
			set(toks[0], toks[1]);
		return true;
	}

	public Boolean equal(String value) throws Exception {
		String[] toks = split(value, 2, false);
		if (toks[0].indexOf('$') == -1 && toks[0].indexOf('@') == -1)
			toks[0] = "$(" + toks[0] + ")";
		String val = get(toks[0]);
		if (toks.length == 2)
			return val != null && val.equals(unquote(get(toks[1])));
		else
			return val.length() == 0;
	}

	public Boolean like(String value) throws Exception {
		String[] toks = split(value, 2, false);
		if (toks[0].indexOf('$') == -1 && toks[0].indexOf('@') == -1)
			toks[0] = "$(" + toks[0] + ")";
		String val = get(toks[0]);
		if (val == null)
			return false;
		return Pattern.compile(unquote(get(toks[1]))).matcher(val).matches();
	}

	public Boolean compare(String value) throws Exception {
		set("_", like(value) ? "1" : "0");
		return true;
	}

	public Boolean _if(String value) throws Exception {
		if (isOn("$_"))
			return play(value);
		else
			return true;
	}

	public Boolean _else(String value) throws Exception {
		if (isOff("$_"))
			return play(value);
		else
			return true;
	}

	public Boolean _for(String value) throws Exception {
		String[] toks = split(value, 3, false);
		String forname = toks[0];
		int nb = getInt(toks[1]);
		for (int i = 0; i < nb; i++) {
			set(forname, i);
			if (play(toks[2]) == false)
				return false;
		}
		return true;
	}

	public Boolean sleep(String tmp) throws Exception {
		try {
			Thread.sleep(getInt(tmp) * 1000);
		} catch (Exception e) {
		}
		return true;
	}

	public Boolean sleepms(String tmp) throws Exception {
		try {
			Thread.sleep(getInt(tmp));
		} catch (Exception e) {
		}
		return true;
	}

	public Boolean ext(String value) throws Exception {
		String[] toks = split(value, false);
		Class cl = Class.forName(toks[1]);
		_extensions.put(get(toks[0]), (TestPlayer) cl.newInstance());
		return true;
	}

	public Boolean comment(String value) {
		return _commented = true;
	}

	public Boolean exit(String value) throws Exception {
		if (value == null)
			value = "0";
		System.exit(getInt(value));
		return true;
	}

	public Boolean not(String value) throws Exception {
		return !play(value);
	}

	public Boolean add(String value) throws Exception {
		String[] toks = split(value);
		set(toks[0], String.valueOf(getInt("$(" + toks[0] + ")") + asInt(toks[1])));
		return true;
	}

	public Boolean lower(String value) throws Exception {
		String[] toks = split(value);
		return getInt("$(" + toks[0] + ")") < asInt(toks[1]);
	}

	public Boolean higher(String value) throws Exception {
		String[] toks = split(value);
		return getInt("$(" + toks[0] + ")") > asInt(toks[1]);
	}

	/***************** utilities *************/

	public String getFile(FileReader reader, boolean resolve) throws Exception {
		StringBuilder sb = new StringBuilder();
		char c;
		while ((c = (char) reader.read()) != (char) -1)
			sb.append(c);
		reader.close();
		return resolve ? get(sb.toString()) : sb.toString();
	}

	public String getFile(File file, boolean resolve) throws Exception {
		if (file.exists() == false)
			return null;
		return getFile(new FileReader(file), resolve);
	}

	public String getFile(String file, boolean resolve) throws Exception {
		return getFile(newFile(file), resolve);
	}

	public File newFile(String name) throws Exception {
		return new File(getFileName(name));
	}

	public String getFileName(String file) throws Exception {
		if (file.startsWith("/"))
			return file;
		return get("$directory") + file;
	}

	public void writeFile(String name, String value) throws Exception {
		FileWriter writer = new FileWriter(name);
		writer.write(value);
		writer.close();
	}

	public static byte[] getBytes(String s) {
		byte[] ret = new byte[s.length()];
		for (int i = 0; i < s.length(); i++)
			ret[i] = (byte) s.charAt(i);
		return ret;
	}

	public static String getString(byte[] bytes, boolean quoted, boolean escape) {
		StringBuilder sb = new StringBuilder();
		if (quoted)
			sb.append("\"");
		for (byte b : bytes)
			sb.append((char) b);
		if (quoted)
			sb.append("\"");
		return escape ? escape(sb.toString()) : sb.toString();
	}

	public static String getBinaryString(byte[] bytes) {
		return getString(bytes, true, true);
	}

}
