package com.alcatel.as.service.metatype;

import org.json.JSONObject;

/**
 * Serialization codec for PropertiesDescriptor, InstanceProperties, PropertyDescriptor or InstanceProperty
 */
public interface PropertyFactory {
  /**
   * builds either PropertiesDescriptor, InstanceProperties, PropertyDescriptor or InstanceProperty
   * from a json serialization
   */
  Object loadJson(String json) throws IllegalArgumentException;

  /**
   * builds either PropertiesDescriptor, InstanceProperties, PropertyDescriptor or InstanceProperty
   * from a json serialization
   */
  Object loadJson(JSONObject json) throws IllegalArgumentException;

  /**
   * encodes the argument to a JSONObject
   * @param either a PropertiesDescriptor, InstanceProperties, PropertyDescriptor or InstanceProperty
   */
  JSONObject toJSONObject(Object property) throws IllegalArgumentException;

  /**
   * encodes the argument to a JSON string representation.
   * @param either a PropertiesDescriptor, InstanceProperties, PropertyDescriptor or InstanceProperty
   */
  String toJson(Object property) throws IllegalArgumentException;
}

