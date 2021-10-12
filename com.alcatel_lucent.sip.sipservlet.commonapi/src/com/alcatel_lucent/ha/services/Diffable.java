// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package com.alcatel_lucent.ha.services;

/**
 * 
 * A object that is able to collect its changing fields.
 * 
 */
public interface Diffable {
    /**
     * 
     * A specific list that tracks remove and modification on element. Modifications are tracked by adding , Removal by removing. A remove after a modification will remove the modification. a modifcation after a remove will remove the removal.
     *
     */
   /** interface List extends java.util.List<String>{
        java.util. List<String> mod();
        java.util.List<String> del();
    }*/
    java.util.Set<String> diff(boolean create);
}