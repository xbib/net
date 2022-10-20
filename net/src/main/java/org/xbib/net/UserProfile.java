package org.xbib.net;

import java.util.List;

public interface UserProfile {

    boolean isLoggedIn();

    void setUserId(String uid);

    String getUserId();

    void setEffectiveUserId(String eid);

    String getEffectiveUserId();

    void setName(String name);

    String getName();

    void addRole(String role);

    void addEffectiveRole(String role);

    List<String> getRoles();

    List<String> getEffectiveRoles();

    boolean hasRole(String role);

    boolean hasEffectiveRole(String role);

    boolean hasAnyRole(String[] expectedRoles);

    boolean hasAnyEffectiveRole(String[] expectedRoles);

    boolean hasAllRoles(String[] expectedRoles);

    boolean hasAllEffectiveRoles(String[] expectedRoles);

    void addPermission(String permission);

    void removePermission(String permission);

    List<String> getPermissions();

    List<String> getEffectivePermissions();

    Attributes attributes();

    Attributes effectiveAttributes();

    void setRemembered(boolean remembered);

    boolean isRemembered();

    boolean hasAccess(String requireAnyRole, String requireAllRoles);

}
