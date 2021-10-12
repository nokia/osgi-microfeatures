// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.ha.services;

 interface Set extends java.util.Set<String> {
    java.util.Set<String> mod();
    java.util.Set<String> del();
}
