package com.alcatel.as.util.cl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Dynamic proxy used to set Application Thread Context class Loader at Application method
 * invocation time.
 */
public class ApplicationCCLSetter implements InvocationHandler
{
    protected Object _app;

    protected ClassLoader _appCL;

    public static Object newInstance(ClassLoader cl, Object app)
    {
        return java.lang.reflect.Proxy.newProxyInstance(cl,
                                                        getAllInterfaces(app.getClass()),
                                                        new ApplicationCCLSetter(app, cl));
    }

    protected ApplicationCCLSetter(Object app, ClassLoader appCL)
    {
        _app = app;
        _appCL = appCL;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
    {
        Thread currThread = Thread.currentThread();
        ClassLoader currThreadCL = currThread.getContextClassLoader();
        Object result = null;

        try
        {
            currThread.setContextClassLoader(_appCL);
            result = m.invoke(_app, args);
        }

        catch (InvocationTargetException e)
        {
            throw e.getTargetException();
        }

        catch (Exception e)
        {
            throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
        }

        finally
        {
            currThread.setContextClassLoader(currThreadCL);
        }

        return result;
    }

    public Object getApplication()
    {
        return _app;
    }

    public static Object getApplication(Object app)
    {
        if (Proxy.isProxyClass(app.getClass()))
        {
            return ((ApplicationCCLSetter) Proxy.getInvocationHandler(app))._app;
        }
        else
        {
            return app;
        }
    }

    @SuppressWarnings("unchecked")
    public static Class getApplicationClass(Object app)
    {
        return getApplication(app).getClass();
    }

    @SuppressWarnings("unchecked")
    protected static Class[] getAllInterfaces(Class cls)
    {
        Set set = new HashSet();
        while (cls != null)
        {
            Class[] intfaces = cls.getInterfaces();
            set.addAll(Arrays.asList(intfaces));
            for (int i = 0; i < intfaces.length; i++)
            {
                set.addAll(Arrays.asList(getAllInterfaces(intfaces[i])));
            }
            cls = cls.getSuperclass();
        }
        return (Class[]) set.toArray(new Class[set.size()]);
    }
}
