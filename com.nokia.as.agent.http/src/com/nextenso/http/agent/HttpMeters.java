package com.nextenso.http.agent;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.ValueSupplier;

public class HttpMeters {
  public final static String MONITORABLE_DESC = "HTTP Agent Metrics";
  private final static Logger LOGGER = Logger.getLogger("agent.http.monitorable");

  protected MeteringService metering;
  protected SimpleMonitorable mon;
  
  private Meter processedRequests;
  private Meter abortedRequests;
  private Meter pendingRequests;
  private Meter channelConnectedServer, channelConnectedClient, channelClosed;
  private Meter webSockets;
  private final Meter _h2clientTimeouts;
  private final Meter _h2clientProtocolErrors;
  private final Meter _h2clientException;
  private final Meter _h2clientResponses;

  void addValueSuppliedMeters(ValueSupplier clientsSupplier, ValueSupplier socketsSupplier) {
	mon.createValueSuppliedMeter(metering, "clients", clientsSupplier);
    mon.createValueSuppliedMeter(metering, "channels.opened", socketsSupplier);
    mon.updated();
  }
  
  public HttpMeters(MeteringService metering, SimpleMonitorable mon) {
    this.metering = metering;
    this.mon = mon;
    processedRequests = mon.createIncrementalMeter(metering, "requests.processed", null);
    abortedRequests = mon.createIncrementalMeter(metering, "requests.aborted", null);
    pendingRequests = mon.createIncrementalMeter(metering, "requests.pending", null);
    Meter connected = mon.createIncrementalMeter(metering, "channels.connected", null);
    channelConnectedServer = mon.createIncrementalMeter(metering, "channels.connected.server", connected);
    channelConnectedClient = mon.createIncrementalMeter(metering, "channels.connected.client", connected);
    channelClosed = mon.createIncrementalMeter(metering, "channels.closed", null);
    webSockets = mon.createIncrementalMeter(metering, "websockets", null);
    _h2clientTimeouts = mon.createIncrementalMeter(metering, "h2client.errors.timeout", null);
    _h2clientProtocolErrors = mon.createIncrementalMeter(metering, "h2client.errors.protocol", null);
    _h2clientException = mon.createIncrementalMeter(metering, "h2client.errors.exception", null);
    _h2clientResponses = mon.createIncrementalMeter(metering, "h2client.responses", null);
  }
  
  public void incHttpClientTimeouts() {
	_h2clientTimeouts.inc(1); 
  }
  
  public void incHttpClientProtocolErrors() {
	_h2clientProtocolErrors.inc(1); 
  }
  
  public void incHttpClientExceptions() {
	_h2clientException.inc(1); 
  }

  public void incHttpClientResponses() {
	  _h2clientResponses.inc(1); 
  }

  public void incProcessedRequests() {
    processedRequests.inc(1);
  }

  public long getProcessedRequests() {
    return processedRequests.getValue();
  }

  public void incAbortedRequests() {
    abortedRequests.inc(1);
  }

  public long getAbortedRequests() {
    return abortedRequests.getValue();
  }

  public void incPendingRequests() {
    pendingRequests.inc(1);
  }

  public void decPendingRequests() {
    pendingRequests.dec(1);
  }

  public long getPendingRequests() {
    return pendingRequests.getValue();
  }
  
  public void channelConnected(boolean client) {
    if (client)
      channelConnectedClient.inc(1);
    else
      channelConnectedServer.inc(1);
  }
  
  public void channelClosed() {
    channelClosed.inc(1);
  }
  
  public void incWebSockets() {
    webSockets.inc(1);
  }
  
}
