// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.amdatu.scheduling.quartz.scheduler;

import static org.osgi.framework.Constants.SERVICE_ID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * JobFactory implementation to call {@link org.quartz.Job} or {@link org.amdatu.scheduling.Job} services 
 * that are registered in the OSGi Service registry.
 */
public class OSGiJobFactory implements JobFactory {
    
    private final Map<Long, Object> m_jobs = new ConcurrentHashMap<>();
    
    /**
     * Called by dependency manager when a new {@link org.quartz.Job} or {@link org.amdatu.scheduling.Job}
     * is registered. 
     * 
     * @param ref {@link ServiceReference} of the added service
     * @param job the service can be a {@link org.quartz.Job} or {@link org.amdatu.scheduling.Job}
     */
    public void jobAdded(ServiceReference<?> ref, Object job) {
        m_jobs.put((Long) ref.getProperty(SERVICE_ID), job);
    }
    
    /**
     * Called by dependency manager when a job is unregistered.
     * 
     * @param ref {@link ServiceReference} of the removed service
     */
    public void jobRemoved(ServiceReference<?> ref) {
        m_jobs.remove((Long) ref.getProperty(SERVICE_ID));
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        final JobDetail jobDetail = bundle.getJobDetail();
        
        Long serviceId = jobDetail.getJobDataMap().getLongValue(SERVICE_ID);
        Object job = m_jobs.get(serviceId);
        if (job == null) {
            throw new IllegalStateException("Service with id " + serviceId + " not found");
        }
        
        Class<? extends Object> jobClass = job.getClass();
        if (Job.class.isAssignableFrom(jobClass)) {
            return (Job) job;
        } else if (org.amdatu.scheduling.Job.class.isAssignableFrom(jobClass)) {
            return jobExecutionContext -> ((org.amdatu.scheduling.Job)job).execute();
        } else {
            throw new IllegalStateException("Can't create Job for type " + jobClass);
        }
    }

}
