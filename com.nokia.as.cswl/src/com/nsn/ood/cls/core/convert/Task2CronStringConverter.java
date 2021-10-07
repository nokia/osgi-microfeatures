/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.internal.TaskExpression;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Task expression <-> Schedule expression
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "task")
@Property(name = "to", value = "cronString")
public class Task2CronStringConverter implements Converter<TaskExpression, String> {
	
	@Override
	public String convertTo(final TaskExpression task) {
		if (task == null) {
			throw new CLSIllegalArgumentException("Task expression must not be null");
		}
		
		return task.getSecond() + " " + task.getMinute() + " " + task.getHour() + " " +
			   task.getDayOfMonth() + " " + task.getMonth() + " " + task.getDayOfWeek() + " " +
			   task.getYear();
	}

	@Override
	public TaskExpression convertFrom(final String cronString) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
