// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.coordinator;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A Coordinator service coordinates activities between different parties.
 * A bundle that wishes to participate to a coordination must register in the OSGi service registry
 * a Participant service with a Participant.COORDINATION service property.  
 * The bundle that registers the Participant must also provides a "ASR-CoordinatorParticipants" manifest
 * header that corresponds to the number of participants the bundle will register.
 */
public interface Coordinator {
	/**
	 * Creates a new Coordination object, which can be started using the {@link #begin()} method.
	 * @param coordination the coordination name. All Participants with a "coordination" service property matching this name
	 * will be bound to that coordination
	 * @param an optional map of properties (can be null).
	 * @return the new coordination
	 */
	Coordination newCoordination(String coordination, Map<String, Object> properties);
    
    /**
     * Begins the coordination. All Participants having the Participant.COORDINATION service property matching the that matches this coordination name will be invoked in their synchronize method.
     *
     * @param onComplete The callback to invoke once all participants have called their onComplete parameter.
     * @param exec an optional executor used to schedule the callback, or null.
     * @return the number of coordination participants.
     */
    int begin(Coordination coordination, Callback onComplete, Executor exec);
}
