/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// WARNING This class MUST not have references to the Category or
// WARNING RootCategory classes in its static initiliazation neither
// WARNING directly nor indirectly.

// Contributors:
//                Luke Blanshard <luke@quiq.com>
//                Mario Schomburg - IBM Global Services/Germany
//                Anders Kristensen
//                Igor Poteryaev

package org.apache.log4j;


import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.or.RendererMap;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.ThrowableRendererSupport;
import org.apache.log4j.spi.ThrowableRenderer;

/**
   This class is specialized in retrieving loggers by name and also
   maintaining the logger hierarchy.

   <p><em>The casual user does not have to deal with this class
   directly.</em>

   <p>The structure of the logger hierarchy is maintained by the
   {@link #getLogger} method. The hierarchy is such that children link
   to their parent but parents do not have any pointers to their
   children. Moreover, loggers can be instantiated in any order, in
   particular descendant before ancestor.

   <p>In case a descendant is created before a particular ancestor,
   then it creates a provision node for the ancestor and adds itself
   to the provision node. Other descendants of the same ancestor add
   themselves to the previously created provision node.

   @author Ceki G&uuml;lc&uuml;

*/
public class Hierarchy implements LoggerRepository, RendererSupport, ThrowableRendererSupport {

  /**
     Create a new logger hierarchy.

     @param root The root of the new hierarchy.

   */
  public
  Hierarchy(Logger root) {
  }

  /**
     Add an object renderer for a specific class.
   */
  public
  void addRenderer(Class classToRender, ObjectRenderer or) {
  }

  public
  void addHierarchyEventListener(HierarchyEventListener listener) {
  }

  /**
     This call will clear all logger definitions from the internal
     hashtable. Invoking this method will irrevocably mess up the
     logger hierarchy.

     <p>You should <em>really</em> know what you are doing before
     invoking this method.

     @since 0.9.0 */
  public
  void clear() {
  }

  public
  void emitNoAppenderWarning(Category cat) {
  }

  /**
     Check if the named logger exists in the hierarchy. If so return
     its reference, otherwise returns <code>null</code>.

     @param name The name of the logger to search for.

  */
  public
  Logger exists(String name) {
	  return null;
  }

  /**
     The string form of {@link #setThreshold(Level)}.
  */
  public
  void setThreshold(String levelStr) {
  }


  /**
     Enable logging for logging requests with level <code>l</code> or
     higher. By default all levels are enabled.

     @param l The minimum level for which logging requests are sent to
     their appenders.  */
  public
  void setThreshold(Level l) {
  }

  public
  void fireAddAppenderEvent(Category logger, Appender appender) {
  }

  void fireRemoveAppenderEvent(Category logger, Appender appender) {
  }

  /**
     Returns a {@link Level} representation of the <code>enable</code>
     state.

     @since 1.2 */
  public
  Level getThreshold() {
    return null;
  }

  /**
     Returns an integer representation of the this repository's
     threshold.

     @since 1.2 */
  //public
  //int getThresholdInt() {
  //  return thresholdInt;
  //}


  /**
     Return a new logger instance named as the first parameter using
     the default factory.

     <p>If a logger of that name already exists, then it will be
     returned.  Otherwise, a new logger will be instantiated and
     then linked with its existing ancestors as well as children.

     @param name The name of the logger to retrieve.

 */
  public
  Logger getLogger(String name) {
    return null;
  }

 /**
     Return a new logger instance named as the first parameter using
     <code>factory</code>.

     <p>If a logger of that name already exists, then it will be
     returned.  Otherwise, a new logger will be instantiated by the
     <code>factory</code> parameter and linked with its existing
     ancestors as well as children.

     @param name The name of the logger to retrieve.
     @param factory The factory that will make the new logger instance.

 */
  public
  Logger getLogger(String name, LoggerFactory factory) {
    return null;
  }

  /**
     Returns all the currently defined categories in this hierarchy as
     an {@link java.util.Enumeration Enumeration}.

     <p>The root logger is <em>not</em> included in the returned
     {@link Enumeration}.  */
  public
  Enumeration getCurrentLoggers() {
	  return null;
  }

  /**
     @deprecated Please use {@link #getCurrentLoggers} instead.
   */
  public
  Enumeration getCurrentCategories() {
    return getCurrentLoggers();
  }


  /**
     Get the renderer map for this hierarchy.
  */
  public
  RendererMap getRendererMap() {
    return null;
  }


  /**
     Get the root of this hierarchy.

     @since 0.9.0
   */
  public
  Logger getRootLogger() {
    return null;
  }

  /**
     This method will return <code>true</code> if this repository is
     disabled for <code>level</code> object passed as parameter and
     <code>false</code> otherwise. See also the {@link
     #setThreshold(Level) threshold} emthod.  */
  public
  boolean isDisabled(int level) {
    return false;
  }

  /**
     @deprecated Deprecated with no replacement.
  */
  public
  void overrideAsNeeded(String override) {
    LogLog.warn("The Hiearchy.overrideAsNeeded method has been deprecated.");
  }

  /**
     Reset all values contained in this hierarchy instance to their
     default.  This removes all appenders from all categories, sets
     the level of all non-root categories to <code>null</code>,
     sets their additivity flag to <code>true</code> and sets the level
     of the root logger to {@link Level#DEBUG DEBUG}.  Moreover,
     message disabling is set its default "off" value.

     <p>Existing categories are not removed. They are just reset.

     <p>This method should be used sparingly and with care as it will
     block all logging until it is completed.</p>

     @since 0.8.5 */
  public
  void resetConfiguration() {
  }

  /**
     Does nothing.

     @deprecated Deprecated with no replacement.
   */
  public
  void setDisableOverride(String override) {
    LogLog.warn("The Hiearchy.setDisableOverride method has been deprecated.");
  }



  /**
     Used by subclasses to add a renderer to the hierarchy passed as parameter.
   */
  public
  void setRenderer(Class renderedClass, ObjectRenderer renderer) {
  }

    /**
     * {@inheritDoc}
     */
  public void setThrowableRenderer(final ThrowableRenderer renderer) {
  }

    /**
     * {@inheritDoc}
     */
  public ThrowableRenderer getThrowableRenderer() {
	  return null;
  }


  /**
     Shutting down a hierarchy will <em>safely</em> close and remove
     all appenders in all categories including the root logger.

     <p>Some appenders such as {@link org.apache.log4j.net.SocketAppender}
     and {@link AsyncAppender} need to be closed before the
     application exists. Otherwise, pending logging events might be
     lost.

     <p>The <code>shutdown</code> method is careful to close nested
     appenders before closing regular appenders. This is allows
     configurations where a regular appender is attached to a logger
     and again to a nested appender.


     @since 1.0 */
  public
  void shutdown() {
  }

}


