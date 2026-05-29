package com.example.heritage_sharing_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "resources_tags")
@IdClass(ResourceTagId.class)
public class ResourceTag {
    @Id
    @Column(name = "resource_id")
    private Long resourceId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    public ResourceTag() {}

    public ResourceTag(Long resourceId, Long tagId) {
        this.resourceId = resourceId;
        this.tagId = tagId;
    }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
}
