// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.ioh;

public class DummyMuxConnectionMeters implements MuxConnectionMeters {

	@Override
	public void stop() {
	}

	@Override
	public void tcpSocketConnected(int sockId, boolean clientSocket) {
	}

	@Override
	public void tcpSocketClosed(int sockId) {
	}

	@Override
	public void tcpSocketAborted(int sockId) {
	}

	@Override
	public void sctpSocketConnected(int sockId, boolean fromClient) {
	}

	@Override
	public void sctpSocketClosed(int sockId) {
	}

	@Override
	public void tcpSocketListening() {
	}

	@Override
	public void tcpSocketFailedConnect() {
	}

	@Override
	public void sctpSocketFailedConnect() {
	}

	@Override
	public void sctpSocketSendFailed() {
	}

	@Override
	public void sctpPeerAddressChanged() {
	}

	@Override
	public void udpSocketBound() {
	}

	@Override
	public void udpSocketFailedBind() {
	}

	@Override
	public void udpSocketClosed() {
	}

	@Override
	public void tcpSocketData(int len) {
	}

	@Override
	public void sctpSocketData(int len) {
	}

	@Override
	public void udpSocketData(int len) {
	}

	@Override
	public void sendTcpSocketData(int remaining) {
	}

	@Override
	public void sendSctpSocketData(int remaining) {
	}

	@Override
	public void sendUdpSocketData(int remaining) {
	}

	@Override
	public void sctpSocketListening() {
	}

	@Override
	public void setMuxStarted(boolean local, boolean started) {
	}

}
