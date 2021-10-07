package com.nextenso.proxylet.diameter.client;

import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.DiameterSession;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Generic Diameter Client.
 * <p/>
 * It implements no specific application. An application-specific client must be
 * written on top of it. <br/>
 * It is only in charge of performing requests and maintains no applicative
 * state.
 * <p/>
 * An instance can be obtained via DiameterClientFactory.
 */
@ProviderType
public interface DiameterClient {

	/**
	 * The type of a DiameterClient which only handles Accounting requests.
	 */
	public static final int TYPE_ACCT = 1;
	/**
	 * The type of a DiameterClient which only handles Authorization requests.
	 */
	public static final int TYPE_AUTH = 2;
	/**
	 * The type of a DiameterClient which handles both Accounting and
	 * Authorization requests.
	 */
	public static final int TYPE_ALL = TYPE_ACCT | TYPE_AUTH;

	/**
	 * Gets the destination host.
	 * 
	 * @return The destination host.
	 */
	public String getDestinationHost();

	/**
	 * Gets the destination realm
	 * 
	 * @return The destination realm.
	 */
	public String getDestinationRealm();

	/**
	 * Gets the Diameter Session.
	 * 
	 * @return The Diameter Session, or <code>null</code> if the client is
	 *         stateless.
	 */
	public DiameterSession getDiameterSession();

	/**
	 * Gets the Diameter application.
	 * 
	 * @return The Diameter application.
	 */
	public long getDiameterApplication();

	/**
	 * Gets the Diameter application vendorId.
	 * 
	 * @return the Diameter application vendorId.
	 */
	public long getDiameterApplicationVendorId();

	/**
	 * Instantiates a new authorization request. <br/>
	 * Some default AVPs are put in the request, as: Session-Id (if stateful),
	 * Destination-Host, Destination-Realm, Auth-Application-Id,
	 * Vendor-Specific-Application-Id. <br>
	 * <b>This method throws an UnsupportedOperationException if the client type
	 * does not include TYPE_AUTH</b>
	 * 
	 * @param commandCode The request code.
	 * @param proxiable The Proxy flag.
	 * @return A new request.
	 */
	public DiameterClientRequest newAuthRequest(int commandCode, boolean proxiable);

	/**
	 * Instantiates a new accounting request. <br/>
	 * Some default AVPs are put in the request, as: Session-Id (if stateful),
	 * Destination-Host, Destination-Realm, Acct-Application-Id,
	 * Vendor-Specific-Application-Id. <br>
	 * <b>This method throws an UnsupportedOperationException if the client type
	 * does not include TYPE_ACCT</b>
	 * 
	 * @param commandCode The request code.
	 * @param proxiable The Proxy flag.
	 * @return A new request.
	 */
	public DiameterClientRequest newAcctRequest(int commandCode, boolean proxiable);

	/**
	 * Instantiates a new request. <br/>
	 * No AVPs are put in the request. <br>
	 * No check is performed on the client type.
	 * 
	 * @param commandCode The request code.
	 * @param proxiable The Proxy flag.
	 * @return A new request.
	 */
	public DiameterClientRequest newRequest(int commandCode, boolean proxiable);
	
	/**
	 * Fill the system AVPs of the given request
	 * The following AVP will be filled using the client informations:
	 * Session-Id (if stateful),
	 * Destination-Host, Destination-Realm, Auth-Application-Id,
	 * Acct-Application-Id <br>
	 * @param the Diameter Message to fill
	 */
	public void fillMessage(DiameterMessage msg);

	/**
	 * Releases the client, destroys the Diameter Session and removes
	 * the DiameterRequestListener.
	 */
	public void close();

	/**
	 * Sets an incoming request listener. <br/>
	 * It indicates that the diameter container should be prepared to receive
	 * incoming requests (like a notification). <br/>
	 * It should be set prior to executing the first request. <br/>
	 * Incoming requests do not go through the proxylets. <br/>
	 * <b>Note: it can only be called on a stateful client, and if the session has
	 * no timeout defined, an explicit destroy() on the session must be called to
	 * garbage collect it.</b>
	 * 
	 * @param listener The listener.
	 */
	public void setDiameterRequestListener(DiameterRequestListener listener);

}
