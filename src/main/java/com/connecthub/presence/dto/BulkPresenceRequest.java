package com.connecthub.presence.dto;

import java.util.List;

public class BulkPresenceRequest {

    private List<Integer> userIds;

    public List<Integer> getUserIds() { return userIds; }
    public void setUserIds(List<Integer> userIds) { this.userIds = userIds; }
}
