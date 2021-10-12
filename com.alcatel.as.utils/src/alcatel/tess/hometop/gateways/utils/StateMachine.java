// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.log4j.Logger;

/**
 * Base class for all state machines.
 * Mainly, this class offers the following features:
 * <br> Each state can transition to another state, using the changeState method.
 * <br> All state transition are logged.
 * <br> an ERROR state is automatically entered when the state machine gets a 
 * runtime exception
 * <br> A state can postpone a method invocation to the next state transition.
 */
public class StateMachine<STATE extends State> implements InvocationHandler {
  /**
   * Creates a new State Machine. Once created, a state machine can enter into a state,
   * using the changeState method.
   *
   * @param errorState: the state which is entered, when an unexpected runtime exception si
   *			caught.
   * @oaram logger The logger used to log all state transitions.
   */
  public StateMachine(STATE errorState, Logger logger) {
    _logger = logger;
    _errorState = errorState;
    _proxy = (State) Proxy.newProxyInstance(errorState.getClass().getClassLoader(), errorState.getClass()
        .getInterfaces(), this);
  }
  
  /************************************* InvocationHandler interface *******************************************/
  
  /**
   * Dispatch the method to the current state.
   */
  public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
    if (_logger.isDebugEnabled()) {
      logInvocation(method, args, "Invoking");
    }
    
    try {
      _invokePostponed = false;
      Object o = method.invoke(_current, args);
      if (_invokePostponed) {
        postponeInvoke(method, args);
      }
      return o;
    }
    
    catch (Throwable t) {
      // Log the invoked method exception.
      if (t instanceof InvocationTargetException) {
        t = ((InvocationTargetException) t).getTargetException();
      }
      _logger.error(this + ": Got unexpected exception while calling method " + method.getName() + " on "
          + getName(_current), t);
      
      // Leave the current state
      try {
        if (_logger.isDebugEnabled())
          _logger.debug(this + ": Exit " + getName(_current));
        _current.exit(this);
      } catch (Throwable t2) {
        _logger.error(this + ": Got unexpected exception while leaving state " + getName(_current), t2);
        return null;
      }
      
      // Enter into the error state
      try {
        _current = _errorState;
        if (_logger.isDebugEnabled())
          _logger.debug(this + ": Enter " + getName(_current));
        _current.enter(this);
      }
      
      catch (Throwable t2) {
        _logger.error(this + ": Got unexpected exception while entering into " + getName(_current), t2);
        return null;
      }
      
      return null;
    }
    
    finally {
      _invokePostponed = false;
    }
  }
  
  /************************************* Public methods *******************************************/
  
  /**
   * Returns the current active state.
   * @returns the current active state.
   */
  public STATE getState() {
    return ((STATE) _proxy);
  }
  
  /**
   * Transitions to another state. This method do the following: 
   * <br> The exit's current state method is called,
   * <br> The enter's next state is called,
   *
   * The enter/exit method can call the changeState method, if required.
   * Any exception will activate the ERROR state.
   */
  public void changeState(State next) {
    if (_changingState) {
      _next = next;
      return;
    }
    _changingState = true;
    
    try {
      while (true) {
        // STEP 1: leave the current state
        exitCurrentState();
        if (_next != null) {
          // The exit's current state method has called changedState
          next = _next;
        }
        
        // STEP 2: enter into next state.
        enter(next);
        
        // STEP 3: goto STEP 1 if the next.enter() called changeState()
        if (_next != null) {
          next = _next;
          continue;
        }
        
        // STEP 4: if the next state has been entered: run postponed methods
        runPostponedInvokes();
        
        // STEP 5: if any postponed methods called changeState(), goto STEP 1
        if (_next != null) {
          next = _next;
          continue;
        }
        
        break; // all done
      }
    }
    
    finally {
      _changingState = false;
    }
  }
  
  /**
   * This method postpone a method to the next time we'll transition to another state.
   * That is: if a method state is invoked, but is not currently able to do anything, it
   * can call this method in order to postpone the method invocation until we enter
   * into another state. Once another state is entered, the postponed method will be invoked
   * on the newly transitioned state.
   */
  public void postponeEvent() {
    _invokePostponed = true;
  }
  
  /**
   * Sets an attribute shared among all states.
   */
  public Object setAttribute(Object key, Object val) {
    if (_attributes == null) {
      _attributes = new Hashtable();
    }
    return _attributes.put(key, val);
  }
  
  /**
   * Gets an attribute shared among all states.
   */
  public Object getAttribute(Object key) {
    if (_attributes == null) {
      return null;
    }
    return _attributes.get(key);
  }
  
  /**
   * Gets an attribute shared among all states.
   */
  public int getIntAttribute(Object key) {
    return ((Integer) getAttribute(key)).intValue();
  }
  
  /**
   * Gets an attribute shared among all states.
   */
  public String getStringAttribute(Object key) {
    return ((String) getAttribute(key));
  }
  
  /**
   * Gets an attribute shared among all states.
   */
  public int getIntAttribute(Object key, int def) {
    Integer I = (Integer) getAttribute(key);
    return I != null ? I.intValue() : def;
  }
  
  /**
   * Gets an attribute shared among all states.
   */
  public boolean getBooleanAttribute(Object key) {
    return ((Boolean) getAttribute(key)).booleanValue();
  }
  
  /**
   * Gets an attribute shared among all states.
   */
  public boolean getBooleanAttribute(Object key, boolean def) {
    Boolean b = (Boolean) getAttribute(key);
    return (b != null) ? b.booleanValue() : def;
  }
  
  /**
   * Gets an attribute shared among all states.
   */
  public long getLongAttribute(Object key) {
    return ((Long) getAttribute(key)).longValue();
  }
  
  /**
   * Removes an attribute shared among all states.
   */
  public Object removeAttribute(Object key) {
    if (_attributes == null) {
      return null;
    }
    return _attributes.remove(key);
  }
  
  /**
   * Returns all attributes keys.
   */
  public Enumeration getAttributeKeys() {
    if (_attributes == null) {
      return null;
    }
    return _attributes.keys();
  }
  
  /**
   * Removes all attributes.
   */
  public void removeAttributes() {
    if (_attributes != null) {
      _attributes.clear();
    }
  }
  
  /************************************* Private methods *******************************************/
  
  private void exitCurrentState() {
    if (_current != null) {
      if (_logger.isDebugEnabled())
        _logger.debug(this + ": Exit " + getName(_current));
      try {
        _next = null;
        _current.exit(this);
      } catch (Throwable t) {
        // Could not leave the current state: enter into the error state.
        _logger.error(this + ": Got unexpected exception while leaving state " + getName(_current), t);
        _next = _errorState;
      }
    }
  }
  
  private void enter(State state) {
    try {
      _current = state;
      if (_logger.isDebugEnabled())
        _logger.debug(this + ": Enter " + getName(_current));
      _next = null;
      _current.enter(this);
    } catch (Throwable t) {
      // Could not enter into the next state: transition to the Error state.
      _logger.error(this + ": Got unexpected exception while entering into " + getName(_current), t);
      _next = _errorState;
    }
  }
  
  private void runPostponedInvokes() {
    int count = 0;
    if (_postponedInvokes != null && (count = _postponedInvokes.size()) > 0) {
      if (_logger.isDebugEnabled()) {
        _logger.debug(this + ": Invoking postponed methods (count=" + count + ")");
      }
      
      for (int i = 0; i < count; i++) {
        PostponedInvoke pi = _postponedInvokes.remove(0);
        
        try {
          _invokePostponed = false;
          if (_logger.isDebugEnabled()) {
            logInvocation(pi.method, pi.args, "Invoking (postponed)");
          }
          pi.method.invoke(_current, pi.args);
          if (_invokePostponed) {
            postponeInvoke(pi.method, pi.args);
          }
        }
        
        catch (Throwable t) {
          _logger.error(this + ": Got unexpected exception while invoking postponed method", t);
          _next = _errorState;
        }
        
        finally {
          _invokePostponed = false;
        }
        
        if (_next != null) {
          break; // The current postponed method called this.changeState ...
        }
      }
    }
  }
  
  private void logInvocation(Method method, Object[] args, String msg) {
    StringBuffer sb = new StringBuffer();
    sb.append(msg).append(" ").append(getName(_current));
    sb.append(".").append(method.getName()).append("(");
    
    for (int i = 0; args != null && i < args.length; i++) {
      // Truncate method parameters if they go beyond one single line.
      
      if (args[i] != null) {
        String arg = args[i].toString();
        boolean truncated = false;
        for (int j = 0; j < arg.length(); j++) {
          if (arg.charAt(j) == '\n') {
            if (j > 0 && arg.charAt(j - 1) == '\r') {
              j--;
            }
            truncated = true;
            sb.append(arg, 0, j).append(" ...");
            break;
          }
        }
        if (!truncated) {
          sb.append(arg);
        }
      }
      
      if (i < args.length - 1) {
        sb.append(", ");
      }
    }
    sb.append(")");
    
    _logger.debug(sb.toString());
  }
  
  private void postponeInvoke(Method m, Object[] args) {
    if (_postponedInvokes == null) {
      _postponedInvokes = new ArrayList<PostponedInvoke>();
    }
    _postponedInvokes.add(new PostponedInvoke(m, args));
    logInvocation(m, args, "Postponing");
  }
  
  private String getName(State s) {
    String name = s.getClass().getSimpleName();
    if (name.length() == 0) {
      name = s.toString();
    }
    return name;
  }
  
  private static class PostponedInvoke {
    private Method method;
    private Object[] args;
    
    PostponedInvoke(Method m, Object[] args) {
      this.method = m;
      this.args = args;
    }
  }
  
  /************************************* Private attributes *******************************************/
  
  private ArrayList<PostponedInvoke> _postponedInvokes;
  private boolean _invokePostponed;
  private Logger _logger;
  private State _proxy;
  private State _current;
  private State _next;
  private Hashtable _attributes;
  private boolean _changingState;
  private State _errorState;
}
