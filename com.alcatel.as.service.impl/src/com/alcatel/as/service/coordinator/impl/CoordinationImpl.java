package com.alcatel.as.service.coordinator.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.osgi.framework.BundleContext;

import com.alcatel.as.service.coordinator.Callback;
import com.alcatel.as.service.coordinator.Coordination;
import com.alcatel.as.service.coordinator.Participant;

import alcatel.tess.hometop.gateways.utils.Log;

/**
 * Coordination implementation.
 */
public class CoordinationImpl implements Coordination {
    private final List<Participant> _participants;
    private final String _name;
    private final Map<String, Object> _properties;
	private final BundleContext _bc;
    private final static Log _logger = Log.getLogger(CoordinationImpl.class);

    public CoordinationImpl(String name, List<Participant> participants, Map<String, Object> properties, BundleContext bc) {
        _participants = participants;
        _name = name;
        _properties = properties == null ? Collections.<String, Object>emptyMap() : properties;
        _bc = bc;
    }
        
    @Override
    public Map<String, Object> getProperties() {
        return _properties;
    }

    @Override 
    public String getName() {
        return _name;
    }
    
    int begin(final Callback onComplete, final Executor exec) {
        _logger.info("Begining coordination " + _name + " with participants: %s", _participants);
                        
        // Use a Phaser in order to detect when all participants are synchronized. 
        // When all participants are synchronized, then we'll call the onComplete callback.
        final Phaser phaser = new Phaser() {
            protected void done(final Throwable error) {
                Runnable task = new Runnable() {
                	public void run() {
                		try {
                			onComplete.joined(error);
                		} catch (Throwable t) {
                			_logger.info("onComplete callback threw an exception for coordination %s", t, _name);
                		}
                		registerCoordination();
                	}
                };
                if (exec != null) {
                    exec.execute(task);
                } else {
                	task.run();
                }
            }
        };
        
        phaser.register();        
        for (Participant participant : _participants) {
        	final Participant p = participant;
            phaser.register();
            participant.join(this, new Callback() {
                public void joined(Throwable error) {
                    _logger.info("Participant %s joined coordination %s (error=%s)", p, _name, error);
                    phaser.arrive(error);
                }
            });
        }

        phaser.arrive();
        return _participants.size();
    }
    
    private void registerCoordination() {
		_logger.info("Registering Coordination %s", _name);
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("name", _name); // TODO add a constant in API
		_bc.registerService(Coordination.class.getName(), CoordinationImpl.this, properties);
    }
}
