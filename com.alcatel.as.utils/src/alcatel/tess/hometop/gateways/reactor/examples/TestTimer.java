// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.examples;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

public class TestTimer {
  static Logger _logger = Logger.getLogger("test");
  static Logger _loggerReactor = Logger.getLogger("reactor");
  
  public static void main(String args[]) throws IOException {
    ReactorProvider factory = ReactorProvider.provider();
    Reactor reactor = factory.newReactor("reactor", false, _loggerReactor);
    
    _logger.warn("scheduling task in 1 sec ...");
    reactor.schedule(new Runnable() {
      @Override
      public void run() {
        _logger.warn("timer1");
      }
    }, 1, TimeUnit.SECONDS);
    _logger.warn("scheduling periodic task in 2 sec ...");
    
    final Future<?> f = reactor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        _logger.warn("fixed rate timer");
      }
    }, 2, 1, TimeUnit.SECONDS);
    
    reactor.schedule(new Runnable() {
      @Override
      public void run() {
        _logger.warn("cancelling periodic timer: " + f.cancel(false));
        _logger.warn("re-cancelling: " + f.cancel(false));
      }
    }, 5, TimeUnit.SECONDS);
    
    reactor.run();
  }
}
