package com.nokia.licensing.interfaces;

import java.util.HashMap;


public interface CredentialAccess {

    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";

    /**
     * This interface is used to get the UserName and Password of the database used. The different applications can
     * implement this interface and return their credentials.
     *
     * @return HashMap<String, String> where key is the userName and value is the password.
     */
    public HashMap<String, String> getCredentials();
}
