package com.alcatel.as.service.metatype;

/** 
 * The MetatypeParser service is used to load and parse
 * configuration and administration meta data from bundles.
 */
public interface MetatypeParser {
  /** 
   * Loads configuration and administration meta data.
   * @param source One bundle which may contain meta data descriptors, 
   * either as a java.net.URL to the bundle, or a Bundle instance, 
   * depending on the context of use.
   * @return the meta data parsed and wrapped in a MetaData instance.
   */
  MetaData loadMetadata(Object source) throws Exception ;
  
  /** 
   * Loads configuration and administration meta data.
   * @param source One bundle which may contain meta data descriptors, 
   * either as a java.net.URL to the bundle, or a Bundle instance, 
   * depending on the context of use.
   * @param useCache false if all mbeans must not be cached (it means that
   * all beans are re parsed during each method call to loadMetadata).
   * @return the meta data parsed and wrapped in a MetaData instance.
   */ 
  MetaData loadMetadata(Object source, boolean useCache) throws Exception ;
}
