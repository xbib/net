package org.xbib.net;

public final class UserDetails {

    private String name;

    private String userId;

    private String effectiveUserId;

    public UserDetails() {
        this.name = "";
        this.userId = "";
        this.effectiveUserId = "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setEffectiveUserId(String effectiveUserId) {
        this.effectiveUserId = effectiveUserId;
    }

    public String getEffectiveUserId() {
        return effectiveUserId;
    }

    @Override
    public String toString() {
        return "UserDetails{" + "name='" + name + ',' + ",userId='" + userId + ",effectiveUserId=" + effectiveUserId + '}';
    }
}
