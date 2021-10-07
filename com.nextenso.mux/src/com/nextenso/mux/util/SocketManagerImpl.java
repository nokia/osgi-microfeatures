package com.nextenso.mux.util;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nextenso.mux.socket.Socket;
import com.nextenso.mux.socket.SocketManager;

public class SocketManagerImpl implements SocketManager {
  private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Socket>> _tables = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Socket>>();
  
  public SocketManagerImpl() {
    _tables.put(Socket.TYPE_UDP, new ConcurrentHashMap<Integer, Socket>());
    _tables.put(Socket.TYPE_TCP, new ConcurrentHashMap<Integer, Socket>());
    _tables.put(Socket.TYPE_TCP_SERVER, new ConcurrentHashMap<Integer, Socket>());
    _tables.put(Socket.TYPE_SCTP, new ConcurrentHashMap<Integer, Socket>());
    _tables.put(Socket.TYPE_SCTP_SERVER, new ConcurrentHashMap<Integer, Socket>());
  }
  
  public Socket getSocket(int type, int id) {
    ConcurrentHashMap<Integer, Socket> table = getTable(type);
    return table.get(id);
  }
  
  public void addSocket(Socket socket) {
    ConcurrentHashMap<Integer, Socket> table = getTable(socket.getType());
    table.put(socket.getSockId(), socket);
  }
  
  public Socket removeSocket(int type, int id) {
    ConcurrentHashMap<Integer, Socket> table = getTable(type);
    return table.remove(id);
  }
  
  public int getSocketsSize(int type) {
    ConcurrentHashMap<Integer, Socket> table = getTable(type);
    return table.size();
  }
  
  public int getSocketsSize() {
    int count = 0;
    for (Map.Entry<Integer, ConcurrentHashMap<Integer, Socket>> entry : _tables.entrySet()) {
      count += entry.getValue().size();
    }
    return count;
  }
  
  @SuppressWarnings("unchecked")
  public Enumeration getSockets(int type) {
    ConcurrentHashMap<Integer, Socket> table = getTable(type);
    return table.elements();
  }
  
  @SuppressWarnings("unchecked")
  public Enumeration getSockets() {
    return new Enumeration<Socket>() {
      private Enumeration<ConcurrentHashMap<Integer, Socket>> _tableEnum = _tables.elements();
      private Enumeration<Socket> _currSocketEnum;
      
      @Override
      public boolean hasMoreElements() {
        if (_currSocketEnum == null) {
          if (_tableEnum.hasMoreElements()) {
            _currSocketEnum = _tableEnum.nextElement().elements();
          } else {
            return false;
          }
        }
        
        while (!_currSocketEnum.hasMoreElements() && _tableEnum.hasMoreElements()) {
          _currSocketEnum = _tableEnum.nextElement().elements();
        }
        
        return _currSocketEnum.hasMoreElements();
      }
      
      @Override
      public Socket nextElement() {
        return _currSocketEnum.nextElement();
      }
    };
  }
  
  public void removeSockets(int type) {
    ConcurrentHashMap<Integer, Socket> table = getTable(type);
    table.clear();
  }
  
  public void removeSockets() {
    for (Map.Entry<Integer, ConcurrentHashMap<Integer, Socket>> entry : _tables.entrySet()) {
      entry.getValue().clear();
    }
  }
  
  private ConcurrentHashMap<Integer, Socket> getTable(int type) {
    ConcurrentHashMap<Integer, Socket> socketTable = _tables.get(type);
    if (socketTable == null) {
      throw new IllegalArgumentException("Invalid Socket type: " + type);
    }
    return socketTable;
  }
}
