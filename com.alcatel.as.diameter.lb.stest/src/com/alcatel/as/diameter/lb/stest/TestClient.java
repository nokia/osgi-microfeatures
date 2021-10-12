// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.stest;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.DiameterUtils.Avp;

public class TestClient
{

    static Map<String, Socket> _sockets = new HashMap<String, Socket>();
    static Map<String, ServerSocket> _servers = new HashMap<String, ServerSocket>();
    static Map<String, List<String>> _acceptSockets = new HashMap<String, List<String>>();
    static Map<String, Avp> _avps = new HashMap<String, Avp>();
    static DiameterMessage _req, _resp;

    public static void main(String[] args) throws Exception
    {
        setProperty("pause", "50");

        setProperty("log", "1");
        for (String arg : args)
        {
            File file = new File(arg);
            if (file.isDirectory())
            {
                String[] files = file.list(new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".txt");
                    }
                });
                for (String scenario : files)
                    check(execute(arg + File.separator + scenario));
            }
            else
            {
                check(execute(arg));
            }
        }
        System.exit(0);
    }

    private static boolean execute(String scenario) throws Exception
    {
        TestClient client = new TestClient();
        boolean res = client.playFile(scenario);
        client.close();
        return res;
    }

    private static void check(boolean status)
    {
        if (!status && "true".equals(getProperty("exit-on-error")))
        {
            System.exit(1);
        }
    }

    private static boolean log(boolean res)
    {
        if (res)
            System.out.printf(" \tOK\n");
        else
            System.out.printf(" \t\tKO\t<<<--------------------\n");
        return res;
    }

    public TestClient()
    {
    }

    private void close() throws Exception
    {
        for (Socket socket : _sockets.values())
            socket.close();
        for (ServerSocket socket : _servers.values())
            socket.close();
        Thread.sleep(200); // let the sockets close
    }

    private static String resolve(String name)
    {
        int i = name.indexOf('[');
        if (i > -1)
        {
            int j = name.indexOf(']', i);
            return resolve((i > 0 ? name.substring(0, i) : "") + System.getProperty(name.substring(i + 1, j))
                    + (j != name.length() - 1 ? name.substring(j + 1) : ""));
        }
        return name;
    }

    private static int resolveInt(String name)
    {
        int i = name.indexOf('[');
        if (i > -1)
        {
            int j = name.indexOf(']', i);
            return resolveInt((i > 0 ? name.substring(0, i) : "")
                    + System.getProperty(name.substring(i + 1, j))
                    + (j != name.length() - 1 ? name.substring(j + 1) : ""));
        }
        return Integer.parseInt(name);
    }

    private static String getProperty(String name)
    {
        String s = System.getProperty(name);
        if (s == null || s.equals("null"))
            return null;
        return s;
    }

    private static String setProperty(String name, String value)
    {
        name = resolve(name);
        value = resolve(value);
        System.setProperty(name, value);
        return value;
    }

    private static String[][] parseLine(String line)
    {
        int i = line.indexOf(':');
        String cmd = line.substring(0, i).trim().toLowerCase().replace("-", "");
        String value = line.substring(i + 1).trim();
        return new String[][] { { cmd, value }, value.split(" ") };
    }

    public boolean playFile(String filename) throws Exception
    {
        System.out.printf("*** Playing Scenario : [%s]\n", filename);
        FileReader reader = new FileReader(filename);
        StringBuilder sb = new StringBuilder();
        char c;
        while ((c = (char) reader.read()) != (char) -1)
            sb.append(c);
        reader.close();
        boolean res = playScenario(sb.toString());
        System.out.printf("*** Done Playing Scenario : [%s] : [%b]\n", filename, res);
        return res;
    }

    public boolean playScenario(String scenario) throws Exception
    {
        boolean res = true;
        BufferedReader reader = new BufferedReader(new StringReader(scenario));
        try
        {
            while (res)
            {
                String line = reader.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#"))
                    continue;
                String[][] args = parseLine(line);
                if (args[0][0].equals("for"))
                {
                    String forname = args[1][0];
                    int nb = resolveInt(args[1][1]);
                    StringBuilder sb = new StringBuilder();
                    while (true)
                    {
                        line = reader.readLine().trim();
                        if (line.length() == 0 || line.startsWith("#"))
                            continue;
                        String[][] tmp = parseLine(line);
                        if (tmp[0][0].equals("endfor") && tmp[1][0].equals(forname))
                            break;
                        sb.append(line).append('\n');
                    }
                    for (int iter = 0; iter < nb; iter++)
                        playScenario(sb.toString());
                }
                else
                {
                    res = playLine(line);
                }
                Thread.sleep(resolveInt("[pause]"));
            }
        }
        finally
        {
            Thread.sleep(resolveInt("[pause]"));
            reader.close();
        }
        return res;
    }

    private boolean playLine(String line) throws Exception
    {
        boolean log = resolveInt("[log]") == 1;
        int i = line.indexOf(':');
        String cmd = i != -1 ? line.substring(0, i).trim() : line.trim();
        String value = i != -1 ? line.substring(i + 1).trim() : null;
        cmd = cmd.toLowerCase().replace("-", "");
        if (cmd.equals("log"))
        {
            System.out.printf(cmd + " : " + value + "\n");
            return true;
        }
        else if (log)
            System.out.printf("Executing [%s]", line);
        Method method = TestClient.class.getDeclaredMethod(cmd, String.class);
        boolean res = (Boolean) method.invoke(this, value);
        if (res)
        {
            if (log)
                log(res);
        }
        else
        {
            if (!log)
                System.out.printf("Executing [%s]", line);
            log(res);
        }
        return res;
    }

    public Boolean conf(String value)
    {
        value = value.trim();
        int firstSpace = value.indexOf(" ");
        setProperty(value.substring(0, firstSpace), value.substring(firstSpace + 1));
        return true;
    }

    public Boolean sleep(String tmp) throws Exception
    {
        try
        {
            Thread.sleep(resolveInt(tmp) * 1000);
        }
        catch (Exception e)
        {
        }
        return true;
    }

    public Boolean sleepms(String tmp) throws Exception
    {
        try
        {
            Thread.sleep(resolveInt(tmp));
        }
        catch (Exception e)
        {
        }
        return true;
    }

    public Boolean tcpopen(String name) throws Exception
    {
        Socket socket = new Socket(getProperty("ip"), Integer.parseInt(getProperty("port")));
        _sockets.put(name, socket);
        return true;
    }

    public Boolean tcplisten(String value) throws Exception
    {
        String[] props = value.split(" ");
        final String name = props[0];
        final ServerSocket ss = new ServerSocket(Integer.parseInt(props[1]));
        _servers.put(props[0], ss);
        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    while (true)
                    {
                        Socket socket = ss.accept();
                        List<String> list = _acceptSockets.get(name);
                        String name = list.remove(0);
                        _sockets.put(name, socket);
                    }
                }
                catch (Exception e)
                {
                }
            }
        };
        new Thread(r).start();
        return true;
    }

    public Boolean tcpaccept(String value) throws Exception
    {
        String[] props = value.split(" ");
        List<String> list = _acceptSockets.get(props[0]);
        if (list == null)
        {
            list = new ArrayList<String>();
            _acceptSockets.put(props[0], list);
        }
        list.add(props[1]);
        return true;
    }

    public Boolean tcpclose(String value) throws Exception
    {
        Socket socket = _sockets.remove(value);
        socket.close();
        return true;
    }

    public Boolean tcpclosed(String value) throws Exception
    {
        Socket socket = _sockets.remove(value);
        return socket.getInputStream().read() == -1;
    }

    public Boolean avp(String value) throws Exception
    {
        String[] props = value.split(" ");
        String name = props[0];
        byte[] avpvalue = null;
        if (props[3].startsWith("/"))
        {
            int i = Integer.parseInt(props[3].substring(1));
            avpvalue = DiameterUtils.setIntValue(i, new byte[4], 0);
        }
        else
        {
            avpvalue = props[3].getBytes("ascii");
        }
        Avp avp = new Avp(Integer.parseInt(props[1]), Integer.parseInt(props[2]), false, avpvalue);
        _avps.put(name, avp);
        return true;
    }

    public Boolean tcpsendbin(String value) throws Exception
    {
        String[] props = value.split(" ");
        Socket clientSocket = _sockets.get(props[0]);
        for (int i = 1; i < props.length; i++)
        {
            String prop = props[i];
            int p = prop.indexOf('x');
            int radix = 10;
            if (p > -1)
            {
                radix = Integer.parseInt(prop.substring(0, p));
                prop = prop.substring(p + 1);
            }
            int v = Integer.parseInt(prop, radix);
            clientSocket.getOutputStream().write(v);
        }
        clientSocket.getOutputStream().flush();
        return true;
    }

    public Boolean tcpsendreq(String value) throws Exception
    {
        String[] props = value.split(" ");
        Socket clientSocket = _sockets.get(props[0]);
        Avp[] avps = new Avp[props.length - 3];
        for (int i = 3; i < props.length; i++)
        {
            avps[i - 3] = _avps.get(props[i]);
        }
        DiameterMessage msg = DiameterUtils.makeRequest(resolveInt(props[1]), resolveInt(props[2]), avps);
        msg.updateHopIdentifier((int) System.currentTimeMillis() & 0xFFFF);
        clientSocket.getOutputStream().write(msg.getBytes());
        clientSocket.getOutputStream().flush();
        return true;
    }

    public Boolean tcpsendresp(String value) throws Exception
    {
        String[] props = value.split(" ");
        Socket clientSocket = _sockets.get(props[0]);
        Avp[] avps = new Avp[props.length - 1];
        for (int i = 3; i < props.length; i++)
        {
            avps[i - 3] = _avps.get(props[i]);
        }
        DiameterMessage msg = DiameterUtils.makeResponse(_req, avps);
        clientSocket.getOutputStream().write(msg.getBytes());
        clientSocket.getOutputStream().flush();
        return true;
    }

    public Boolean tcpreadreq(String value) throws Exception
    {
        _req = tcpreadmsg(value);
        return _req.isRequest();
    }

    public Boolean tcpreadresp(String value) throws Exception
    {
        _resp = tcpreadmsg(value);
        return _resp.isRequest() == false;
    }

    public Boolean respchecklatency(String value)
    {
        Integer max = resolveInt(value);
        int sent = _resp.getHopIdentifier() & 0xFFFF;
        int now = (int) System.currentTimeMillis() & 0xFFFF;
        if ((now - sent) > max)
        {
            System.out.println(now - sent + "/" + max + " ---> " + sent);
            //System.exit (1);
        }
        return (now - sent) <= max;
    }

    private DiameterMessage tcpreadmsg(String value) throws Exception
    {
        String[] props = value.split(" ");
        Socket socket = _sockets.get(props[0]);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = socket.getInputStream();
        int i = in.read();
        baos.write(i);
        int len = 0;
        i = in.read();
        len = i;
        baos.write(i);
        i = in.read();
        len <<= 8;
        len |= i;
        baos.write(i);
        i = in.read();
        len <<= 8;
        len |= i;
        baos.write(i);
        for (i = 4; i < len; i++)
        {
            baos.write(in.read());
        }
        return new DiameterMessage(baos.toByteArray());
    }

    public Boolean reqcheck(String value) throws Exception
    {
        String[] props = value.split(" ");
        return (_req.getApplicationID() == resolveInt(props[0]) && _req.getCommandCode() == resolveInt(props[1]));
    }

    public Boolean respcheck(String value) throws Exception
    {
        String[] props = value.split(" ");
        return (_resp.getApplicationID() == resolveInt(props[0]) && _resp.getCommandCode() == resolveInt(props[1]));
    }

    public Boolean respcheckavp(String value) throws Exception
    {
        String[] props = value.split(" ");
        byte[] avp = _resp.getAvp(resolveInt(props[0]), resolveInt(props[1]));
        return avp != null;
    }

    public Boolean respcheckavplen(String value) throws Exception
    {
        String[] props = value.split(" ");
        byte[] avp = _resp.getAvp(resolveInt(props[0]), resolveInt(props[1]));
        return avp != null && avp.length == resolveInt(props[2]);
    }

    public Boolean send(String value) throws Exception
    {
        String[] props = value.split(" ");
        Socket clientSocket = _sockets.get(props[0]);
        String file = props[1];
        InputStream fis = getInputStream(new FileInputStream(file));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = 0;
        while ((i = fis.read()) != -1)
        {
            if (i == '\n')
                baos.write('\r');
            baos.write(i);
        }
        fis.close();
        clientSocket.getOutputStream().write(baos.toByteArray());
        clientSocket.getOutputStream().flush();
        return true;
    }

    public Boolean read(String value) throws Exception
    {
        String[] props = value.split(" ");
        Socket clientSocket = _sockets.get(props[0]);
        String file = props[1];
        FileInputStream fis = new FileInputStream(file);
        int i1 = 0;
        while ((i1 = fis.read()) != -1)
        {
            boolean var = false;
            String name = null;
            String val = null;
            if (i1 == '{')
            {
                var = true;
                name = "";
                while ((i1 = fis.read()) != '}')
                    name = "" + (char) i1;
                val = "";
                i1 = fis.read();
            }
            if (i1 == '[')
            {
                name = "";
                while ((i1 = fis.read()) != ']')
                    name = name + (char) i1;
                val = System.getProperty(name);
                byte[] valb = val.getBytes();
                for (int k = 0; k < valb.length; k++)
                {
                    int i = clientSocket.getInputStream().read();
                    if (i != valb[k])
                        return false;
                }
                continue;
            }
            do
            {
                int i2 = clientSocket.getInputStream().read();
                if (i2 == '\r')
                { // skip CR
                    i2 = clientSocket.getInputStream().read();
                }
                if (var)
                {
                    if (i1 == i2)
                    {
                        System.setProperty(name, val);
                        var = false;
                    }
                    else
                    {
                        val = val + (char) i2;
                    }
                }
                else
                {
                    if (i1 != i2)
                        return false;
                }
            } while (var);
        }
        fis.close();
        return true;
    }

    private InputStream getInputStream(final InputStream in)
    {
        return new InputStream()
        {
            private ByteArrayInputStream _bin;

            public void close() throws IOException
            {
                in.close();
            }

            public int read() throws IOException
            {
                if (_bin != null)
                {
                    int i = _bin.read();
                    if (i != -1)
                        return i;
                    _bin = null;
                }
                int i = in.read();
                if (i == -1)
                    return i;
                if (i == '[')
                {
                    String name = "";
                    while ((i = in.read()) != ']')
                        name = name + (char) i;
                    _bin = new ByteArrayInputStream(System.getProperty(name).getBytes());
                    return _bin.read();
                }
                return i;
            }
        };
    }
}
