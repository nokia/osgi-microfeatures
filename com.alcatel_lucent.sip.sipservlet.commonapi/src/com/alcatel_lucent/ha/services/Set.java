package com.alcatel_lucent.ha.services;

 interface Set extends java.util.Set<String> {
    java.util.Set<String> mod();
    java.util.Set<String> del();
}
