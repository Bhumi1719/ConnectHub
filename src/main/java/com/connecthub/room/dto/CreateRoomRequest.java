package com.connecthub.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    private String name;

    private String description;

    // GROUP or DM
    private String type = "GROUP";

    @NotNull(message = "createdById is required")
    private Integer createdById;

    private String avatarUrl;
    private Boolean isPrivate = false;
    private Integer maxMembers = 100;

    // For DM — the other user's ID
    private Integer dmTargetUserId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getCreatedById() { return createdById; }
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }
    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }
    public Integer getDmTargetUserId() { return dmTargetUserId; }
    public void setDmTargetUserId(Integer dmTargetUserId) { this.dmTargetUserId = dmTargetUserId; }
}
