// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;


/**
 * Base class for all log events. A log event may regroups all
 * log informations (for example: string message, date, stacktrace, etc...).
 */
abstract class LogEvent {
  abstract void release();
  
  abstract Tracer getTracer();
  
  abstract int getLevel();
}
