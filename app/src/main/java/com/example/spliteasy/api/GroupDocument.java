package com.example.spliteasy.api;

public class GroupDocument {
    private Long id;
    private Long groupId;
    private Long uploaderUserId;
    private String uploaderName;
    private String fileName;
    private String fileType;
    private String uploadedAt;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public Long getUploaderUserId() { return uploaderUserId; }
    public String getUploaderName() { return uploaderName; }
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public String getUploadedAt() { return uploadedAt != null ? uploadedAt.replace("T", " ") : ""; }
}
