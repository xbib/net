package org.xbib.net.security.jaas;

import java.security.Principal;
 
public class PlainUserPrincipal implements Principal {
 
    String UserName;
     
    public PlainUserPrincipal(String name) {
        UserName = name;
    }
    public String getName() {
        return UserName;
    }
     
    public String toString() {
        return ("UserPrincipal: " + UserName);
    }  
 
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }  
        if (obj instanceof PlainUserPrincipal) {
            PlainUserPrincipal other = (PlainUserPrincipal) obj;
            return UserName.equals(other.UserName);
        }  
        return false;
    }  
 
    public int hashCode() {
        return UserName.hashCode();
    }  
}
