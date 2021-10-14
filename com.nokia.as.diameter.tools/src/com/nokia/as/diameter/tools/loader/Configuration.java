package com.nokia.as.diameter.tools.loader;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@SuppressWarnings("restriction")
@Config(section="http loader")
public interface Configuration {

    @StringProperty(help="diameter loader local address", title="from address", defval="127.0.0.1")
	default String getFrom() { return "127.0.0.1"; }
    
    @StringProperty(help="diameter loader sctp local secondary address", title="secondary", required = false)
	String getSecondary();
    
    @StringProperty(help="number of seconds before bindling to local sctp secondary addr", title="secondary schedule", defval="0")
    default String getSecondarySchedule() { return "0"; }

    @StringProperty(help="diameter loader remote address", title="to address", defval="127.0.0.1")
	default String getTo() { return "127.0.0.1"; }

    @StringProperty(help="remote sctp server port number", title="remote port", defval="3868")
    default String getPort() { return "3868"; }

    @StringProperty(help="number of diameter peer connections", title="peers", defval="1")
    default String getPeers() { return "1"; }
    
    @StringProperty(help="range of transactions per secs. Format is: tps1/duration_in_secondsS tps2/duration_in_secondsS. Example: 1/10s 100/20s. Default = unlimitted.", title="tps", defval = "10/5s,1000/10s")
	String getTps(); 

    @StringProperty(help="test duration in secs (-1=unlimitted)", title="duration", required = true, defval = "-1")
    default String getDuration() { return "-1"; }

    @StringProperty(help="diameter loader read timeout in sec", title="read timeout", defval="5")
    default String getReadTimeout() { return "5"; }

    @StringProperty(help="Using SCTP", title="sctp", defval="false")
    default String getSctp() { return "false"; }

    @StringProperty(help="diameter message bulk size", title="bulksize", defval="65536")
    default String getBulkSize() { return String.valueOf(64*1024); }

}
