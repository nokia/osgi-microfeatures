/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author wro50095
 *
 */
@Component(provides = BulkUpdateReservations.class)
public class BulkUpdateReservations {
	private static final int CLIENT_ID = 1;
	private static final int FEATURE_CODE = 2;
	private static final int SERIAL_NUMBER = 3;
	private static final int CAPACITY = 4;
	private static final int RESERVATION_TIME = 5;
	private static final int MODE = 6;
	private static final int TYPE = 7;
	private static final int END_DATE = 8;
	private static final int FILE_NAME = 9;

	@Resource(lookup = "java:/datasources/CLSMariaDS")
	private DataSource dataSource;
	
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	
	@ServiceDependency(filter = "(&(from=licenseMode)(to=integer))")
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	
	@ServiceDependency(filter = "(&(from=licenseType)(to=integer))")
	private Converter<License.Type, Integer> licenseType2IntegerConverter;

	private Connection connection;
	private PreparedStatement reservationsDeleteStatement;
	private PreparedStatement reservationInsertStatement;

	private long featureCode;

	public void init(final long featureCode) throws UnknownErrorException {
		this.featureCode = featureCode;
		try {
			initConnection();
			prepareReservationsDeleteStatement();
			prepareReservationsInsertStatement();
		} catch (final SQLException e) {
			throw new UnknownErrorException("Error occured during statement creation", e);
		}
	}

	private void initConnection() throws SQLException {
		this.connection = this.dataSource.getConnection();
	}

	public void addReservationsToUpdate(final Client client, final List<Reservation> reservations) throws UnknownErrorException {
		try {
			addReservationDelete(this.reservationsDeleteStatement, client);
			addReservationInsert(this.reservationInsertStatement, reservations);
		} catch (final SQLException e) {
			throw new UnknownErrorException("Error occured during reservation update", e);
		}

	}

	public void performReservationsUpdate() throws UnknownErrorException {
		try {
			this.reservationsDeleteStatement.executeBatch();
			this.reservationInsertStatement.executeBatch();
		} catch (final SQLException e) {
			throw new UnknownErrorException("Error occured during reservations update", e);
		} finally {
			try {
				this.reservationsDeleteStatement.close();
				this.reservationInsertStatement.close();
				this.connection.close();
			} catch (final SQLException e) {
				throw new UnknownErrorException("Error occured during prepared statement closing", e);
			}
		}

	}

	private void prepareReservationsInsertStatement() throws SQLException {
		this.reservationInsertStatement = this.connection.prepareStatement("insert into cls.reservations"
				+ " (clientid, featurecode, serialnumber, capacity, reservationtime, mode, type, enddate, filename)"
				+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
	}

	private void addReservationInsert(final PreparedStatement statement, final List<Reservation> reservations) throws SQLException {
		for (final Reservation reservation : reservations) {
			statement.setString(CLIENT_ID, reservation.getClientId());
			statement.setLong(FEATURE_CODE, reservation.getFeatureCode());
			statement.setString(SERIAL_NUMBER, reservation.getSerialNumber());
			statement.setLong(CAPACITY, reservation.getCapacity());
			statement.setTimestamp(RESERVATION_TIME, timestamp2DateTimeConverter.convertFrom(reservation.getReservationTime()));
			statement.setInt(MODE, licenseMode2IntegerConverter.convertTo(reservation.getMode()));
			statement.setInt(TYPE, licenseType2IntegerConverter.convertTo(reservation.getType()));
			statement.setTimestamp(END_DATE, timestamp2DateTimeConverter.convertFrom(reservation.getEndDate()));
			statement.setString(FILE_NAME, reservation.getFileName());
			statement.addBatch();
		}
	}

	private void prepareReservationsDeleteStatement() throws SQLException {
		this.reservationsDeleteStatement = this.connection
				.prepareStatement("delete from cls.reservations where clientid = ? and featurecode = ?");
	}

	private void addReservationDelete(final PreparedStatement deleteReservationsQuery, final Client client) throws SQLException {
		deleteReservationsQuery.setString(CLIENT_ID, client.getClientId());
		deleteReservationsQuery.setLong(FEATURE_CODE, this.featureCode);
		deleteReservationsQuery.addBatch();
	}
}
