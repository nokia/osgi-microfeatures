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
package org.amdatu.scheduling.constants;

/**
 * Defines names for properties used to configure Jobs via service properties.
 * The values associated with these keys are indicated for each key.
 *
 */
public interface Constants {
	
	/**
	 * Property identifying a Job's cron settings. The value of this property
	 * must be <code>String</code>.
	 */
	public static final String CRON = "cron";
	
	/**
	 *  Property identifying a Job's description. The value of this property 
	 *  must be <code>String</code>.
	 */
	public static final String DESCRIPTION = "description";
	
	/**
	 * Property identifying a Job's 'modified by calendar' setting. The value
	 * of this property must be <code>String</code>. 
	 */
	public static final String MODIFIED_BY_CALENDAR = "modified.by.calendar";
	
	/**
	 * Property identifying a Job's priority setting. The value of this property
	 * must be <code>Integer</code>.
	 */
	public static final String PRIORITY = "priority";
	
	/**
	 * Property identifying a Job's repeat count setting. The value of this property
	 * must be <code>Integer</code>.
	 */
	public static final String REPEAT_COUNT = "repeat.count";
	
	/**
	 * Property identifying a Job's repeat forever setting. The value of this property
	 * must be <code>Boolean</code>.
	 */
	public static final String REPEAT_FOREVER = "repeat.forever";
	
	/**
	 * Property identifying a Job's repeat interval value. The value of this property
	 * must be <code>Long</code>.
	 */
	public static final String REPEAT_INTERVAL_VALUE = "repeat.interval.value";
	
	/**
	 * Property identifying a Job's repeat interval period. The value of this property
	 * must be <code>String</code>. It can be one of the following: millisecond, second,
	 * minute, hour, day. If the repeat interval period is missing but the repeat interval
	 * value is present, the default period will be set to "second".
	 */
	public static final String REPEAT_INTERVAL_PERIOD = "repeat.interval.period";
	
	/**
	 * Property identifying a Job's request recovery setting. The value of this property
	 * must be <code>Boolean<code>.
	 */
	public static final String REQUEST_RECOVERY = "request.recovery";
	
	/**
	 * Property identifying a Job's days of the week setting. The value of this property
	 * must be <code>List&ltInteger></code>.
	 */
	public static final String DAYS_OF_THE_WEEK = "days.of.week";
	
	/**
	 * Property identifying a Job's ending daily at hour setting. The value of this
	 * property must be <code>Integer</code>.
	 */
	public static final String ENDING_DAILY_AT_HOUR = "end.daily.hour";
	
	/**
	 * Property identifying a Job's ending daily at minute setting. The value of this
	 * property must be <code>Integer</code>. If this property is not present, its
	 * value will be set to 0.
	 */
	public static final String ENDING_DAILY_AT_MINUTE = "end.daily.minute";
	
	/**
	 * Property identifying a Job's ending daily at second setting. The value of this
	 * property must be <code>Integer</code>. If this property is not present, its
	 * value will be set to 0.
	 */
	public static final String ENDING_DAILY_AT_SECOND = "end.daily.second";
	
	/**
	 * Property identifying a Job's starting daily at hour setting. The value of this
	 * property must be <code>Integer</code>.
	 */
	public static final String STARTING_DAILY_AT_HOUR = "start.daily.hour";
	
	/**
	 * Property identifying a Job's starting daily at minute setting. The value of this
	 * property must be <code>Integer</code>. If this property is not present, its
	 * value will be set to 0.
	 */
	public static final String STARTING_DAILY_AT_MINUTE = "start.daily.minute";
	
	/**
	 * Property identifying a Job's starting daily at second setting. The value of this
	 * property must be <code>Integer</code>. If this property is not present, its
	 * value will be set to 0.
	 */
	public static final String STARTING_DAILY_AT_SECOND = "start.daily.second";
	
	/**
	 * Property identifying a Job's every day setting. The value of this property must
	 * be <code>Boolean</code>.
	 */
	public static final String EVERY_DAY = "every.day";
	
	/**
	 * Property identifying a Job's interval value. The value of this property
	 * must be <code>Long</code>.
	 */
	public static final String INTERVAL_VALUE = "interval.value";
	
	/**
	 * Property identifying a Job's interval period. The value of this property
	 * must be <code>String</code>. It can be one of the following: second,
	 * minute, hour. If the repeat interval period is missing but the repeat interval
	 * value is present, the default period will be set to "second".
	 */
	public static final String INTERVAL_PERIOD = "interval.period";
	
	/**
	 * Property identifying a Job's Monday through Friday setting. The value of this
	 * property must be <code>Boolean</code>. 
	 */
	public static final String MONDAY_THROUGH_FRIDAY = "monday.friday";
	
	/**
	 * Property identifying a Job's Saturday and Sunday setting. The value of this
	 * property must be <code>Boolean</code>.
	 */
	public static final String SATURDAY_AND_SUNDAY = "saturday.sunday";

}
