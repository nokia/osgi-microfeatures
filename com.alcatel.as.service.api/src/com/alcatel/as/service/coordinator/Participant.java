// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.coordinator;

/**
 * A Coordination participant.
 */
public interface Participant {
    /**
     * A Participant must provide this OSGi service property that matches a coordination name.
     */
    public String COORDINATION = "coordination";

    /**
     * A bundle that registers some Participant services must provide in its bundle's manifest this header, 
     * which must holds the number of Participants that the bundle will register.
     */
    public String PARTICIPANTS = "ASR-CoordinatorParticipants";

    /**
     * Synchronize this participant with a ongoing coordination.
     * @param coordination the coordination which is in progress
     * @param onComplete the callback to call when the participant is done with the coordination.
     */
    void join(Coordination coordination, Callback onComplete);
}
