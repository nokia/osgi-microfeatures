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

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.amdatu.scheduling.annotations.Cron;
import org.amdatu.scheduling.annotations.Description;
import org.amdatu.scheduling.annotations.ModifiedByCalendar;
import org.amdatu.scheduling.annotations.Priority;
import org.amdatu.scheduling.annotations.RepeatCount;
import org.amdatu.scheduling.annotations.RepeatForever;
import org.amdatu.scheduling.annotations.RepeatInterval;
import org.amdatu.scheduling.annotations.RequestRecovery;
import org.amdatu.scheduling.annotations.timeinterval.DaysOfTheWeek;
import org.amdatu.scheduling.annotations.timeinterval.EndingDailyAt;
import org.amdatu.scheduling.annotations.timeinterval.EveryDay;
import org.amdatu.scheduling.annotations.timeinterval.Interval;
import org.amdatu.scheduling.annotations.timeinterval.MondayThroughFriday;
import org.amdatu.scheduling.annotations.timeinterval.SaturdayAndSunday;
import org.amdatu.scheduling.annotations.timeinterval.StartingDailyAt;
import org.amdatu.scheduling.constants.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.quartz.CronScheduleBuilder;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

public class WhiteboardJobServiceImpl {
	
	private enum  RepeatIntervalPeriod { 
		MILLISECOND, SECOND, MINUTE, HOUR, DAY
	}
	
	private enum IntervalPeriod {
		SECOND, MINUTE, HOUR
	}
    
    private volatile Scheduler m_scheduler;
    
    private volatile LogService m_logService;
    
    private final Map<ServiceReference<?>, JobDetail> m_jobs = new HashMap<>();
    
    public void jobAdded(ServiceReference<?> ref, Object job) {
        final Class<?> jobClass = job.getClass();
        
        final JobBuilder jobBuilder = JobBuilder.newJob();
        final TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
        
        if (buildJobAndTrigger(jobBuilder, triggerBuilder, jobClass, ref)) {
            final String group = "bundle" + ref.getBundle().getBundleId();
            final String name = jobClass.getName();
            jobBuilder.withIdentity(name, group);
            
            // Add the service id to the job data map to be able to lookup the actual service 
            // in the OSGiJobFactory when this job is triggered. 
            jobBuilder.usingJobData(SERVICE_ID, (Long)ref.getProperty("service.id"));
            
            final JobDetail jobDetail = jobBuilder.build();
            final Trigger trigger = triggerBuilder.build();
            try {
                synchronized(m_jobs) {
                    m_scheduler.scheduleJob(jobDetail, trigger);
                    m_jobs.put(ref, jobDetail);
                }
            } catch (SchedulerException e) {
                m_logService.log(LogService.LOG_WARNING, "Failed to schedule job", e);
            }
        }
    }
    
    public void jobChanged(ServiceReference<Job> ref, Object job) {
        // AMDATUSCH-6: Re-add the job when the service properties have changed
        jobRemoved(ref);
        jobAdded(ref, job);
    } 
    
    public void jobRemoved(ServiceReference<?> ref) {
        synchronized(m_jobs) {
            final JobDetail jobDetail = m_jobs.get(ref);
            if (jobDetail != null) { // Can be null here the Job is only added in case there is a schedule 
                try {
                    m_scheduler.deleteJob(jobDetail.getKey());
                    m_jobs.remove(ref);
                } catch (SchedulerException e) {
                    m_logService.log(LogService.LOG_WARNING, "Failed to delete job", e);
                }
            }
        }
    }
    
    public void stop() {
        synchronized(m_jobs) {
            for (JobDetail job : m_jobs.values()) {
                try {
                    m_scheduler.deleteJob(job.getKey());
                } catch (SchedulerException e) {
                    m_logService.log(LogService.LOG_WARNING, "Failed to delete job", e);
                }
            }
        }
    }
    
    private boolean buildJobAndTrigger(JobBuilder jobBuilder, TriggerBuilder<Trigger> triggerBuilder, AnnotatedElement element, ServiceReference<?> sr) {
    	
    	final String description = getDescription(element, sr);
    	if(description != null) {
    		jobBuilder.withDescription(description);
    	}
    	
    	final Boolean requestRecovery = getRequestRecovery(element, sr);
    	if(requestRecovery != null) {
    		jobBuilder.requestRecovery(requestRecovery.booleanValue());
    	}
    	
    	final String modifiedByCalendar = getModifiedByCalendar(element, sr);
    	if(modifiedByCalendar != null) {
    		triggerBuilder.modifiedByCalendar(modifiedByCalendar);
    	}        
        
    	final Integer priority = getPriority(element, sr);
    	if(priority != null) {
    		triggerBuilder.withPriority(priority.intValue());
    	}
    	
        final ScheduleBuilder<? extends Trigger> schedule = getScheduleFor(element, sr);
        if (schedule != null) {
            triggerBuilder.withSchedule(schedule);
            return true;
        } else {
            return false;
        }
    }

    private ScheduleBuilder<? extends Trigger> getScheduleFor(AnnotatedElement element, ServiceReference<?> sr) {
    	final String cron = getCron(element, sr);
    	final RepeatIntervalInner repeatInterval = getRepeatInterval(element, sr);
    	final IntervalInner interval = getInterval(element, sr);
    	
    	if(cron != null && repeatInterval == null && interval == null) {
    		return CronScheduleBuilder.cronSchedule(cron);
    	} else if(repeatInterval != null && interval == null && cron == null) {
    		return getSimpleScheduleFor(element, sr, repeatInterval);
    	} else if(interval != null && cron != null && repeatInterval != null) {
    		return getDailyTimeIntervalScheduleFor(element, sr, interval);
    	}
    	return null;    	
    }
    
    private ScheduleBuilder<? extends Trigger> getSimpleScheduleFor(AnnotatedElement element, 
    		ServiceReference<?> sr, RepeatIntervalInner repeatInterval) {
    	
    	final SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule();
    	schedule.withIntervalInMilliseconds(repeatInterval.getValueInMilliseconds().longValue());
    	
    	final Integer repeatCount = getRepeatCount(element, sr);
    	if(repeatCount != null) {
    		schedule.withRepeatCount(repeatCount.intValue());
    	}
    	final Boolean repeatForever = getRepeatForever(element, sr);
    	if((repeatForever != null) && repeatForever.booleanValue()) {
    		schedule.repeatForever();
    	}
    	return schedule;
    }
    
    
    private ScheduleBuilder<? extends Trigger> getDailyTimeIntervalScheduleFor(AnnotatedElement element, 
    		ServiceReference<?> sr, IntervalInner interval) {
    	
    	final DailyTimeIntervalScheduleBuilder schedule = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule();
    	schedule.withIntervalInSeconds(interval.getValueInSeconds());
    	
    	Set<Integer> daysOfTheWeek = getDaysOfTheWeek(element, sr);
    	if(daysOfTheWeek != null) {
    		schedule.onDaysOfTheWeek(daysOfTheWeek);
    	}
    	
    	Boolean everyDay = getEveryDay(element, sr);
    	if(everyDay != null && everyDay.booleanValue()) {
    		schedule.onEveryDay();
    	}
    	
    	Boolean mondayThroughFriday = getMondayThroughFriday(element, sr);
    	if(mondayThroughFriday != null && mondayThroughFriday.booleanValue()) {
    		schedule.onMondayThroughFriday();
    	}
    	
    	Boolean saturdayAndSunday = getSaturdayAndSunday(element, sr);
    	if(saturdayAndSunday != null && saturdayAndSunday.booleanValue()) {
    		schedule.onSaturdayAndSunday();
    	}
    	
    	DailyAt startingDailyAt = getStartingDailyAt(element, sr);
    	if(startingDailyAt != null) {
    		schedule.startingDailyAt(new TimeOfDay(startingDailyAt.getHour(), startingDailyAt.getMinute(), startingDailyAt.getSecond()));
    	}
    	
    	DailyAt endingDailyAt = getEndingDailyAt(element, sr);
    	if(endingDailyAt != null) {
    		schedule.endingDailyAt(new TimeOfDay(endingDailyAt.getHour(), endingDailyAt.getMinute(), endingDailyAt.getSecond()));
    	}
    	
    	return schedule;
    }
    
    private String getDescription(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<String> property = getStringProperty(sr, Constants.DESCRIPTION);
    	if(property.isPresent()) {
    		return property.get();
    	}
	    final Description annotation = element.getAnnotation(Description.class);
	    if (annotation != null) {
	    	return annotation.value();
	    }
    	return null;
    }
    
    private Boolean getRequestRecovery(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Boolean> property = getBooleanProperty(sr, Constants.REQUEST_RECOVERY);
    	if(property.isPresent()) {
    		return property.get();
    	}
    	final RequestRecovery annotation = element.getAnnotation(RequestRecovery.class);
    	if (annotation != null) {
    		return annotation.value();
    	}
    	return null;
    }
    
    private String getModifiedByCalendar(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<String> property = getStringProperty(sr, Constants.MODIFIED_BY_CALENDAR);
    	if(property.isPresent()) {
    		return property.get();
    	}
    	final ModifiedByCalendar annotation = element.getAnnotation(ModifiedByCalendar.class);
    	if (annotation != null) {
    		return annotation.value();
    	}
    	return null;
    }
    
    private Integer getPriority(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Integer> property = getIntegerProperty(sr, Constants.PRIORITY);
    	if(property.isPresent()) {
    		return property.get();
    	}
    	final Priority annotation = element.getAnnotation(Priority.class);
        if (annotation != null) {
        	return annotation.value();
        }
    	return null;
    }

    private String getCron(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<String> property = getStringProperty(sr, Constants.CRON);
    	if(property.isPresent()) {
    		return property.get();
    	}
    	final Cron cron = element.getAnnotation(Cron.class);
    	if(cron != null) {
    		return cron.value();
    	}
    	return null;
    }
    
    private RepeatIntervalInner getRepeatInterval(AnnotatedElement element, ServiceReference<?> sr) {
    	
    	Optional<String> periodProperty = getStringProperty(sr, Constants.REPEAT_INTERVAL_PERIOD);
    	Optional<Long> valueProperty = getLongProperty(sr, Constants.REPEAT_INTERVAL_VALUE);
    	
    	if(periodProperty.isPresent()) {
    		RepeatIntervalPeriod period = getRepeatIntervalPeriod(periodProperty.get());
    		if(valueProperty.isPresent()) {
    			return new RepeatIntervalInner(period, valueProperty.get());
    		} else {
    			// fall back to annotations
    			m_logService.log(LogService.LOG_WARNING, Constants.REPEAT_INTERVAL_PERIOD + " property present, " + Constants.REPEAT_INTERVAL_VALUE + " not present");
    		}
    	} else {
    		if(valueProperty.isPresent()) {
    			return new RepeatIntervalInner(RepeatIntervalPeriod.SECOND, valueProperty.get());
    		}
    	}
    	
    	final RepeatInterval annotation = element.getAnnotation(RepeatInterval.class);
    	if(annotation != null) {
    		return new RepeatIntervalInner(getRepeatIntervalPeriod(annotation.period()), annotation.value());
    	}
    	
    	return null;
    }
    
    private RepeatIntervalPeriod getRepeatIntervalPeriod(long period) {    	
    	if(period == RepeatInterval.MILLISECOND)
    		return RepeatIntervalPeriod.MILLISECOND;
    	if(period == RepeatInterval.SECOND) 
    		return RepeatIntervalPeriod.SECOND;
    	if(period == RepeatInterval.MINUTE)
    		return RepeatIntervalPeriod.MINUTE;
    	if(period == RepeatInterval.HOUR)
    		return RepeatIntervalPeriod.HOUR;
    	if(period == RepeatInterval.DAY)
    		return RepeatIntervalPeriod.DAY;
    	return RepeatIntervalPeriod.SECOND;
    }
    
    private RepeatIntervalPeriod getRepeatIntervalPeriod(String period) {
    	return RepeatIntervalPeriod.valueOf(period.toUpperCase()); 
    }
    
    private IntervalInner getInterval(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<String> periodProperty = getStringProperty(sr, Constants.INTERVAL_PERIOD);
    	Optional<Integer> valueProperty = getIntegerProperty(sr, Constants.INTERVAL_VALUE);
    	
    	if(periodProperty.isPresent()) {
    		IntervalPeriod period = getIntervalPeriod(periodProperty.get());
    		if(valueProperty.isPresent()) {
    			return new IntervalInner(period, valueProperty.get());
    		} else {
    			// fall back to annotations
    			m_logService.log(LogService.LOG_WARNING, Constants.INTERVAL_PERIOD + " property present, " + Constants.INTERVAL_VALUE + " not present");
    		}
    	} else {
    		if(valueProperty.isPresent()) {
    			return new IntervalInner(IntervalPeriod.SECOND, valueProperty.get());
    		}
    	}

    	final Interval annotation = element.getAnnotation(Interval.class);
    	if(annotation != null) {
    		return new IntervalInner(getIntervalPeriod(annotation.period()), annotation.value());
    	}
    	
    	return null;
    }
    
    private IntervalPeriod getIntervalPeriod(long period) {
    	if(period == Interval.SECOND)
    		return IntervalPeriod.SECOND;
    	if(period == Interval.MINUTE)
    		return IntervalPeriod.MINUTE;
    	if(period == Interval.HOUR)
    		return IntervalPeriod.HOUR;
    	return IntervalPeriod.SECOND;
    }
    
    private IntervalPeriod getIntervalPeriod(String period) {
    	return IntervalPeriod.valueOf(period.toUpperCase());
    }

    private Integer getRepeatCount(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Integer> property = getIntegerProperty(sr, Constants.REPEAT_COUNT);
    	if(property.isPresent()) {
    		return property.get();
    	}
		final RepeatCount annotation = element.getAnnotation(RepeatCount.class);
		if (annotation != null) {
			return annotation.value();
		}
    	return null;    	
    }
    
    private Boolean getRepeatForever(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Boolean> property = getBooleanProperty(sr, Constants.REPEAT_FOREVER);
    	if(property.isPresent()) {
    		return property.get();
    	}
    	final RepeatForever annotation = element.getAnnotation(RepeatForever.class);
    	if(annotation != null) {
    		return true;
    	}
    	return null;
    }
    
    private Set<Integer> getDaysOfTheWeek(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<List<Integer>> property = getIntegerListProperty(sr, Constants.DAYS_OF_THE_WEEK);
    	if(property.isPresent()) {
    		return new HashSet<>(property.get());
    	}
    	final DaysOfTheWeek annotation = element.getAnnotation(DaysOfTheWeek.class);
    	if(annotation != null) {
    		final Set<Integer> days = new HashSet<>();
    		for(int day : annotation.value()) {
    			days.add(day);
    		}
    		return days;
    	}
    	return null;
    }

    private Boolean getEveryDay(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Boolean> property = getBooleanProperty(sr, Constants.EVERY_DAY);
    	if(property.isPresent()) {
    		return property.get();
    	}    	
    	final EveryDay annotation = element.getAnnotation(EveryDay.class);
    	if(annotation != null) {
    		return true;
    	}
    	return null;
    }
    
    private Boolean getMondayThroughFriday(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Boolean> property = getBooleanProperty(sr, Constants.MONDAY_THROUGH_FRIDAY);
    	if(property.isPresent()) {
    		return property.get();
    	}
    	final MondayThroughFriday annotation = element.getAnnotation(MondayThroughFriday.class);
    	if(annotation != null) {
    		return true;
    	}
    	return null;
    }
    
    private Boolean getSaturdayAndSunday(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Boolean> property = getBooleanProperty(sr, Constants.SATURDAY_AND_SUNDAY);
    	if(property.isPresent()) {
    		return property.get();
    	}
    	final SaturdayAndSunday annotation = element.getAnnotation(SaturdayAndSunday.class);
    	if(annotation != null) {
    		return true;
    	}
    	return null;
    }

    private DailyAt getStartingDailyAt(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Integer> hourProperty = getIntegerProperty(sr, Constants.STARTING_DAILY_AT_HOUR);
    	Optional<Integer> minuteProperty = getIntegerProperty(sr, Constants.STARTING_DAILY_AT_MINUTE);
    	Optional<Integer> secondProperty = getIntegerProperty(sr, Constants.STARTING_DAILY_AT_SECOND);
    	
    	if(hourProperty.isPresent()) {
    		int second = 0, minute = 0;
    		if(minuteProperty.isPresent()) {
    			minute = minuteProperty.get();
    		}
    		if(secondProperty.isPresent()) {
    			second = secondProperty.get();
    		}
    		return new DailyAt(hourProperty.get(), minute, second);
    	} else {
    		if(minuteProperty.isPresent() || secondProperty.isPresent()) {
    			m_logService.log(LogService.LOG_WARNING, Constants.STARTING_DAILY_AT_HOUR + " property not present");
    		}
    	}
    	
    	final StartingDailyAt annotation = element.getAnnotation(StartingDailyAt.class);
    	if(annotation != null) {
    		return new DailyAt(annotation.hour(), annotation.minute(), annotation.second());
    	}
    	return null;
    }
    
    private DailyAt getEndingDailyAt(AnnotatedElement element, ServiceReference<?> sr) {
    	Optional<Integer> hourProperty = getIntegerProperty(sr, Constants.ENDING_DAILY_AT_HOUR);
    	Optional<Integer> minuteProperty = getIntegerProperty(sr, Constants.ENDING_DAILY_AT_MINUTE);
    	Optional<Integer> secondProperty = getIntegerProperty(sr, Constants.ENDING_DAILY_AT_SECOND);
    	
    	if(hourProperty.isPresent()) {
    		int second = 0, minute = 0;
    		if(minuteProperty.isPresent()) {
    			minute = minuteProperty.get();
    		}
    		if(secondProperty.isPresent()) {
    			second = secondProperty.get();
    		}
    		return new DailyAt(hourProperty.get(), minute, second);
    	} else {
    		if(minuteProperty.isPresent() || secondProperty.isPresent()) {
    			m_logService.log(LogService.LOG_WARNING, Constants.ENDING_DAILY_AT_HOUR + " property not present");
    		}
    	}
    	
    	final EndingDailyAt annotation = element.getAnnotation(EndingDailyAt.class);
    	if(annotation != null) {
    		return new DailyAt(annotation.hour(), annotation.minute(), annotation.second());
    	}
    	return null;
    }

    private Optional<String> getStringProperty(ServiceReference<?> sr, String propertyName) {
    	return Optional.ofNullable((String)sr.getProperty(propertyName));
    }
    
    private Optional<Integer> getIntegerProperty(ServiceReference<?> sr, String propertyName) {
    	return Optional.ofNullable((Integer)sr.getProperty(propertyName));
    }
    @SuppressWarnings("unchecked")
	private Optional<List<Integer>> getIntegerListProperty(ServiceReference<?> sr, String propertyName) {
    	return Optional.ofNullable((List<Integer>)sr.getProperty(propertyName));
    }
    
    private Optional<Long> getLongProperty(ServiceReference<?> sr, String propertyName) {
    	return Optional.ofNullable((Long)sr.getProperty(propertyName));
    }
    
    private Optional<Boolean> getBooleanProperty(ServiceReference<?> sr, String propertyName) {
    	return Optional.ofNullable((Boolean)sr.getProperty(propertyName));
    }
    
    private class RepeatIntervalInner {
    	private RepeatIntervalPeriod m_period;
    	private Long m_value;
    	
    	public RepeatIntervalInner(RepeatIntervalPeriod period, Long value) {
    		m_period = period;
    		m_value = value;
		}
    	
    	public Long getValueInMilliseconds() {
    		Long factor = Long.valueOf(1000);
    		switch (m_period) {
			case MILLISECOND:
				factor = Long.valueOf(1);
				break;
			case SECOND:
				factor = Long.valueOf(1000);
				break;
			case MINUTE:
				factor = Long.valueOf(60 * 1000);
				break;
			case HOUR:
				factor = Long.valueOf(60 * 60 * 1000);
				break;
			case DAY:
				factor = Long.valueOf(24 * 60 * 60 * 1000);
				break;
			}
    		return m_value * factor;
    	}
    }
    
    private class IntervalInner {
    	private IntervalPeriod m_period;
    	private Integer m_value;
    	
    	public IntervalInner(IntervalPeriod period, Integer value) {
    		m_period = period;
    		m_value = value;
		}
    	
    	public Integer getValueInSeconds() {
    		Integer factor = Integer.valueOf(1);
    		switch (m_period) {
			case SECOND:
				factor = Integer.valueOf(1);
				break;
			case MINUTE:
				factor = Integer.valueOf(60);
				break;
			case HOUR:
				factor = Integer.valueOf(60 * 60);
				break;
			}
    		return m_value * factor;

    	}
    }
    
    private class DailyAt {
    	private int m_hour;
    	private int m_minute;
    	private int m_second;
    	
    	public DailyAt(int hour, int minute, int second) {
    		m_hour = hour;
    		m_minute = minute;
    		m_second = second;
		}
    	
    	public int getHour() {
    		return m_hour;
    	}
    	public int getMinute() {
    		return m_minute;
    	}
    	public int getSecond() {
    		return m_second;
    	}
    }

}
