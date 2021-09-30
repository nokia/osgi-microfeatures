package com.nsn.ood.cls.core.db.creator;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.nsn.ood.cls.core.convert.LicenseMode2IntegerConverter;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;

@Ignore
public class LicenseCreatorFastTest {
	
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter = createMock(LicenseMode2IntegerConverter.class);
	private Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);

	private LicenseCreatorFast fast;
	private LicenseCreator normal;

	@Before
	public void setUp() throws Exception {
		this.fast = new LicenseCreatorFast();
		this.normal = new LicenseCreator();
		
		expect(timestamp2DateTimeConverter.convertTo(ResultSetImp.END_DATE_TS)).andReturn(ResultSetImp.END_DATE).times(150 * 32 * 1000 * 2);
		expect(timestamp2DateTimeConverter.convertTo(ResultSetImp.START_DATE_TS)).andReturn(ResultSetImp.START_DATE).times(150 * 32 * 1000);
		expect(licenseMode2IntegerConverter.convertFrom(1)).andReturn(License.Mode.ON_OFF).times(150 * 32 * 1000 * 2);
		expect(licenseType2IntegerConverter.convertFrom(2)).andReturn(License.Type.POOL).times(150 * 32 * 1000 * 2);

		replayAll();
		
		setInternalState(fast, "timestamp2DateTimeConverter", timestamp2DateTimeConverter);
		setInternalState(fast, "licenseMode2IntegerConverter", licenseMode2IntegerConverter);
		setInternalState(fast, "licenseType2IntegerConverter", licenseType2IntegerConverter);
		setInternalState(normal, "timestamp2DateTimeConverter", timestamp2DateTimeConverter);
		setInternalState(normal, "licenseMode2IntegerConverter", licenseMode2IntegerConverter);
		setInternalState(normal, "licenseType2IntegerConverter", licenseType2IntegerConverter);
	}

	/**
	 *
	 */
	private List<ResultSet> createData(final int size) throws SQLException {
		System.out.println("Starting data creation");
		final List<ResultSet> result = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			result.add(createResultSet());
		}
		System.out.println("Data has been created!");
		return result;
	}

	private ResultSet createResultSet() throws SQLException {
		return new ResultSetImp();
		// final ResultSet resultSetMock = createMock(ResultSet.class);
		//
		// expect(resultSetMock.getString("serialnumber")).andReturn("serialnumber");
		// expect(resultSetMock.getString("capacityunit")).andReturn("capacityunit");
		// expect(resultSetMock.getString("code")).andReturn("code");
		// expect(resultSetMock.getTimestamp("enddate")).andReturn(END_DATE_TS);
		// // expect(this.converterMock.convert(END_DATE_TS, Timestamp.class, DateTime.class)).andReturn(END_DATE);
		// expect(resultSetMock.getString("filename")).andReturn("filename");
		// expect(resultSetMock.getInt("mode")).andReturn(1);
		// // expect(this.converterMock.convert(1, License.Mode.class)).andReturn(License.Mode.CAPACITY);
		// expect(resultSetMock.getString("name")).andReturn("name");
		// expect(resultSetMock.getTimestamp("startdate")).andReturn(START_DATE_TS);
		// // expect(this.converterMock.convert(START_DATE_TS, Timestamp.class, DateTime.class)).andReturn(START_DATE);
		// expect(resultSetMock.getString("targettype")).andReturn("targettype");
		// expect(resultSetMock.getLong("total")).andReturn(1L);
		// expect(resultSetMock.getInt("type")).andReturn(2);
		// // expect(this.converterMock.convert(2, License.Type.class)).andReturn(License.Type.POOL);
		// expect(resultSetMock.getLong("used")).andReturn(23L);
		//
		// expect(resultSetMock.getLong("featurecode")).andReturn(1234L);
		// expect(resultSetMock.getString("featurename")).andReturn("featurename");
		//
		// expect(resultSetMock.getString("targetid")).andReturn("targetId");
		// return resultSetMock;
	}
	

	@Test
	public void testPerformance() throws Exception {
		final Stopwatch sw = Stopwatch.createUnstarted();
		final List<License> res = new ArrayList<>();

		final int dataSize = 150 * // licenses to retrieve for every client
				32 * 1000;// clients

		List<ResultSet> data = createData(dataSize);

		resetStopWatch(sw);
		for (final ResultSet resultSet : data) {
			res.add(this.normal.createLicense(resultSet));
		}
		System.out.println("Normal: " + sw.elapsed(TimeUnit.SECONDS));
		System.out.println(res.getClass());
		res.clear();

		data = createData(dataSize);

		resetStopWatch(sw);
		for (final ResultSet resultSet : data) {
			res.add(this.fast.createLicense(resultSet));
		}
		System.out.println("Fast: " + sw.elapsed(TimeUnit.SECONDS));
		System.out.println(res.getClass());

	}

	/**
	 * @param sw
	 */
	private void resetStopWatch(final Stopwatch sw) {
		sw.reset();
		sw.start();
	}

	private static class ResultSetImp implements ResultSet {
		private static final DateTime END_DATE = new DateTime(2014, 11, 26, 13, 48, 15);
		private static final DateTime START_DATE = new DateTime(2014, 1, 2, 3, 8, 5);
		private static final Timestamp START_DATE_TS = new Timestamp(START_DATE.getMillis());
		private static final Timestamp END_DATE_TS = new Timestamp(END_DATE.getMillis());

		@Override
		public <T> T unwrap(final Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(final Class<?> iface) throws SQLException {
			return false;
		}

		@Override
		public boolean next() throws SQLException {
			return false;
		}

		@Override
		public void close() throws SQLException {
		}

		@Override
		public boolean wasNull() throws SQLException {
			return false;
		}

		@Override
		public String getString(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public boolean getBoolean(final int columnIndex) throws SQLException {
			return false;
		}

		@Override
		public byte getByte(final int columnIndex) throws SQLException {
			return 0;
		}

		@Override
		public short getShort(final int columnIndex) throws SQLException {
			return 0;
		}

		@Override
		public int getInt(final int columnIndex) throws SQLException {
			return 0;
		}

		@Override
		public long getLong(final int columnIndex) throws SQLException {
			return 0;
		}

		@Override
		public float getFloat(final int columnIndex) throws SQLException {
			return 0;
		}

		@Override
		public double getDouble(final int columnIndex) throws SQLException {
			return 0;
		}

		@Override
		public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
			return null;
		}

		@Override
		public byte[] getBytes(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Date getDate(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Time getTime(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Timestamp getTimestamp(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public InputStream getAsciiStream(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public InputStream getBinaryStream(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public String getString(final String columnLabel) throws SQLException {
			return columnLabel;
		}

		@Override
		public boolean getBoolean(final String columnLabel) throws SQLException {
			return false;
		}

		@Override
		public byte getByte(final String columnLabel) throws SQLException {
			return 0;
		}

		@Override
		public short getShort(final String columnLabel) throws SQLException {
			return 0;
		}

		@Override
		public int getInt(final String columnLabel) throws SQLException {
			switch (columnLabel) {
				case "mode":
					return 1;
				case "type":
					return 2;
				default:
					return 0;
			}

		}

		@Override
		public long getLong(final String columnLabel) throws SQLException {
			switch (columnLabel) {
				case "total":
					return 1L;
				case "used":
					return 23L;
				case "featurecode":
					return 1234L;
				default:
					return 0L;
			}
		}

		@Override
		public float getFloat(final String columnLabel) throws SQLException {
			return 0;
		}

		@Override
		public double getDouble(final String columnLabel) throws SQLException {
			return 0;
		}

		@Override
		public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
			return null;
		}

		@Override
		public byte[] getBytes(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Date getDate(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Time getTime(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Timestamp getTimestamp(final String columnLabel) throws SQLException {
			switch (columnLabel) {
				case "enddate":
					return END_DATE_TS;
				case "startdate":
					return START_DATE_TS;
				default:
					return null;
			}
		}

		@Override
		public InputStream getAsciiStream(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public InputStream getBinaryStream(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return null;
		}

		@Override
		public void clearWarnings() throws SQLException {
		}

		@Override
		public String getCursorName() throws SQLException {
			return null;
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException {
			return null;
		}

		@Override
		public Object getObject(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Object getObject(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public int findColumn(final String columnLabel) throws SQLException {
			return 0;
		}

		@Override
		public Reader getCharacterStream(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Reader getCharacterStream(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public boolean isBeforeFirst() throws SQLException {
			return false;
		}

		@Override
		public boolean isAfterLast() throws SQLException {
			return false;
		}

		@Override
		public boolean isFirst() throws SQLException {
			return false;
		}

		@Override
		public boolean isLast() throws SQLException {
			return false;
		}

		@Override
		public void beforeFirst() throws SQLException {
		}

		@Override
		public void afterLast() throws SQLException {
		}

		@Override
		public boolean first() throws SQLException {
			return false;
		}

		@Override
		public boolean last() throws SQLException {
			return false;
		}

		@Override
		public int getRow() throws SQLException {
			return 0;
		}

		@Override
		public boolean absolute(final int row) throws SQLException {
			return false;
		}

		@Override
		public boolean relative(final int rows) throws SQLException {
			return false;
		}

		@Override
		public boolean previous() throws SQLException {
			return false;
		}

		@Override
		public void setFetchDirection(final int direction) throws SQLException {
		}

		@Override
		public int getFetchDirection() throws SQLException {
			return 0;
		}

		@Override
		public void setFetchSize(final int rows) throws SQLException {
		}

		@Override
		public int getFetchSize() throws SQLException {
			return 0;
		}

		@Override
		public int getType() throws SQLException {
			return 0;
		}

		@Override
		public int getConcurrency() throws SQLException {
			return 0;
		}

		@Override
		public boolean rowUpdated() throws SQLException {
			return false;
		}

		@Override
		public boolean rowInserted() throws SQLException {
			return false;
		}

		@Override
		public boolean rowDeleted() throws SQLException {
			return false;
		}

		@Override
		public void updateNull(final int columnIndex) throws SQLException {
		}

		@Override
		public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
		}

		@Override
		public void updateByte(final int columnIndex, final byte x) throws SQLException {
		}

		@Override
		public void updateShort(final int columnIndex, final short x) throws SQLException {
		}

		@Override
		public void updateInt(final int columnIndex, final int x) throws SQLException {
		}

		@Override
		public void updateLong(final int columnIndex, final long x) throws SQLException {
		}

		@Override
		public void updateFloat(final int columnIndex, final float x) throws SQLException {
		}

		@Override
		public void updateDouble(final int columnIndex, final double x) throws SQLException {
		}

		@Override
		public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
		}

		@Override
		public void updateString(final int columnIndex, final String x) throws SQLException {
		}

		@Override
		public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
		}

		@Override
		public void updateDate(final int columnIndex, final Date x) throws SQLException {
		}

		@Override
		public void updateTime(final int columnIndex, final Time x) throws SQLException {
		}

		@Override
		public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
		}

		@Override
		public void updateAsciiStream(final int columnIndex, final InputStream x, final int length)
				throws SQLException {
		}

		@Override
		public void updateBinaryStream(final int columnIndex, final InputStream x, final int length)
				throws SQLException {
		}

		@Override
		public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
		}

		@Override
		public void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {
		}

		@Override
		public void updateObject(final int columnIndex, final Object x) throws SQLException {
		}

		@Override
		public void updateNull(final String columnLabel) throws SQLException {
		}

		@Override
		public void updateBoolean(final String columnLabel, final boolean x) throws SQLException {
		}

		@Override
		public void updateByte(final String columnLabel, final byte x) throws SQLException {
		}

		@Override
		public void updateShort(final String columnLabel, final short x) throws SQLException {
		}

		@Override
		public void updateInt(final String columnLabel, final int x) throws SQLException {
		}

		@Override
		public void updateLong(final String columnLabel, final long x) throws SQLException {
		}

		@Override
		public void updateFloat(final String columnLabel, final float x) throws SQLException {
		}

		@Override
		public void updateDouble(final String columnLabel, final double x) throws SQLException {
		}

		@Override
		public void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {
		}

		@Override
		public void updateString(final String columnLabel, final String x) throws SQLException {
		}

		@Override
		public void updateBytes(final String columnLabel, final byte[] x) throws SQLException {
		}

		@Override
		public void updateDate(final String columnLabel, final Date x) throws SQLException {
		}

		@Override
		public void updateTime(final String columnLabel, final Time x) throws SQLException {
		}

		@Override
		public void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {
		}

		@Override
		public void updateAsciiStream(final String columnLabel, final InputStream x, final int length)
				throws SQLException {
		}

		@Override
		public void updateBinaryStream(final String columnLabel, final InputStream x, final int length)
				throws SQLException {
		}

		@Override
		public void updateCharacterStream(final String columnLabel, final Reader reader, final int length)
				throws SQLException {
		}

		@Override
		public void updateObject(final String columnLabel, final Object x, final int scaleOrLength)
				throws SQLException {
		}

		@Override
		public void updateObject(final String columnLabel, final Object x) throws SQLException {
		}

		@Override
		public void insertRow() throws SQLException {
		}

		@Override
		public void updateRow() throws SQLException {
		}

		@Override
		public void deleteRow() throws SQLException {
		}

		@Override
		public void refreshRow() throws SQLException {
		}

		@Override
		public void cancelRowUpdates() throws SQLException {
		}

		@Override
		public void moveToInsertRow() throws SQLException {
		}

		@Override
		public void moveToCurrentRow() throws SQLException {
		}

		@Override
		public Statement getStatement() throws SQLException {
			return null;
		}

		@Override
		public Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
			return null;
		}

		@Override
		public Ref getRef(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Blob getBlob(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Clob getClob(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Array getArray(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
			return null;
		}

		@Override
		public Ref getRef(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Blob getBlob(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Clob getClob(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Array getArray(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
			return null;
		}

		@Override
		public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
			return null;
		}

		@Override
		public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
			return null;
		}

		@Override
		public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
			return null;
		}

		@Override
		public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
			return null;
		}

		@Override
		public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
			return null;
		}

		@Override
		public URL getURL(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public URL getURL(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public void updateRef(final int columnIndex, final Ref x) throws SQLException {
		}

		@Override
		public void updateRef(final String columnLabel, final Ref x) throws SQLException {
		}

		@Override
		public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
		}

		@Override
		public void updateBlob(final String columnLabel, final Blob x) throws SQLException {
		}

		@Override
		public void updateClob(final int columnIndex, final Clob x) throws SQLException {
		}

		@Override
		public void updateClob(final String columnLabel, final Clob x) throws SQLException {
		}

		@Override
		public void updateArray(final int columnIndex, final Array x) throws SQLException {
		}

		@Override
		public void updateArray(final String columnLabel, final Array x) throws SQLException {
		}

		@Override
		public RowId getRowId(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public RowId getRowId(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public void updateRowId(final int columnIndex, final RowId x) throws SQLException {
		}

		@Override
		public void updateRowId(final String columnLabel, final RowId x) throws SQLException {
		}

		@Override
		public int getHoldability() throws SQLException {
			return 0;
		}

		@Override
		public boolean isClosed() throws SQLException {
			return false;
		}

		@Override
		public void updateNString(final int columnIndex, final String nString) throws SQLException {
		}

		@Override
		public void updateNString(final String columnLabel, final String nString) throws SQLException {
		}

		@Override
		public void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {
		}

		@Override
		public void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {
		}

		@Override
		public NClob getNClob(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public NClob getNClob(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public SQLXML getSQLXML(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public SQLXML getSQLXML(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {
		}

		@Override
		public void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {
		}

		@Override
		public String getNString(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public String getNString(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public Reader getNCharacterStream(final int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public Reader getNCharacterStream(final String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public void updateNCharacterStream(final int columnIndex, final Reader x, final long length)
				throws SQLException {
		}

		@Override
		public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length)
				throws SQLException {
		}

		@Override
		public void updateAsciiStream(final int columnIndex, final InputStream x, final long length)
				throws SQLException {
		}

		@Override
		public void updateBinaryStream(final int columnIndex, final InputStream x, final long length)
				throws SQLException {
		}

		@Override
		public void updateCharacterStream(final int columnIndex, final Reader x, final long length)
				throws SQLException {
		}

		@Override
		public void updateAsciiStream(final String columnLabel, final InputStream x, final long length)
				throws SQLException {
		}

		@Override
		public void updateBinaryStream(final String columnLabel, final InputStream x, final long length)
				throws SQLException {
		}

		@Override
		public void updateCharacterStream(final String columnLabel, final Reader reader, final long length)
				throws SQLException {
		}

		@Override
		public void updateBlob(final int columnIndex, final InputStream inputStream, final long length)
				throws SQLException {
		}

		@Override
		public void updateBlob(final String columnLabel, final InputStream inputStream, final long length)
				throws SQLException {
		}

		@Override
		public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
		}

		@Override
		public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
		}

		@Override
		public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
		}

		@Override
		public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
		}

		@Override
		public void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {
		}

		@Override
		public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
		}

		@Override
		public void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {
		}

		@Override
		public void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {
		}

		@Override
		public void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {
		}

		@Override
		public void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {
		}

		@Override
		public void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {
		}

		@Override
		public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
		}

		@Override
		public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
		}

		@Override
		public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
		}

		@Override
		public void updateClob(final int columnIndex, final Reader reader) throws SQLException {
		}

		@Override
		public void updateClob(final String columnLabel, final Reader reader) throws SQLException {
		}

		@Override
		public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
		}

		@Override
		public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
		}

		@Override
		public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
			return null;
		}

		@Override
		public <T> T getObject(final String columnLabel, final Class<T> type) throws SQLException {
			return null;
		}

	}

}
