package com.example.spliteasy.api;

import java.util.List;

public class CreateGroupRequest {
    private String groupName;
    private Long creatorUserId;
    private List<Long> memberUserIds;

    public CreateGroupRequest(String groupName, Long creatorUserId, List<Long> memberUserIds) {
        this.groupName = groupName;
        this.creatorUserId = creatorUserId;
        this.memberUserIds = memberUserIds;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public List<Long> getMemberUserIds() {
        return memberUserIds;
    }
}
