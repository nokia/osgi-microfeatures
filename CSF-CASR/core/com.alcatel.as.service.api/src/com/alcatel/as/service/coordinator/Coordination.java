package com.alcatel.as.service.coordinator;

import java.util.Map;

/**
 * A Coordination used to synchronize multiple parties.
 *
 * A Coordination may optionally provide some properties. When you begin a coordination, you have to supply
 * a onComplete callback object, and optionally an executor used to execute the onComplete callback once
 * all participants have completed.
 */
public interface Coordination {    
    /**
     * Gets coordination properties
     *
     * @return the properties for this coordination
     */
    Map<String, Object> getProperties();

    /**
     * Returns this coordination name.
     * @return this coordination name.
     */
    String getName();
}
