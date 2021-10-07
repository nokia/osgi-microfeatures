package com.alcatel.as.felix;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.felix.main.Main;

import com.alcatel_lucent.as.management.annotation.config.*;

/**
 * Class used to bootstrap a knopflerfish framework.
 */
@Config(name="felix", section="Felix Configuration")
public class FelixLauncher
{
    @FileDataProperty(title="JVM Parameters",
		      help="Configuration of the JVM options used to launch the Felix OSGi framework",
		      fileData="jvm.opt",
		      required=true)
		      public final static String JVM_CONFIGURATION  = "jvm.configuration";

    @FileDataProperty(title="Application JVM Parameters",
		      help="Application configuration of the JVM options used to launch the Felix OSGi framework",
		      fileData="user.jvm.opt",
		      required=true)
		      public final static String USER_JVM_CONFIGURATION  = "user.jvm.configuration";

  @FileDataProperty(title="Osgi Felix framework configuration",
      help="Configuration of the properties used to launch the Felix OSGi framework",
      fileData="felix.properties",
      required=true)
  public final static String OSGI_CONFIGURATION  = "osgi.configuration";

  @FileDataProperty(title="Java Util Logging Configuration",
      help="Configuration used to initialize Java Util Logging",
      fileData="jul.properties",
      required=true)
  public final static String JUL_CONFIGURATION  = "jul.configuration";
    
  @FileDataProperty(title="Bundle Start Levels",
	      help="This file contains specific bundle symbolic name start levels. By default bundles are using a start level 25. If for whatever reasons, " +
	    	   "a bundle start ordering must be configured, you can configure in this file a list of bundle symbolic name start levels. Lower start levels are started before" +
	    	   "higher start levels. The start level -1 is special and means that a bundle must not be installed/started.",
	      fileData="startlevel.txt",
	      required=true)
  public final static String START_LEVEL  = "start.levels";
    
  // --- Public Attributes

    public final static FelixLauncher instance = new FelixLauncher();

    // --- Public methods

    public static void main(String... args) throws Exception
    {
        if (args.length != 8)
        {
            System.err.println("Usage: FelixLauncher <Felix property file> <modname> <instname> <modid> <host> <pid> <fchost> <sudo flag>");
            System.exit(1);
        }

        // Setup platform parameters.
        if (System.getProperty("INSTALL_DIR") == null)
        {
            // may be set as -DINSTALL_DIR independently of system env
            System.setProperty("INSTALL_DIR", System.getenv("INSTALL_DIR"));
        }
        String instname = args[2];
        if (System.getProperty("platform.name") != null
            && System.getProperty("group.name") != null
            && System.getProperty("component.name") != null
            && System.getProperty("instance.name") != null)
        {
          instname = System.getProperty("platform.name") 
            +"."+ System.getProperty("group.name")
            +"__"+ System.getProperty("component.name")
            +"."+ System.getProperty("instance.name");
        }

        System.setProperty("platform.agent.moduleName", args[1]);
        System.setProperty("platform.agent.instanceName", instname);
        System.setProperty("platform.agent.moduleId", args[3]);
        System.setProperty("platform.agent.host", args[4]);
        System.setProperty("platform.agent.pid", args[5]);
        System.setProperty("platform.agent.fcHost", args[6]);
        System.setProperty("platform.agent.sudo", args[7]);

	String redirectStdout = System.getenv("CASR_LOGSTDOUT");
	if (redirectStdout == null) {
	    // redirect stdout to var/log/instname/msg.log ...
	    PrintStream stdoutPS = new PrintStream(new StdoutStream(instname), true);
	    System.setOut(stdoutPS);
	    System.setErr(stdoutPS);
	}
	// Launch the osgi framework.
	String felixPropsPath = args[0];
	if (!new File(felixPropsPath).exists())
	{
	    System.out.println("FATAL: Felix properties configuration file does not exist: "+felixPropsPath);
	    System.out.println("EXIT");
	    System.exit(1);
	}
        System.setProperty(Main.CONFIG_PROPERTIES_PROP, "file:" + felixPropsPath);
        System.setProperty(Main.SHUTDOWN_HOOK_PROP, "false");

        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S").format(new Date())
                + " Starting Felix OSGi Framework ...");
        Main.main(new String[0]);
    }

    // ---------------- Private attributes

    private final static String LINE_SEPARATOR = System.getProperty("line.separator");

    private final static PrintStream OUT =
            new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 128));

    private final static PrintStream ERR =
            new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.err), 128));

    /**
     * Redirects stdout/stderr to the default log4j logger.
     */
    private static class StdoutStream extends OutputStream
    {
        private final PrintStream _ps;

        StdoutStream(String instname) throws IOException
        {
            File logDir = new File(System.getProperty("INSTALL_DIR", ".") + "/var/log/" + instname);
            if (!logDir.exists() && !logDir.mkdirs())
            {
                throw new IOException("Could not open logdir: " + logDir);
            }
            File dir = new File(System.getProperty("INSTALL_DIR") + "/var/log/" + instname);
            if (!dir.isDirectory())
            {
                if (!dir.mkdirs())
                {
                    throw new RuntimeException("Could not create directory " + dir);
                }
            }
            _ps = new PrintStream(new FileOutputStream(dir.getAbsolutePath() + "/felix.log", false));
        }

        @Override
        public void write(final int data) throws IOException
        {
            buf.append((char) data);
        }

        @Override
        public void close() throws IOException
        {
            if (_ps != null)
            {
                _ps.close();
            }
        }

        @Override
        public void flush() throws IOException
        {
            if (buf.length() > 0)
            {
                final String s = buf.toString();
                if (!s.equals(LINE_SEPARATOR))
                {
                    // detect recursive call to flush ...
                    StackTraceElement[] st = Thread.currentThread().getStackTrace();
                    int flushCall = 0;
                    boolean recursiveCallDetected = false;
                    for (int i = 0; i < st.length; i++)
                    {
                        if (st[i].getClassName().equals(getClass().getName()))
                        {
                            if (++flushCall > 1)
                            {
                                recursiveCallDetected = true;
                                break;
                            }
                        }
                    }

                    if (!recursiveCallDetected)
                    {
                        try
                        {
                            if (s.endsWith(System.getProperty("line.separator")))
                            {
                                _ps.print(s);
                            }
                            else
                            {
                                _ps.println(s);
                            }
                            _ps.flush();
                        }
                        catch (Throwable e)
                        {
                            e.printStackTrace(ERR);
                            ERR.println(s);
                            ERR.flush();
                        }
                    }
                    else
                    {
                        OUT.println(s);
                        OUT.flush();
                    }
                }

                buf.setLength(0);
            }
        }

        private final StringBuffer buf = new StringBuffer();
    }
}
