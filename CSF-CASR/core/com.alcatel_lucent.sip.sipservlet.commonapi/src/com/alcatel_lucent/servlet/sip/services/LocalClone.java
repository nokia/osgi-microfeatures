package com.alcatel_lucent.servlet.sip.services;

/**
 * Basic interface uses when cloning local messages in a cohosting server.
 * localclone() perform a logic than differs from clone() 
 */
public interface LocalClone extends Cloneable {
    Object localclone();

}
