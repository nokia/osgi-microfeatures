package com.alcatel.as.util.state;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Base class for all state machines. This class is not thread safe.
 * <p>
 * Sample code:
 * <p>
 * <hr>
 * <blockquote>
 * 
 * <pre>
 * 
 * import static java.lang.System.out;
 * import java.util.Arrays;
 * 
 * // -------------------- Main ---------------------------------------------------------------------------------------
 * 
 * public class Test {
 *   public static void main(String ... args) {
 *     Light light = new Light(); // We are in &quot;ON&quot; state by default
 *     light.handleEvent(Event.TURN_ON);
 *     light.handleEvent(Event.CHANGE_BULB, &quot;40w&quot;, &quot;60w&quot;, &quot;100w&quot;); // will be postponed until we enter into OFF state !
 *     light.handleEvent(Event.TURN_OFF);
 *     light.handleEvent(Event.TURN_OFF);
 *     light.handleEvent(Event.TURN_ON);
 *   }
 * }
 * 
 * // -------- Our stateful object which has two states: &quot;ON&quot; and &quot;OFF&quot;  (when OFF: all actions are postponed)
 * 
 * class Light extends StateMachine {
 *   Light() {
 *     super(ON.instance); // default state is &quot;ON&quot;
 *   }
 * 
 *   void log(String msg) {
 *     out.println(msg);
 *   }
 * }
 * 
 * // ------------------ our events ----------------------------------------------------------------------------------
 * 
 * enum Event {
 *   TURN_ON, TURN_OFF, CHANGE_BULB
 * }
 * 
 * // ------------------ our states --------------------------------------------------------------------------------
 * 
 * class ON extends State&lt;Light, Event&gt; {
 *   static ON instance = new ON();
 * 
 *   public String toString() { return &quot;ON&quot;; }    
 * 
 *   public void enter(Light light) throws TransitionException { 
 *     try {
 *       light.log(&quot;Turning ON&quot;);
 *     } catch (Throwable t) {
 *  throw new TransitionException(&quot;Could not enter into the ON state&quot;, t, OFF.instance);
 *     }
 *   }
 * 
 *   public void exit(Light light) throws TransitionException { 
 *   }
 * 
 *   public State handleEvent(Light ctx, Event event, Object ... args) {
 *     switch (event) {
 *     case TURN_ON:
 *       ctx.log(&quot;Already ON&quot;);
 *       return this;
 * 
 *     case TURN_OFF:
 *       return OFF.instance;
 * 
 *     case CHANGE_BULB:
 *       return POSTPONE_EVENT; // postponed ! we must not change a bulb when the ligth is turned on !!!!!!
 * 
 *     default:
 *       return this;
 *     }
 *   }
 * }
 * 
 * class OFF extends State&lt;Light, Event&gt; {
 *   static OFF instance = new OFF();
 * 
 *   public String toString() { return &quot;OFF&quot;; }
 * 
 *   public void enter(Light light) throws TransitionException { 
 *     System.out.println(&quot;Turning OFF&quot;);
 *   }
 * 
 *   public void exit(Light light) throws TransitionException { 
 *   }
 * 
 *   public State handleEvent(Light ctx, Event event, Object ... args) {
 *     switch (event) {
 *     case TURN_OFF:
 *       out.println(&quot;Already OFF&quot;);
 *       return this;
 * 
 *     case TURN_ON:
 *       return ON.instance;
 * 
 *     case CHANGE_BULB:
 *       out.println(&quot;Changing bulb: powers=&quot; + Arrays.toString(args));
 *       return this;
 *       
 *     default:
 *       return this;
 *     }
 *   }
 * }
 * 
 * </pre>
 * 
 * </blockquote>
 * <hr>
 */
@SuppressWarnings("unchecked")
public class StateMachine
{
    /**
     * Creates a new StateMachine instance.
     * @param initialState the state used as the initial one, when the state machine starts
     */
    public StateMachine(State initialState)
    {
        this(initialState, null, _defaultLogger);
    }

    /**
     * Creates a new StateMachine instance.
     * @param initialState the state used as the initial one, when the state machine starts
     * @param finalState the error state where we'll enter on any exceptions caught
     */
    public StateMachine(State initialState, State finalState)
    {
        this(initialState, finalState, _defaultLogger);
    }

    /**
     * Creates a new StateMachine instance.
     * @param initialState the state used as the initial one, when the state machine starts
     * @param logger the logger to be used by the state machine
     */
    public StateMachine(State initialState, Logger logger)
    {
        this(initialState, null, logger);
    }

    /**
     * Creates a new StateMachine instance.
     * @param initialState the state used as the initial one, when the state machine starts
     * @param finalState the error state where we'll enter on any exceptions caught
     * @param logger the logger to be used by the state machine
     */
    public StateMachine(State initialState, State finalState, Logger logger)
    {
        _initialState = initialState;
        _finalState = finalState;
        _logger = logger;
    }

    /**
     * Handle an event and forward it to the current active state 
     * (see {@link State#handleEvent(StateMachine, Enum, Object...)} 
     * method. If the {@link StateMachine#handleEvent(Enum, Object...)} method is invoked
     * from within the {@link State#handleEvent(StateMachine, Enum, Object...)} method, 
     * then the event will be scheduled and will be executed only once the 
     * {@link State#handleEvent(StateMachine, Enum, Object...)} method has completed.
     * 
     * @param event the event to handle
     * @param args some arguments passed to the current state
     */
    public void handleEvent(Enum event, Object... args)
    {
        if (_handlingEvent)
        {
            _postedEvents.add(new PostponedEvent(event, args));
            return;
        }
        _handlingEvent = true;
        try
        {
            /**
             * First handle the event.
             */
            _handleEvent(event, args);

            /**
             * Next, handle event which have been internally posted.
             */
            while (_postedEvents.size() > 0)
            {
                PostponedEvent postedEvent = _postedEvents.remove(0);
                _handleEvent(postedEvent._event, postedEvent._args);
            }
        }
        finally
        {
            _handlingEvent = false;
        }
    }

    /**
     * Sets an attribute shared among all states.
     * @param key the attribute name
     * @param val the attribute value
     * @return the previous attribute value
     */
    public <T> T setAttribute(Object key, Object val)
    {
        return (T) _attributes.put(key, val);
    }

    /**
     * Gets an attribute shared among all states.
     * @param key an attribute name
     * @return the attribute value
     */
    public <T> T getAttribute(Object key)
    {
        return (T) _attributes.get(key);
    }

    /**
     * Gets an attribute shared among all states.
     * @param key an attribute name
     * @return the attribute value
     */
    public int getIntAttribute(Object key)
    {
        return ((Integer) getAttribute(key)).intValue();
    }

    /**
     * Gets an attribute shared among all states.
     * @param key an attribute name
     * @return the attribute value
     */
    public String getStringAttribute(Object key)
    {
        return ((String) getAttribute(key));
    }

    /**
     * Gets an attribute shared among all states.
     * @param key an attribute name
     * @param def the default value returned if no attribute value is found
     * @return the attribute value
     */
    public int getIntAttribute(Object key, int def)
    {
        Integer I = (Integer) getAttribute(key);
        return I != null ? I.intValue() : def;
    }

    /**
     * Gets an attribute shared among all states.
     * @param key an attribute name
     * @return the attribute value
     */
    public boolean getBooleanAttribute(Object key)
    {
        return ((Boolean) getAttribute(key)).booleanValue();
    }

    /**
     * Gets an attribute shared among all states.
     * @param key an attribute name
     * @param def the default value returned if no attribute value is found
     * @return the attribute value
     */
    public boolean getBooleanAttribute(Object key, boolean def)
    {
        Boolean b = (Boolean) getAttribute(key);
        return (b != null) ? b.booleanValue() : def;
    }

    /**
     * Gets an attribute shared among all states.
     * @param key an attribute name
     * @return the attribute value
     */
    public long getLongAttribute(Object key)
    {
        return ((Long) getAttribute(key)).longValue();
    }

    /**
     * Removes an attribute shared among all states.
     * @param key an attribute name
     * @return the attribute value
     */
    public <T> T removeAttribute(Object key)
    {
        return (T) _attributes.remove(key);
    }

    /**
     * Returns all attributes keys.
     * @returns all attributes keys.
     */
    public Iterator getAttributeKeys()
    {
        return _attributes.keySet().iterator();
    }

    /**
     * Removes all attributes.
     */
    public void removeAttributes()
    {
        _attributes.clear();
    }

    @Override
    public String toString()
    {
        return super.toString() + "-currentState=" + (_currState == null ? "null" : _currState);
    }

    // --------------------------------- Protected methods

    protected void enterInitialState()
    {
        if (_currState == null)
        {
            doTransition(_initialState);
        }
    }

    protected State getCurrentState()
    {
        return _currState;
    }

    // --------------------------------- Private methods

    private void _handleEvent(Enum event, Object... args)
    {
        if (_terminated)
        {
            throw new IllegalStateException("state machine is in terminated state");
        }

        try
        {
            if (_currState == null)
            {
                doTransition(_initialState);
            }

            State nextState;
            if (_logger.isDebugEnabled())
            {
                _logger.debug("Handling event " + event + " on state " + _currState);
            }
            if ((nextState = _currState.handleEvent(this, event, args)) != _currState)
            {
                if (nextState == null)
                {
                    if (_logger.isDebugEnabled())
                    {
                        _logger.debug("Postponed event: " + event);
                    }
                    _postponedEvents.add(new PostponedEvent(event, args));
                }
                else
                {
                    doTransition(nextState);
                }
            }
        }

        catch (Throwable t)
        {
            // Got unexpected exception from a state machine: enter into the _finalState.
            if (_finalState != null)
            {
                try
                {
                    _logger.error("Got unexpected exception from state " + _currState
                            + ". Entering into final state " + _finalState, t);
                    _currState = _finalState;
                    _currState.enter(this);
                }
                catch (Throwable t2)
                {
                    _logger.error("Could not enter into the final state " + _currState, t2);
                }
            }
            else
            {
                _terminated = true;
                if (t instanceof RuntimeException)
                {
                    throw (RuntimeException) t;
                }
                else
                {
                    throw new RuntimeException("Got unexpected exception from state " + _currState, t);
                }
            }
        }
    }

    private void doTransition(State nextState)
    {
        // Leave current state.
        if (_currState != null)
        {
            try
            {
                if (_logger.isDebugEnabled())
                {
                    _logger.debug("Leaving state: " + _currState);
                }
                _currState.exit(this);
            }

            catch (TransitionException e)
            {
                if (e.getCause() != null)
                {
                    _logger.warn("Unexpected exception while exiting state: " + _currState, e);
                }
                else if (_logger.isDebugEnabled())
                {
                    _logger.debug("Existing state: " + _currState
                            + " , but the exit method requires to move to the following state: "
                            + e.getNextState());
                }
                nextState = e.getNextState();
            }
        }

        // Enter into next state.
        _currState = nextState;
        boolean processEvents = true;
        while (processEvents)
        {
            try
            {
                if (_logger.isDebugEnabled())
                {
                    _logger.debug("Entering into state: " + _currState);
                }
                _currState.enter(this);

                int n = _postponedEvents.size();
                if (n > 0 && _logger.isDebugEnabled())
                {
                    _logger.debug("Running " + n + " postponed events ...");
                }
                while (n > 0 && _postponedEvents.size() > 0)
                {
                    PostponedEvent postponed = _postponedEvents.remove(0);
                    _handleEvent(postponed._event, postponed._args);
                    n--;
                }
                processEvents = false; // we have process all postponed events
            }

            catch (TransitionException e)
            {
                if (e.getCause() != null)
                {
                    _logger.warn("Unexpected exception while entering state: " + _currState, e);
                }
                else if (_logger.isDebugEnabled())
                {
                    _logger.debug("Entering into state: " + _currState
                            + " , but the enter method requires to move to the state: " + e.getNextState());
                }
                _currState = e.getNextState();
            }
        }
    }

    // --------------------------------- Private attributes

    private static class PostponedEvent
    {
        PostponedEvent(Enum event, Object[] args)
        {
            _event = event;
            _args = args;
        }

        Enum _event;
        Object[] _args;
    }

    private boolean _handlingEvent = false;
    private boolean _terminated;
    private State _initialState;
    private State _finalState; // entered when an unexpected exception is caught by a state
    // handler.
    private State _currState;
    private final List<PostponedEvent> _postponedEvents = new LinkedList<PostponedEvent>();
    private final List<PostponedEvent> _postedEvents = new LinkedList<PostponedEvent>();
    private final HashMap _attributes = new HashMap();
    private final Logger _logger;
    private final static Logger _defaultLogger = Logger.getLogger("as.util.StateMachine");
}
