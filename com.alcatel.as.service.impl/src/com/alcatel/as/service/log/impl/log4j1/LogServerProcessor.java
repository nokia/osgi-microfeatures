package com.alcatel.as.service.log.impl.log4j1;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import com.alcatel.as.ioh.server.TcpServer;
import com.alcatel.as.ioh.server.TcpServerProcessor;
import com.alcatel.as.ioh.tools.IOStreams;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.log.LogServiceFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

@Component(service = { TcpServerProcessor.class }, property = { "processor.id=logger", "processor.advertize.id=444" }, immediate = true)
public class LogServerProcessor implements TcpServerProcessor {

	private Logger _rootlogger;
	private Logger _logger;
	private final static String FILE = "FILE";
	private final static String APPENDER = "tcpAppender";
	private WriterAppender _appender;
	private OutputStream _out;
	private int _onlinesize;
	private ByteArrayOutputStream _temp;
	private String _tcplogger = "as.service.reactor.ioh.server.main.TcpChannelImpl";
	private Level _originalLevel;
	private AppenderSkeleton _originalAppender;
	private Priority _originalThreshold;
	private Priority _loggerlThreshold = Level.DEBUG;
	private PlatformExecutor _executor;
	private List<Runnable> _tasks = new LinkedList<Runnable>();

	class MemoryBuffer extends ByteArrayOutputStream {
		private int _max;

		MemoryBuffer(int max) {
			super(max);
			_max = max;
		}

		@Override
		public void write(byte[] b, int off, int len) {
			if (size() + len > _max) {
				reset();
			}
			super.write(b, off, len);
		}
	}

	@Reference
	public void setLogService(LogServiceFactory service) {
		// just to guarantee that the platform configuration is loaded before
		// starting this component
	}

	@Reference
	public void setPlatformExecutors(PlatformExecutors execs) {
		_executor = execs.getIOThreadPoolExecutor();
	}

	private void removeAppender() {
		if (_appender != null) {
			resetTasks();
			_rootlogger.removeAppender(_appender);
		}
	}

	private void resetTasks() {
		for (Runnable t : _tasks) {
			t.run();
		}
	}

	private void addTask(Runnable t) {
		_tasks.add(t);
	}

	private void removeTask(Runnable t) {
		_tasks.remove(t);
	}

	private void avoidLoop(boolean on) {
		Logger.getLogger(_tcplogger).setLevel(on ? Level.WARN : _originalLevel);
		_logger.info("avoidLoop " + _tcplogger + "=" + Logger.getLogger(_tcplogger).getLevel());
	}

	private void setAppender() {
		if (_originalAppender!=null) {
		_logger.info("appender adding...");
		if (_out != null) {
			_logger.info("appender connecting ...");
			if (_appender != null) {
				try {
					_logger.info("flushing ... " + _temp.size());
					_temp.writeTo(_out);
					_out.flush();
				} catch (IOException e) {
					_logger.error("cannot write buffer logs");
				} finally {
					_temp.reset();
				}
				removeAppender();
			}
			_appender = new WriterAppender(_originalAppender.getLayout(), new PrintStream(_out, true));
			_logger.info("appender connected.");
		} else {
			_appender = new WriterAppender(_originalAppender.getLayout(), _temp);
		}
		_appender.setName(APPENDER);
		_appender.setThreshold(_loggerlThreshold);
		_rootlogger.addAppender(_appender);
		_logger.info("appender added.");
		} else {
		_logger.error("logger processing not possible, missing FILE appender");
		}
	}

	@Activate
	public void activate(org.osgi.framework.BundleContext ctx, Map<String, String> props) {
		_rootlogger = Logger.getRootLogger();
		_logger = Logger.getLogger("as.ioh.server.logger");
		_originalAppender = (AppenderSkeleton) _rootlogger.getAppender(FILE);
		if (_originalAppender!=null) _originalThreshold = _originalAppender.getThreshold(); 
	}

	public void connectionAccepted(TcpServer server, TcpChannel client,
			java.util.Map<java.lang.String, java.lang.Object> props) {
		_logger.info("connectionAccepted");
		LoggerProcessor proc = new LoggerProcessor(client);
		client.attach(proc);
	}

	public TcpChannelListener getChannelListener(TcpChannel channel) {
		return (TcpChannelListener) channel.attachment();
	}

	public void serverCreated(TcpServer server) {
		_logger.info("serverCreated :  " + server + "\n" + server.getProperties().keySet());
	}

	public void serverDestroyed(TcpServer server) {
	}

	public void serverOpened(TcpServer server) {
		Map<String, Object> props = server.getProperties();
		_logger.info("serverOpened :" + props.keySet());
		String dynamicname = (String) props.get(TcpServer.PROP_SERVER_NAME);
		_tcplogger = "as.service.reactor.ioh.server." + dynamicname + ".TcpChannelImpl";
		_originalLevel = Logger.getLogger(_tcplogger).getLevel();
		_logger.info("serverOpened : keep control on processor logger = " + _tcplogger);
		String onlinebuffer = (String) props.get("online.buffer");
		String offlinebuffer = (String) props.get("offline.buffer");
		_onlinesize = onlinebuffer != null ? new Integer(onlinebuffer) : 4096;
		int _offlinesize = offlinebuffer != null ? new Integer(offlinebuffer) : 20480;
		_temp = new MemoryBuffer(_offlinesize);
		setAppender();
		_logger.info("serverOpened :  " + server + " with offline.buffer=" + _offlinesize + ",online.buffer="
				+ _onlinesize);
	}

	public void serverFailed(TcpServer server, java.lang.Object cause) {
	}

	public void serverUpdated(TcpServer server) {
	}

	public void serverClosed(TcpServer server) {
		removeAppender();
	}

	public String[][] getInfo(TcpServer server, String key) {
		return new String[0][];
	}

	private class LoggerProcessor implements TcpChannelListener {

		PipedOutputStream _input = null;
		boolean _close = false;

		protected LoggerProcessor(final TcpChannel socket) {
			try {
				_input = new PipedOutputStream();
				final PipedInputStream _in = new PipedInputStream(_input);
				_out = new BufferedOutputStream(IOStreams.getOutputStream(socket), _onlinesize);
				setAppender();
				_executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							avoidLoop(true);
							BufferedReader reader = new BufferedReader(new InputStreamReader(_in, "utf-8"));
							do {
								final String command = reader.readLine();
								_logger.warn("interpreting  [" + command + "]");
								if (command != null && !command.equals("")) {
									switch (command.toLowerCase()) {
									case "help":
										_logger.warn("\n\nhelp\t\t\t\t\tdisplays that help\nany text\t\t\t\t\tdefine a regexp message based filter\nreset\t\t\t\t\treset any changes\nshow\t\t\t\t\tshow filters/loggers active in that console\ndebug|info|warn|error\t\t\tchange the threshold\nX=LEVEL[,timeout][/any text]\t\tset the X logger to LEVEL during timeout(5s default) with an optional regexp based filter");
										break;
									case "reset":
										_appender.clearFilters();
										_appender.setThreshold(_loggerlThreshold);
										resetTasks();
										_logger.warn("filters reset");
										break;
									case "show":
										Filter f = _appender.getFilter();
										String sep = ">";
										StringBuilder show = new StringBuilder("------");
										show.append("\n\nFILTER=");
										while (f != null) {
											if (show.length() > 0)
												show.append(sep);
											show.append(f.toString());
											f = f.getNext();
										}
										show.append("\nTHRESHOLD=").append(_appender.getThreshold());
										show.append("\nTASKS=").append(_tasks.size());
										show.append("\nLOGGERS=");
										for (@SuppressWarnings("unchecked")
										Enumeration<Logger> loggers = LogManager.getCurrentLoggers(); loggers
												.hasMoreElements();) {
											Logger logger = loggers.nextElement();
											if (logger.getLevel() != null)
												show.append("\n\t").append(logger.getName()).append("=")
														.append(logger.getLevel());
										}
										_logger.warn(show);
										break;
									case "debug":
										_appender.setThreshold(Level.DEBUG);
										_logger.warn("setting threshold to " + Level.DEBUG);
										break;
									case "info":
										_appender.setThreshold(Level.INFO);
										_logger.warn("setting threshold to " + Level.INFO);
										break;
									case "warn":
										_appender.setThreshold(Level.WARN);
										_logger.warn("setting threshold to " + Level.WARN);
										break;
									case "error":
										_appender.setThreshold(Level.ERROR);
										_logger.warn("setting threshold to " + Level.ERROR);
										break;
									default: {
										String[] part3 = command.split("/");
										final String regexp = part3.length == 2 ? part3[1] : null;
										final String[] kv = part3[0].split("=");
										if (kv.length == 2) {
											try {
												final Logger l = Logger.getLogger(kv[0]);
												String[] part2 = kv[1].split(",");
												int timeout = part2.length == 2 ? new Integer(part2[1]) : 5;
												String level = part2[0];
												String msg = "temporarly (" + timeout + "s) setting log4j.logger."
														+ kv[0] + "=";
												final Level original = l.getLevel();
												_executor.schedule(new Runnable() {
													private Filter _filter = null;
													{
														if (regexp != null) {
															_filter = new Filter() {
																Pattern p = Pattern.compile(regexp);

																@Override
																public int decide(LoggingEvent event) {
																	String loggername = event.getLogger().getName();
																	if ((event.getLevel().equals(Level.WARN) && loggername
																			.equals(_logger.getName()))
																			|| (loggername.startsWith(l.getName()) && p
																					.matcher(
																							event.getMessage()
																									.toString()).find())) {
																		return Filter.NEUTRAL;
																	}
																	return Filter.DENY;
																}

																@Override
																public String toString() {
																	return l.getName() + "/" + regexp;
																}
															};
															_appender.addFilter(_filter);
															_logger.warn("add filter " + _filter);
														}
														addTask(this);
													}

													@Override
													public void run() {
														l.setLevel(original);
														if (_originalAppender!=null) _originalAppender.setThreshold(_originalThreshold);
														if (_filter != null)
															_appender.clearFilters();
														removeTask(this);
														_logger.warn("resetting log4j.logger." + kv[0] + "=" + original);
													}
												}, timeout, TimeUnit.SECONDS);
												if (_originalAppender!=null) _originalAppender.setThreshold(Level.WARN);
												l.setLevel(Level.toLevel(level));
												_logger.warn(msg + level);
											} catch (Throwable t) {
												_logger.error("cannot perform " + command, t);
											}
										} else {
											Filter filter = new Filter() {
												final String _c = command;
												Pattern p = Pattern.compile(_c);

												@Override
												public int decide(LoggingEvent event) {
													if (event.getLogger().getName().equals(_logger.getName())
															|| p.matcher(event.getMessage().toString()).find()) {
														return Filter.NEUTRAL;
													}
													return Filter.DENY;
												}

												@Override
												public String toString() {
													return _c;
												}
											};
											_appender.addFilter(filter);
											_logger.warn("filter added [" + command + "]");
										}
									}
									}
								}
							} while (!_close);
							reader.close();
							_in.close();
						} catch (Throwable e) {
							_logger.warn("Exception in command reader", e);
						} finally {
							socket.close();
							avoidLoop(false);
						}
					}
				});
				socket.enableReading();
			} catch (Throwable t) {
				_logger.fatal("cannot process", t);
			}
		}

		@Override
		public void connectionClosed(TcpChannel cnx) {
			try {
				if (_input != null)
					_input.close();
				if (_out != null)
					_out.close();
			} catch (Throwable e) {
			} finally {
				_out = null;
				_close = true;
			}
			setAppender();
		}

		@Override
		public void writeUnblocked(TcpChannel cnx) {
		}

		@Override
		public void writeBlocked(TcpChannel cnx) {
		}

		@Override
		public void receiveTimeout(TcpChannel cnx) {
		}

		@Override
		public int messageReceived(TcpChannel cnx, java.nio.ByteBuffer msg) {
			if (_input != null) {
				try {
					while (msg.hasRemaining())
						_input.write(msg.get());
				} catch (Throwable e) {
				}// cannot happen
			} else
				msg.clear();
			return 0;
		}
	}
}
