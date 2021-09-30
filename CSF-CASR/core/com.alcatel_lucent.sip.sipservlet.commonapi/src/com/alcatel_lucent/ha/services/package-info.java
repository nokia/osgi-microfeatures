/**
**
 * <p>
 * defines a  framework for performing efficient service for call preservation when call infers  multiple applications or multiple protocols (said convergent as http with sip)
 * <p>Goals
 * <br>The framework aims to address  various strategies :
 * <li>binding an unique cluster replication location by call context
 * <li>detecting and writing only differences
 * <li>limit the network bandwidth
 * <li>limit the number of execution point-cuts
 * <li>limit the  memory usage
 * <ul>
 * <p>Principles
 *<br> The framework provides a bucket named HAContext that is the unit of replication over a cluster.
 *This context is provided by message to every application inferring during a "call".  Each persistent objects (dialogs etc...)
 *register to this HAcontext. In order to proceed, the object must be a Flattable Object.
 *<br>A flattable object is made of FlatField and extends the Flattable interfaces.
 *<br>Every flat field can be written into a map.
 *<br>When a flattable object refers to another flattable object, the framework  will write it into the same map
 *<p> When passivating, the flat fields are written through a Map paradigm towards a cluster entity. Typically
 * a remote distributed session
 * <br>By definition a flattable object extends the Diffeable interface and is able to provide the differences
 * after the object has been written once. The framework will only then write the differences to the remote
 * entity.
 * </p>
 * 
 */
package com.alcatel_lucent.ha.services;
