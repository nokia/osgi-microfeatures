/**
 * 
 */
package com.alcatel_lucent.ha.services;

import java.util.List;
import java.util.Map;


/**
 * 
 * A bucket in which one or many flattable object can register to. All the
 * contained registered flattable are passivated with the same operation
 * through a common representation that can be a map or a other cluster
 * structure
 */
public interface HAContext extends FlattableVisitor {
    String id();
    /**
     * register a flattable object to track its activity within the bucket
     * @param a flattable object
     */
    void register(Flattable o);
    
    void registerRoot(int key);
    /**
     * unregister a flattable object from the bucket
     * @param a flattable object
     * @return the number of flattable remaining in the bucket
     */
    int unregister(Flattable o);
    /**
     * trigger the passivation of the bucket
     * @return the resulting map of passivated attributes
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchFieldException
     */
    Map<String, Object> passivate() throws IllegalArgumentException,
            IllegalAccessException, SecurityException, NoSuchFieldException;
    
    /**
     *  for testing purpose only
     * @param map
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchFieldException
     */
    Map<String, Object> passivate(Map<String, Object> map) throws IllegalArgumentException,
    IllegalAccessException, SecurityException, NoSuchFieldException;

    List<Flattable> content();
    void destroy();
}
