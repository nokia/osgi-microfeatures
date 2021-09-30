package com.nokia.licensing.querycache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.nokia.licensing.logging.LicenseLogger;


public class LicenseSQLParser {

	LicenseSQLParser() {
	}

	/**
	 * Parses input stream with XML data and extract pair of ID and SQL. Each pair is put into map.
	 * 
	 * @param inputStream
	 *            data in XML format
	 * @return map of IDs and SQLs
	 */
	Map<String, String> parseSQLFile(final InputStream inputStream, final String schema) {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			final Document dom = dbf.newDocumentBuilder().parse(inputStream);
			return parseDocument(dom, schema);
		} catch (final ParserConfigurationException e) {
			LicenseLogger.getInstance().error(LicenseSQLParser.class.getName(), "parseSQLFile", e.getMessage());
		} catch (final SAXException e) {
			LicenseLogger.getInstance().error(LicenseSQLParser.class.getName(), "parseSQLFile", e.getMessage());
		} catch (final IOException e) {
			LicenseLogger.getInstance().error(LicenseSQLParser.class.getName(), "parseSQLFile", e.getMessage());
		}

		return Collections.emptyMap();
	}

	private Map<String, String> parseDocument(final Document dom, final String schema) {
		final Map<String, String> cache = new HashMap<String, String>();
		final NodeList nodeList = dom.getElementsByTagName("Statement");
		if (nodeList != null) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				final Element element = (Element) nodeList.item(i);

				final String id = element.getAttribute("id");
				final String sql = getSQL(element).replaceAll("\\$SCHEMA\\$", schema);

				cache.put(id, sql);
			}
		}
		return cache;
	}

	/**
	 * Returns text content of <code>SQL</code> element under specified node. For:
	 * 
	 * <pre>
	 * &lt;Statement&gt;&lt;SQL&gt;
	 * INSERT INTO STOREDLICENSE (serialnumber,orderid,licensecode,licensename) VALUES (?,?,?,?);
	 * &lt;/SQL&gt;&lt;/Statement&gt;
	 * </pre>
	 * 
	 * method returns:
	 * 
	 * <pre>
	 * INSERT INTO STOREDLICENSE (serialnumber,orderid,licensecode,licensename) VALUES (?,?,?,?);
	 * </pre>
	 * 
	 * @param element
	 * @return
	 */
	private String getSQL(final Element element) {
		final NodeList nodeList = element.getElementsByTagName("SQL");
		if ((nodeList != null) && (nodeList.getLength() > 0)) {
			final Element subElement = (Element) nodeList.item(0);
			return subElement.getFirstChild().getNodeValue().trim();
		}
		return null;
	}
}