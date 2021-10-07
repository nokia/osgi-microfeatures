/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.convert.License2StringConverter;
import com.nsn.ood.cls.core.convert.Reservation2StringConverter;
import com.nsn.ood.cls.core.operation.util.ReservationsCalculator.CapacityException;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;


/**
 * @author marynows
 * 
 */
class ReservationsCalculatorTester {
	private static final Pattern CONST_PATTERN = Pattern.compile("(\\w+)=(.+)");
	private static final Pattern TEST_BLOCK_PATTERN = Pattern.compile("(capacity|on_off)\\s+\\{");
	private static final Pattern TEST_LINE_PATTERN = Pattern.compile("([flrne]):\\s*(\\w+(\\s*,\\s*\\w+)*)");

	private final Map<String, String> constants = new HashMap<>();
	private final String resourceName;

	private ReservationsCalculatorSingleTest test;
	private int executed = 0;
	private int lineNumber;

	public ReservationsCalculatorTester(final String resourceName) {
		this.resourceName = resourceName;
	}

	public int size() {
		return this.executed;
	}

	public ReservationsCalculatorTester test() {
		this.lineNumber = 0;
		try (BufferedReader bufferedReader = createReader()) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				this.lineNumber++;

				if (isEmptyOrComment(line)) {
					continue;
				}
				if (isInTestBlock()) {
					if (handleTestBlockEnd(line)) {
						continue;
					}
					if (handleTestBlockLine(line)) {
						continue;
					}
				} else {
					if (handleConst(line)) {
						continue;
					}
					if (handleTestBlockBegin(line)) {
						continue;
					}
				}
			}
		} catch (final IOException e) {
			fail(e.getMessage());
		} catch (final AssertionError e) {
			throw new AssertionError("Error at line: " + this.lineNumber, e);
		} catch (final Exception e) {
			throw new RuntimeException("Error at line: " + this.lineNumber, e);
		}
		return this;
	}

	private boolean handleTestBlockBegin(final String line) {
		final Matcher matcher = TEST_BLOCK_PATTERN.matcher(line);
		if (matcher.matches()) {
			this.test = new ReservationsCalculatorSingleTest(matcher.group(1), this.constants.get("NOW"));
			return true;
		}
		return false;
	}

	private boolean handleTestBlockLine(final String line) {
		final Matcher matcher = TEST_LINE_PATTERN.matcher(line);
		if (matcher.matches()) {
			final List<String> values = new ArrayList<>();
			for (final String value : splitValues(matcher.group(2))) {
				if (this.constants.containsKey(value)) {
					values.add(this.constants.get(value));
				} else {
					values.add(value);
				}
			}
			this.test.setData(matcher.group(1), values);
			return true;
		}
		return false;
	}

	private List<String> splitValues(final String value) {
		final List<String> result = new ArrayList<>();
		for (final String s : value.split(",")) {
			result.add(s.trim());
		}
		return result;
	}

	private boolean handleTestBlockEnd(final String line) {
		if ("}".equals(line)) {
			this.test.run();
			this.test = null;
			this.executed++;
			return true;
		}
		return false;
	}

	private boolean isInTestBlock() {
		return this.test != null;
	}

	private boolean handleConst(final String line) {
		final Matcher matcher = CONST_PATTERN.matcher(line);
		if (matcher.matches()) {
			this.constants.put(matcher.group(1).trim(), matcher.group(2).trim());
			return true;
		}
		return false;
	}

	private boolean isEmptyOrComment(final String line) {
		return (line.isEmpty() || line.startsWith("//"));
	}

	private BufferedReader createReader() {
		return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(
				"/reservationsCalculatorTester/" + this.resourceName)));
	}
}

class ReservationsCalculatorSingleTest {
	private final boolean capacity;
	private final DateTime now;
	private final List<License> licenses = new ArrayList<>();
	private final List<Reservation> reservations = new ArrayList<>();
	private final List<Reservation> newReservations = new ArrayList<>();

	private Boolean errorIsRelease;
	private Long errorCapacity;
	private long featureCapacity = 100L;

	public ReservationsCalculatorSingleTest(final String type, final String now) {
		this.now = DateTime.parse(now);
		if ("capacity".equals(type)) {
			this.capacity = true;
		} else if ("on_off".equals(type)) {
			this.capacity = false;
		} else {
			throw new IllegalArgumentException("Unknown type: " + type);
		}
	}

	public void setData(final String type, final List<String> values) {
		switch (type) {
			case "f":
				setFeature(values);
				break;
			case "l":
				setLicense(values);
				break;
			case "r":
				setReservation(values);
				break;
			case "n":
				setNewReservation(values);
				break;
			case "e":
				setError(values);
				break;
		}
	}

	private void setFeature(final List<String> values) {
		this.featureCapacity = Long.valueOf(values.get(0));
	}

	private void setLicense(final List<String> values) { // l: serial, total, used, [pool | floating] <- license
		final License.Type type;
		if ("floating".equals(values.get(3))) {
			type = License.Type.FLOATING_POOL;
		} else if ("pool".equals(values.get(3))) {
			type = License.Type.POOL;
		} else {
			throw new IllegalArgumentException("Unknown license type: " + values.get(3));
		}
		this.licenses.add(license(values.get(0), Long.valueOf(values.get(1)), Long.valueOf(values.get(2)), type, null));
	}

	private void setReservation(final List<String> values) { // r: serial, capacity, time <- current reservation
		this.reservations.add(reservation(null, null, values.get(0), Long.valueOf(values.get(1)),
				DateTime.parse(values.get(2)), null, null, null, null));
	}

	private void setNewReservation(final List<String> values) { // n: serial, (capacity), time <- new reservation
		final Long capacity;
		final DateTime reservationTime;
		if (this.capacity) {
			capacity = Long.valueOf(values.get(1));
			reservationTime = DateTime.parse(values.get(2));
		} else {
			capacity = 1L;
			reservationTime = DateTime.parse(values.get(1));
		}
		this.newReservations.add(reservation(null, null, values.get(0), capacity, reservationTime, null, null, null,
				null));
	}

	private void setError(final List<String> values) { // e: [capacity | on_off | release], (capacity) <- exception
		if ("capacity".equals(values.get(0))) {
			this.errorIsRelease = false;
			this.errorCapacity = Long.valueOf(values.get(1));
		} else if ("on_off".equals(values.get(0))) {
			this.errorIsRelease = false;
			this.errorCapacity = 0L;
		} else if ("release".equals(values.get(0))) {
			this.errorIsRelease = true;
			this.errorCapacity = Long.valueOf(values.get(1));
		} else {
			throw new IllegalArgumentException("Unknown error type: " + values.get(0));
		}
	}

	public void run() {
		try {
			final ReservationsCalculator calculator = new ReservationsCalculator(this.licenses, this.reservations,
					(this.capacity ? this.featureCapacity : 1), this.now, new Reservation2StringConverter(), new License2StringConverter());
			final List<Reservation> result = calculator.calculate();

			if (this.errorIsRelease == null) {
				assertEquals(this.newReservations.size(), result.size());
				for (int i = 0; i < this.newReservations.size(); i++) {
					assertEquals(this.newReservations.get(i).getSerialNumber(), result.get(i).getSerialNumber());
					assertEquals(this.newReservations.get(i).getCapacity(), result.get(i).getCapacity());
					assertEquals(this.newReservations.get(i).getReservationTime(), result.get(i).getReservationTime());
				}
			} else {
				fail("Expected CalculationException has not been thrown.");
			}
		} catch (final CapacityException e) {
			if (this.errorIsRelease == null) {
				fail("Unexpected CalculationException.");
			} else {
				assertEquals(this.errorIsRelease, Boolean.valueOf(e.isRelease()));
				assertEquals(this.errorCapacity, Long.valueOf(e.getCapacity()));
			}
		}
	}
}
