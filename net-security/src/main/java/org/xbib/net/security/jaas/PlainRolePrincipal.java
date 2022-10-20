package org.xbib.net.security.jaas;

import java.security.Principal;
 
public class PlainRolePrincipal implements Principal {
 
    String roleName;
     
    public PlainRolePrincipal(String name) {
        roleName = name;
    }
    public String getName() {
        return roleName;
    }
     
    public String toString() {
        return ("RolePrincipal: " + roleName);
    }  
 
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }  
        if (obj instanceof PlainRolePrincipal) {
            PlainRolePrincipal other = (PlainRolePrincipal) obj;
            return roleName.equals(other.roleName);
        }  
        return false;
    }  
 
    public int hashCode() {
        return roleName.hashCode();
    }  
}
