package com.alcatel.as.diameter.lb;

import org.apache.log4j.Logger;

public class AlertManager {
    public enum Action
    {
        NONE,
	    SEND_ALERT,
	    CLEAR_ALERT
	    }
    
    private int _highThreshold, _lowThreshold;
    private int _sendDuration;
    private int _clearDuration;
    private State _state;
    private long _sendPeriodStart;
    private long _clearPeriodStart;
    
    /**
     * Constructor.
     * 
     * @param sendDuration corresponds to our "alarm-send" duration (in millis).
     * @param clearDuration corresponds to our "alarm-clear" duration
     */
    public AlertManager(int lowThreshold,
			int highThreshold,
			int sendDuration,
			int clearDuration)
    {
	_lowThreshold = lowThreshold;
	_highThreshold = highThreshold;
        _sendDuration = sendDuration;
        _clearDuration = clearDuration;
        _state = NORMAL;
    }
    
    public Action destroy (){
	return _state.destroy (this);
    }

    public Action process (int value){
	if (value <=_lowThreshold) return _state.belowLowThreshold (this);
	if (value <=_highThreshold) return _state.betweenThresholds (this);
	return _state.aboveHighThreshold (this);
    }

    public Action aboveHighThreshold (){
	return _state.aboveHighThreshold (this);
    }
    public Action betweenThresholds (){
	return _state.betweenThresholds (this);
    }
    public Action belowLowThreshold (){
	return _state.belowLowThreshold (this);
    }

    protected static class State{
	protected Action aboveHighThreshold(AlertManager manager){return Action.NONE;}
	protected Action betweenThresholds (AlertManager manager){return Action.NONE;}
	protected Action belowLowThreshold(AlertManager manager){return Action.NONE;}
	protected Action destroy(AlertManager manager){return Action.NONE;}
    }

    private static final State NORMAL = new State(){
	    protected Action aboveHighThreshold (AlertManager manager){
		manager._sendPeriodStart = System.currentTimeMillis ();
		manager._state = NORMAL_HIGH;
		return Action.NONE;
	    }
	};
    private final static State NORMAL_HIGH = new State (){
	    protected Action aboveHighThreshold (AlertManager manager){
		if (System.currentTimeMillis () - manager._sendPeriodStart >= manager._sendDuration){
		    manager._state = ALERT;
		    return Action.SEND_ALERT;
		} else {
		    return Action.NONE;
		}
	    }
	    protected Action betweenThresholds (AlertManager manager){
		manager._state = NORMAL;
		return Action.NONE;
	    }
	    protected Action belowLowThreshold(AlertManager manager){
		manager._state = NORMAL;
		return Action.NONE;
	    }
	};
    private static final State ALERT = new State(){
	    protected Action belowLowThreshold (AlertManager manager){
		manager._clearPeriodStart = System.currentTimeMillis();
		manager._state = ALERT_LOW;
		return Action.NONE;
	    }
	    @Override
	    protected Action destroy (AlertManager manager){
		return Action.CLEAR_ALERT;
	    }
	};
    private static final State ALERT_LOW = new State(){
	    protected Action aboveHighThreshold (AlertManager manager){
		manager._state = ALERT;
		return Action.NONE;
	    }
	    protected Action betweenThresholds (AlertManager manager){
		manager._state = ALERT;
		return Action.NONE;
	    }
	    protected Action belowLowThreshold (AlertManager manager){
		if (System.currentTimeMillis () - manager._clearPeriodStart >= manager._clearDuration){
		    manager._state = NORMAL;
		    return Action.CLEAR_ALERT;
		}
		return Action.NONE;
	    }
	    @Override
	    protected Action destroy (AlertManager manager){
		return Action.CLEAR_ALERT;
	    }
	};
}
