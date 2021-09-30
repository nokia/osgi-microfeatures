package com.alcatel.as.ioh.impl.tools;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.log4j.Logger;
import org.jline.builtins.Commands;
import org.jline.builtins.Options.HelpException;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.alcatel.as.ioh.server.TcpServer;
import com.alcatel.as.ioh.server.TcpServerProcessor;
import com.alcatel.as.ioh.tools.IOStreams;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.log.service.admin.LogAdmin;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

@Component(service = { TcpServerProcessor.class }, factory = "gogoProcessor")
public class GogoServerProcessor implements TcpServerProcessor {

	private CommandProcessor _cmdProc;
	private Logger LOGGER = Logger.getLogger("as.ioh.server.gogo");
	private boolean _shell, _client;

	@Reference
	public void setCommandProcessor(CommandProcessor proc) {
		_cmdProc = proc;
	}

	@Reference
	public void setPlatformExecutors(PlatformExecutors execs) {
		GogoCommandsUtils.setPlatformExecutors(execs);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
	public void addLogAdmin(LogAdmin logAdmin) {
		GogoCommandsUtils.addLogAdmin(logAdmin);
	}

	public void removeLogAdmin(LogAdmin logAdmin) {
		GogoCommandsUtils.removeLogAdmin(logAdmin);
	}

	@Activate
	public void activate(org.osgi.framework.BundleContext ctx, Map<String, String> props) {
		_shell = "gogo.shell".equals(props.get("processor.id"));
		_client = "gogo.client".equals(props.get("processor.id")); // this is used with the jline based client
		GogoCommandsUtils.registerCommands(ctx);
	}

	public void connectionAccepted(TcpServer server, TcpChannel client,
			java.util.Map<java.lang.String, java.lang.Object> props) {
		LOGGER.info("connectionAccepted");
		GogoProcessor proc = _shell ? new GogoShellProcessor(client, props) : new GogoCommandProcessor(client, props);
		client.attach(proc);
		proc.run(client, props);
	}

	public TcpChannelListener getChannelListener(TcpChannel channel) {
		return (TcpChannelListener) channel.attachment();
	}

	public void serverCreated(TcpServer server) {
	}

	public void serverDestroyed(TcpServer server) {
	}

	public void serverOpened(TcpServer server) {
	}

	public void serverFailed(TcpServer server, java.lang.Object cause) {
	}

	public void serverUpdated(TcpServer server) {
	}

	public void serverClosed(TcpServer server) {
	}

	public String[][] getInfo(TcpServer server, String key) {
		return new String[0][];
	}

	private abstract class GogoProcessor implements TcpChannelListener {
		protected PipedOutputStream _clientOutputPipe;
		protected PipedInputStream _clientInput;
		protected PrintStream _clientOutput;
		protected CommandSession _session;
		protected String _prompt = "g> ";

		protected GogoProcessor(TcpChannel socket, Map<String, Object> props) {
			try {
				_clientOutputPipe = new PipedOutputStream();
				_clientInput = new PipedInputStream(_clientOutputPipe);
			} catch (Throwable t) {
			} // cannot happen
			_clientOutput = new PrintStream(new BufferedOutputStream(IOStreams.getOutputStream(socket)), true);
			_session = _cmdProc.createSession(new DataInputStream(_clientInput), _clientOutput, _clientOutput);
			_session.put("gogo.ioh.channel", socket);
			for (Map.Entry<String, Object> entry : props.entrySet().toArray(new Map.Entry[0]))
				_session.put(entry.getKey(), entry.getValue());
		}

		protected void run(TcpChannel socket, Map<String, Object> props) {
		}

		public void receiveTimeout(TcpChannel cnx) {
			cnx.close();
		}

		public void writeBlocked(TcpChannel cnx) {
			cnx.close();
		}

		public void writeUnblocked(TcpChannel cnx) {
		}

		public int messageReceived(TcpChannel cnx, java.nio.ByteBuffer msg) {
			if (_clientOutputPipe != null) {
				try {
					while (msg.hasRemaining())
						_clientOutputPipe.write(msg.get());
				} catch (Throwable e) {
				} // cannot happen
			} else
				msg.clear();
			return 0;
		}

		public void connectionClosed(TcpChannel cnx) {
			try {
				if (_clientOutputPipe != null)
					_clientOutputPipe.close();
			} catch (Throwable e) {
			} // cannot happen
		}

		protected void execute(TcpChannel socket, String command) throws Exception {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Running command : " + command);
			try {
				Object result = _session.execute(command);
				if (result != null && !Boolean.FALSE.equals(_session.get(".Gogo.format"))) {
					_clientOutput.println(_session.format(result, Converter.INSPECT));
				}
				sendSeparator(socket);
			}

			catch (Exception e) {
				if (e.getClass().toString().indexOf("CommandNotFoundException") != -1) {
					socket.send(java.nio.ByteBuffer.wrap(("CommandNotFoundException: " + command + "\n").getBytes("ascii")), false);
				} else {
					socket.send(java.nio.ByteBuffer.wrap((e.getMessage() + "\n").getBytes("ascii")), false);
				}
				sendSeparator(socket);
			}
		}

		protected boolean sendSeparator(TcpChannel socket) { 
			return false; // no separator sent
		}
	}

	private class GogoShellProcessor extends GogoProcessor {
		private GogoShellProcessor(final TcpChannel socket, Map<String, Object> props) {
			super(socket, props);
			_session.put("gogo.shell", false);
		}

		protected void run(final TcpChannel socket, Map<String, Object> props) {
			try {
				Thread handler = new Thread() {
					public void run() {
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(_clientInput, "utf-8"))) {
							String line = null;

							while (true) {
								_clientOutput.print(_prompt);
								line = reader.readLine();
								if (line != null) {
									line = line.trim();
									if (line.length() > 0) {
										execute(socket, line);
									}
								} else {
									// socket closed
									break;
								}
							}
						} catch (Throwable e) {
							LOGGER.warn("Exception while running gosh", e);
						} finally {
							_session.close();
							socket.close();
						}
					}
				};
				handler.start();
				socket.enableReading();
			}

			catch (Exception e) {
				LOGGER.warn("could not accept gogo command socket from " + socket, e);
				socket.close();
			}
		}
	}

	// not used, just here to test if jline works with telnet
	private class GogoShellJlineProcessor extends GogoProcessor {
		private GogoShellJlineProcessor(final TcpChannel socket, Map<String, Object> props) {
			super(socket, props);
			_session.put("gogo.shell", false);
		}

		protected void run(final TcpChannel socket, Map<String, Object> props) {
			try {
				String prompt = "g> ";
				TerminalBuilder builder = TerminalBuilder.builder();
				Completer completer = new StringsCompleter();
				Parser parser = null;
				String rightPrompt = null;

				Terminal terminal = new DumbTerminal(_clientInput, _clientOutput);
				LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(completer).parser(parser)
						.variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ").build();

				Thread handler = new Thread() {
					public void run() {
						try {
							while (true) {
								String line = null;
								try {
									line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
									line = line.trim();
									if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
										break;
									}
									if ("history".equals(line)) {
										ParsedLine pl = reader.getParser().parse(line, 0);
										String[] argv = pl.words().subList(1, pl.words().size()).toArray(new String[0]);
										Commands.history(reader, _clientOutput, _clientOutput, argv);
									} else {
										execute(socket, line);
									}
								} catch (HelpException e) {
									HelpException.highlight(e.getMessage(), HelpException.defaultStyle())
											.print(terminal);
								} catch (IllegalArgumentException e) {
									LOGGER.warn("exception", e);
								} catch (UserInterruptException e) {
									// Ignore
								} catch (EndOfFileException e) {
									LOGGER.debug("eof, closing");
									return;
								}
							}
						} catch (Throwable e) {
							LOGGER.warn("Exception while running gosh", e);
						} finally {
							_session.close();
							socket.close();
						}
					}
				};
				handler.start();
				socket.enableReading();
			}

			catch (Exception e) {
				LOGGER.warn("could not accept gogo command socket from " + socket, e);
				socket.close();
			}
		}
	}

	private class GogoCommandProcessor extends GogoProcessor {
		private boolean _noClose;
		private long _daemon;
		private byte[] _sep;

		private GogoCommandProcessor(final TcpChannel socket, Map<String, Object> props) {
			super(socket, props);
			if (_client) {
				_noClose = true;
				if (props.get("gogo.command.separator") == null)
					props.put("gogo.command.separator", ".done");
			} else {
				_noClose = "false".equals(props.get("gogo.close"));
			}
			try {
				String sep = (String) props.get("gogo.command.separator");
				if (sep != null)
					_sep = (sep + "\n").getBytes("ascii");
			} catch (Exception e) {
			}
			Object daemon = props.get("gogo.daemon");
			if (daemon != null) {
				String s = (String) daemon;
				long unit = 1000L;
				if (s.endsWith("ms")) {
					s = s.substring(0, s.length() - 2);
					unit = 1L;
				} else if (s.endsWith("s")) {
					s = s.substring(0, s.length() - 1);
					unit = 1000L;
				}
				_daemon = Long.parseLong(s) * unit;
			}
			_session.put("gogo.shell", false);
		}

		private List<String> getCommands(Object o) {
			if (o instanceof String) {
				ArrayList<String> ret = new ArrayList<String>(1);
				ret.add((String) o);
				return ret;
			}
			return (List<String>) o;
		}

		@Override
		protected boolean sendSeparator(TcpChannel socket) {
			if (_sep != null) {
				socket.send(java.nio.ByteBuffer.wrap(_sep), false);
				return true;
			}
			return false;
		}

		protected void run(final TcpChannel socket, Map<String, Object> props) {
			socket.enableReading();
			final Object o = props.get("gogo.command");
			if (o != null) {
				Thread handler = new Thread() {
					public void run() {
						List<String> commands = getCommands(o);
						try {
							if (_daemon == 0) {
								for (String command : commands) {
									execute(socket, command);
								}
							} else {
								while (!socket.isClosed()) {
									for (String command : commands) {
										execute(socket, command);
									}
									Thread.sleep(_daemon);
								}
							}
						} catch (Throwable t) {
							LOGGER.warn("Exception in GogoCommandProcessor when executing : " + commands, t);
						} finally {
							_session.close();
							socket.close();
						}
					}
				};
				handler.start();
			} else {
				Thread handler = new Thread() {
					public void run() {
						try {
							BufferedReader reader = new BufferedReader(new InputStreamReader(_clientInput, "utf-8"));
							do {
								String command = reader.readLine();
								if (command != null) {
									if (_daemon == 0) {
										execute(socket, command);
									} else {
										while (!socket.isClosed()) {
											execute(socket, command);
											Thread.sleep(_daemon);
										}
										break;
									}
								} else {
									break;
								}
							} while (_noClose);
						} catch (Throwable e) {
							LOGGER.warn("Exception in GogoCommandProcessor", e);
						} finally {
							_session.close();
							socket.close();
						}
					}
				};
				handler.start();
			}
		}
	}

}
