package com.alcatel_lucent.servlet.sip.services;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 *  ExtendedService that may be use to define high availability point-cuts in a
 *  critical SipServlet code. A point cut is a place where the recovery service
 *  is called. The container has default point cut according to the SIP roles :
 *  proxy or user agent. The service may be found through the
 *  javax.servlet.ServletContext' s attribute : com.alcatel.sip.sipservlet.
 *  RecoveryService. From the service you can active or passivate thanks to the
 *  sub services PassivationService and ActivationService. By convention, the
 *  expected parameter is the SipApplicationSession object of the application.
 */
public interface RecoveryService extends Statable {
	final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("agent.sip.ha");
	final static String SESSION = "com.alcatel.as.session.distributed.session";


	/**
	 *  a abstract place where the object are passivated/recovered. Internal usage
	 *  only
	 */
	interface RecoveryRepository {
		/**
		 *  Gets the output attribute of the RecoveryRepository object
		 *
		 *@return    The output value
		 */
		ObjectOutput getOutput();


		/**
		 *  Gets the input attribute of the RecoveryRepository object
		 *
		 *@return    The input value
		 */
		ObjectInput getInput();

	}
	/**
		an asynchronous api to be callback when asynchronous passivation occurs
		@param boolean true if passivation is ok, false else
	*/
	interface PassivationCallback {
		void passivated(boolean ok);
	}
    interface ActivationCallback {
        void activated(boolean ok);
    }
	/**  a service to backup critical data into the RecoveryRepository */
	interface PassivationService {
		/**
		 *  method to call at point-cuts in code to backup critical data into an
		 *  application.
		 *
		 *@param  o                the object to backup, must be a
		 *      SipApplicationSession instance
		 *@exception  IOException  when backup fails
		 */
		void passivate(Object o) throws IOException;


		/**
		 *  method to call 
		 *
		 *@param  o                Description of the Parameter
		 *@exception  IOException  Description of the Exception
		 */
		void passivate(Object o, PassivationCallback cb) throws IOException;


		/**
		 *  method to call to remove the critical data from the share memory. This
		 *  method is important for long-live "proactive" application to be cancelled.
		 *  Without calling it, if a failure occurs, some "outdated" treatment could
		 *  be performed.
		 *
		 *@param  o                the same object passed to the passivate method
		 *@exception  IOException  when remove fails. Note that critical logs are
		 *      producted here.
		 */
		void unpassivate(Object o) throws IOException;
      
	}


	/**
	 *  a service to activate critical data from the RecoveryRepository. Generally
	 *  the container is in charge of recovering the data related to
	 *  SipApplicationSession/Session. Usage of this service is reserved to very
	 *  specific application.
	 */
	interface ActivationService {
		/**
		 *  recover the object from the repository
		 *
		 *@param  o                the key to retrieve the object, i.e the
		 *      SipApplicationSession's Id
		 *@return                  the retrieved object(s).
		 *@exception  IOException  when recover fails
		 */
        void activate(Object o, ActivationCallback cb);
	}



	/**
	 *  Gets the passivationService attribute of the RecoveryService object
	 *
	 *@return    The passivationService value
	 */
	PassivationService getPassivationService();


	/**
	 *  creates the ActivationService for the repository
	 *
	 *@return    the Service
	 */
	ActivationService getActivationService();
	

}

