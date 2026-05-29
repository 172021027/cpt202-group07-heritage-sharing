package com.example.heritage_sharing_api.entity;

import java.io.Serializable;

public class ResourceTagId implements Serializable {
    private Long resourceId;
    private Long tagId;

    public ResourceTagId() {}

    public ResourceTagId(Long resourceId, Long tagId) {
        this.resourceId = resourceId;
        this.tagId = tagId;
    }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceTagId that = (ResourceTagId) o;
        return resourceId != null && resourceId.equals(that.resourceId) && tagId != null && tagId.equals(that.tagId);
    }

    @Override
    public int hashCode() {
        return (resourceId != null ? resourceId.hashCode() : 0) * 31 + (tagId != null ? tagId.hashCode() : 0);
    }
}
