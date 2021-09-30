package com.alcatel.as.service.appmbeans.impl;

import static com.alcatel.as.util.config.ConfigConstants.HOST_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.nextenso.proxylet.mgmt.Monitor.LEVEL_CLEAR;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanNotificationInfo;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.StringCaseHashtable;

import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel.as.service.reporter.api.ExtendedInfo;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

/**
 *   Listens to Jmx Notifications emitted from Mbeans, and fowards them to the AlarmService
 */
@Config(name="mbeanReporter", rootSnmpName="alcatel.srd.a5350.CalloutAgent", rootOid={637, 71, 6, 110}, section="JMX")
public class MBeanReporter {
  @BooleanProperty(title="Append JMX source Notification to Alarm instance name", help="When set to TRUE, any Jmx source notification (if not null) is appended to the generated alarm instance name.", oid=105, snmpName="AppendJMXSourceNotificationToAlarmInstanceName", required=true, dynamic=true, defval=false)
  public static final String APPEND_JMX_SOURCE_NOTIF = "agent.appendJmxSourceNotif";
  @StringProperty(title="Alarm send window", help="You can reduce the number of alarm sent within a range of milliseconds. This can be used to avoid flooding the jmx gateway with alarm repetitions. If you leave this property to '0', it means that all alarms will be fired, including alarm repetitions. A positive value represents a range (in millis) in which alarms of the same code can't be repeted.", oid=133, snmpName="AlarmSendWindow", required=false, dynamic=false, defval="0")

  public static final String ALARM_SEND_WINDOW = "agent.alarmSendWindow";

  /**
   * Start the proxylet reporter. We'll initialize a Jmx Notification Listener, 
   * which is used to forward Jmx notifications to the alarm service.
   */
  public void start() throws Exception {
    this.server = findMBeanServer (_agentName) ;
    _appendJMXSourceNotif = ConfigHelper.getBoolean(_agentConf, APPEND_JMX_SOURCE_NOTIF, false);
    _alarmSendWindow = ConfigHelper.getLong(_agentConf, ALARM_SEND_WINDOW, 0L);

    // Listen to Mbean registration events.
    _mbeanRegistrationListener = new MBeanRegistrationListener();
    server.addNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), 
        _mbeanRegistrationListener, null, null);
  }

  public void stop() {
    try {
      server.removeNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), 
          _mbeanRegistrationListener);
    } catch (Throwable t) {
      _logger.error("Could not stop MBeanReporter service", t);
    }
  }

  /**
   * Handle a property changed event.
   */
  @SuppressWarnings("unchecked")
  public void bindSystemConf(Dictionary d) {
    if (_hostName == null) {
      _hostName = ConfigHelper.getString(d, HOST_NAME);
    }
    if (_agentName == null) {
      _agentName = ConfigHelper.getString(d, INSTANCE_NAME);
    }
  }

  private MBeanServer findMBeanServer(String agentid) {
    return java.lang.management.ManagementFactory.getPlatformMBeanServer();
  }

  private static Logger _logger = Logger.getLogger("as.service.management.MBeanReporter");
  private MBeanServer server;
  protected AlarmService _alarmService; // injected by Activator

  @SuppressWarnings("unchecked")
  private Dictionary _agentConf; // injected by DM
  private volatile String _agentName;
  private volatile String _hostName;
  private MBeanRegistrationListener _mbeanRegistrationListener;

  /** List of mbeans sending alarms. (Key=ObjectName, Value=MbeanObserver) */
  @SuppressWarnings("unchecked")
  private Hashtable<ObjectName, MBeanObserver> _mbeanObservers = new Hashtable<ObjectName, MBeanObserver>();

  /** When set to TRUE, we append any JMX source notif (if not null) to our Alarm instance name. */
  private boolean _appendJMXSourceNotif;

  /** Map used to detect alarm repetition. */
  Map<String, AlarmRepetition> _repetitions = new HashMap<String, AlarmRepetition>();

  /** Max alarm send window, in millis. We won't send more than one alarm of the same code in the specified time */
  private long _alarmSendWindow = 0L;

  /** Data structure used to detect alarm repetiion. */
  static class AlarmRepetition {
    long _sendTime;
    boolean _repetitionLogged;

    AlarmRepetition(long sendTime) {
      _sendTime = sendTime;
    }
  }

  /**
   * Class used to map a notification (from the mbean-descriptor.xml file) with A5350 specific
   * alarm informations (alarm code + alarm level).
   */
  private static class AlarmDescriptor {
    private volatile String _alarmType;
    private volatile int _alarmCode = 50; // Legacy
    private volatile int _alarmSeverity = 6; // "normal" (see javadoc of ModelMBeanNotificationInfo).

    AlarmDescriptor(String type, Object code, Object severity) {
      _alarmType = type;
      if (code == null) {
        _logger.warn("Missing alarmCode in mbean-descriptor.xml for notification " 
            + type + ". Will use Legacy alarm code by default");
      } else {
        try {
          _alarmCode = Integer.parseInt(code.toString());
        } catch (NumberFormatException e) {
          _logger.warn("Invalid alarmCode: " + code 
              + " in mbean-descriptor.xml for notification " + type + ". Will use Legacy alarm code by default");
        }
      }
      if (severity == null) {
        _logger.warn("Missing severiry in mbean-descriptor.xml for notification " 
            + type + ". Will use 6 (normal) by default");
      } else {
        try {
          _alarmSeverity = Integer.parseInt(severity.toString());
        } catch (NumberFormatException e) {
          _logger.warn("Invalid alarm severity: " + severity 
              + " in mbean-descriptor.xml for notification " + type + ". Will use 0 (unknown) by default");
        }
      }
    }

    private String getAlarmType() {
      return _alarmType;
    }

    private int getAlarmCode() {
      return _alarmCode;
    }

    private int getAlarmSeverity() {
      return _alarmSeverity;
    }
        
    public String toString() {
      return "alarmType=" + _alarmType + ", alarmCode=" + _alarmCode + ", alarmSeverity=" + _alarmSeverity;
    }
  }

  /**
   * This listener is used to listen to MBean registration. When a MBean is registered into our
   * MBean server, we'll register our MBeanObserver into it, in order to catch MBean Notifications
   */
  public class MBeanRegistrationListener implements NotificationListener {

    public MBeanRegistrationListener() {
    }

    @SuppressWarnings("unchecked")
    public void handleNotification(Notification event, Object handback) {
      if (!(event instanceof MBeanServerNotification)) {
        _logger.debug("MBeanRegistrationListener: ignoring registration event " + event);
        return;
      }
      try {
        MBeanServerNotification msn = (MBeanServerNotification) event;
        ObjectName oname = msn.getMBeanName();
        if (_logger.isDebugEnabled()) {
          _logger.debug("MBeanRegistrationListener: received event " + event + " for mbean " + oname);
        }
        if (msn.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
          MBeanInfo mi = server.getMBeanInfo(oname);
          if (!(mi instanceof ModelMBeanInfo)) {
            if (_logger.isDebugEnabled()) {
              _logger.debug("MBeanObserver: ignoring mbean " + oname 
                  + ": not a ModelMBeanInfo (" + mi.getClass() + ")");
            }
            return;
          }
          if (_logger.isDebugEnabled()) {
            _logger.debug("MBeanObserver: registering notification listener in mbean: " + oname);
          }
          if (!ProxyletObjectName.isPlatformObjectName(oname)) {
            if (_logger.isInfoEnabled()) {
              _logger.info("Ignoring Mbean " + oname);
            }
            return;
          }

          MBeanObserver alarmListener = new MBeanObserver((ModelMBeanInfo) mi, oname);
          server.addNotificationListener(oname, alarmListener, null, null);
          synchronized (_mbeanObservers) {
            _mbeanObservers.put(oname, alarmListener);
          }
        } else if (msn.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
          MBeanObserver mo = null;
          synchronized (_mbeanObservers) {
            mo = (MBeanObserver) _mbeanObservers.remove(oname);
          }
          if (mo != null) {
            if (_logger.isDebugEnabled()) 
              _logger.debug("MBeanObserver: unregistered notification listener for mbean: " + oname);
            }
          } else {
            _logger.warn("MBeanRegistrationListener: got unexpected MbeanServerNotification type: " + msn.getType());
          }
        } catch (Throwable t) {
          _logger.warn("MBeanRegistrationListener: got unexpected exception while handling" 
              + " mbean server registration event: " + event, t);
        }
      }
    }

  /**
   * The MBeanObserver class listens to notification emmited from MBeans and redirects them to the alarm service.
   */
  public class MBeanObserver implements NotificationListener {
    private StringCaseHashtable _notifications;
    private ObjectName _mbName;
    private ProxyletObjectName _pxletOName;
    private MBeanAttributeInfo[] _attInfos;

    /**
     * Creates an Alarms Listener: we'll register to all known proxylet/sipservlet MBeans.
     */
    public MBeanObserver(ModelMBeanInfo mmi, ObjectName mbName) throws Exception {
      _notifications = new StringCaseHashtable();
      _mbName = mbName;
      _pxletOName = new ProxyletObjectName(MBeanReporter.this._hostName, mbName);
      _attInfos = mmi.getAttributes();
      MBeanNotificationInfo[] mnis = mmi.getNotifications();
      for (int i = 0; i < mnis.length; i++) {
        ModelMBeanNotificationInfo mmni = mmi.getNotification(mnis[i].getName());
        Descriptor d = mmni.getDescriptor();
        AlarmDescriptor ad = new AlarmDescriptor(mnis[i].getName(), 
            d.getFieldValue("messageID"), d.getFieldValue("severity"));
        if (_logger.isInfoEnabled()) {
          _logger.info("Registered alarm descr: " + ad);
        }
        _notifications.putObject(ad.getAlarmType(), ad);
      }
    }

    public ProxyletObjectName getProxyletObjectName() {
      return _pxletOName;
    }

    public void handleNotification(Notification event, Object handback) {
      _logger.info("Received notification: type=" + event.getType() + ", time=" 
          + new java.util.Date(event.getTimeStamp()) + ", message=" + event.getMessage());

      // Lookup in our ModelMBeanInfo the ModelMBeanNotificationInfo corresponding to the event type.
      AlarmDescriptor ad = (AlarmDescriptor) _notifications.getObject(event.getType());
      if (ad == null) {
        _logger.warn("ProxyletReporter.MBeanObserver: could not handle unknown notification event: " + event);
        return;
      }

      // Detect alarm repetition and don't store it in order to avoid flooding jmxgateway
      if (detectAlarmRepetition(event)) {
        return;
      }
      String instance = _mbName.getKeyProperty("instance"); //see ApplicationObjectNameFactory
      if (_appendJMXSourceNotif && event.getSource() != null) {
        // check if the source name string contains proper charset (we only accept
        // [a-zAZ09]).
        String source = event.getSource().toString();
        if (source.indexOf(";") != -1) {
          throw new IllegalArgumentException("Invalid Jmx Notification Source parameter: \"" 
              + source + "\": " + "The source contains the forbidden '; character");
        }
        instance += ("/" + event.getSource().toString());
      }

      if (_logger.isDebugEnabled()) {
        _logger.debug("forwarding jmx notification: <" + event + "> : instance=" + instance 
            + ", alarmCode=" + ad.getAlarmCode() + ", severity=" + ad.getAlarmSeverity() 
            + ", message=" + event.getMessage());
      }
      int level = ad.getAlarmSeverity();
      Object o = event.getUserData() ;
      ExtendedInfo extendedInfo = null ;
      String user1 = null ;
      String user2 = null ;
      if (o != null) {
        if (o instanceof ExtendedInfo) {
          extendedInfo = (ExtendedInfo) o ;
        } else if (o instanceof String) {
          String[] arr = ((String) o).split ("#") ;
          if (arr.length > 0) {
            user1 = arr[0] ;
          }
          if (arr.length > 1) {
            user2 = arr[1] ;
          }
        }
      }
      if (level == 6 /* cleared */) {
        if (_logger.isDebugEnabled()) {
          _logger.debug("Clearing alarm code: " + ad.getAlarmCode());
        }
        if (extendedInfo == null) {
          MBeanReporter.this._alarmService.clearAlarm(instance, ad.getAlarmCode(), event.getMessage(), user1, user2);
        } else {
          MBeanReporter.this._alarmService.clearAlarm(instance, ad.getAlarmCode(), event.getMessage(), extendedInfo);
        }
      } else {
        if (_logger.isDebugEnabled()) {
          _logger.debug("Sending alarm: code=" + ad.getAlarmCode() + ", level=" + level);
        }
        if (extendedInfo == null) {
          MBeanReporter.this._alarmService.sendAlarm(instance, ad.getAlarmCode(), event.getMessage(), user1, user2);
        } else {
          MBeanReporter.this._alarmService.sendAlarm(instance, ad.getAlarmCode(), event.getMessage(), extendedInfo);
        }
      }
    }
  }

  public MBeanServer getServer() {
    return server;
  }

  private synchronized boolean detectAlarmRepetition(Notification alarm) {
    if (_alarmSendWindow == 0L) {
      return false;
    }
    Long now = Long.valueOf(System.currentTimeMillis());
    AlarmRepetition repetition = _repetitions.get(alarm.getType());
    if (repetition == null) {
      _repetitions.put(alarm.getType(), new AlarmRepetition(now.longValue()));
      return false;
    }
    Object source = alarm.getSource();
    long delta = now.longValue() - repetition._sendTime;
    if (delta > _alarmSendWindow) {
      repetition._sendTime = now.longValue();
      if (repetition._repetitionLogged) {
        _logger.warn("Repeated alarm enabled: " + alarm.getType() 
            + ((source != null) ? (" (source=" + source + "): ") : ": ") + alarm.getMessage());
      }
      repetition._repetitionLogged = false;
      return false;
    } else {
      if (!repetition._repetitionLogged) {
        repetition._repetitionLogged = true;
        _logger.warn("Repetead alarm disabled: " + alarm.getType() 
            + ((source != null) ? (" (source=" + source + "): ") : ": ") + alarm.getMessage());
      }
      return true;
    }
  }
}
