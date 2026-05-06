package com.connecthub.room.dto;

public class RoomDirectoryEntry {
    private Integer id;
    private String name;
    private String description;
    private Integer createdBy;
    private Boolean isJoined;
    private String joinStatus;
    private Integer membersCount;

    public RoomDirectoryEntry(Integer id, String name, String description, Integer createdBy,
                              Boolean isJoined, String joinStatus, Integer membersCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.isJoined = isJoined;
        this.joinStatus = joinStatus;
        this.membersCount = membersCount;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getCreatedBy() { return createdBy; }
    public Boolean getIsJoined() { return isJoined; }
    public String getJoinStatus() { return joinStatus; }
    public Integer getMembersCount() { return membersCount; }
}
