// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.cl;

// PlatformServicesAPI
import com.alcatel.as.service.concurrent.PlatformExecutor;

/**
 * Utility class related to class loader management.
 */
public class ClassLoaderHelper
{

    /**
     * Sets the current thread context class loader.
     * 
     * @param cl the class loader to be set in the current thread context.
     * @return the old class loader that was set in the current thread.
     */
    public static ClassLoader setContextClassLoader(ClassLoader cl)
    {
        Thread currThread = Thread.currentThread();
        ClassLoader currCL = currThread.getContextClassLoader();
        currThread.setContextClassLoader(cl);
        return currCL;
    }

    /**
     * Executes a runnable with a given thread context class loader.
     * 
     * @param r a Runnable task to be executed in the context of the given class loader.
     * @param cl the class loader to be set in the current thread, while executing the runnable
     *          task.
     * @throws Exception any exceptions raised by the runnable task.
     */
    public static void executeWithClassLoader(Runnable r, ClassLoader cl) throws Exception
    {
        ClassLoader oldCL = setContextClassLoader(cl);
        try
        {
            r.run();
        }
        finally
        {
            setContextClassLoader(oldCL);
        }
    }

    /**
     * Execute a runnable with a given thread context class loader, inside a given Executor.
     * 
     * @param r a Runnable task to be executed in the context of the given class loader.
     * @param cl the class loader to be set in the current thread, while executing the runnable
     *          task.
     * @param e the executor used to run the given task.
     * @throws Exception any exceptions raised by the runnable task.
     */
    public static void executeWithClassLoader(Runnable r, ClassLoader cl, PlatformExecutor e)
        throws Exception
    {
        ClassLoader oldCL = setContextClassLoader(cl);
        try
        {
            e.execute(r); // Will use the current thread context class loader
        }
        finally
        {
            setContextClassLoader(oldCL);
        }
    }
}
