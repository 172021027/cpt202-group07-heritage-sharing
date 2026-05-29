package com.example.heritage_sharing_api.service;

import com.example.heritage_sharing_api.entity.ResourceTag;
import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.repository.ResourceTagRepository;
import com.example.heritage_sharing_api.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ResourceTagRepository resourceTagRepository;

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public Tag getTagById(Long id) {
        return tagRepository.findById(id).orElse(null);
    }

    public Tag saveTag(Tag tag) {
        return tagRepository.save(tag);
    }

    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }

    public void mergeTags(Long secondaryTagId, Long primaryTagId) throws Exception {
        Tag secondaryTag = getTagById(secondaryTagId);
        Tag primaryTag = getTagById(primaryTagId);

        if (secondaryTag == null || primaryTag == null) {
            throw new Exception("One or both tags not found");
        }

        if (secondaryTagId.equals(primaryTagId)) {
            throw new Exception("Cannot merge a tag with itself");
        }

        // Get all resources with secondary tag and update them to primary tag
        List<ResourceTag> secondaryTaggedResources = resourceTagRepository.findByTagId(secondaryTagId);
        for (ResourceTag rt : secondaryTaggedResources) {
            // Check if this resource already has the primary tag
            boolean existsWithPrimary = resourceTagRepository.existsByResourceIdAndTagId(rt.getResourceId(), primaryTagId);
            resourceTagRepository.delete(rt);
            if (!existsWithPrimary) {
                // Primary key fields on ResourceTag are immutable once managed by Hibernate.
                resourceTagRepository.save(new ResourceTag(rt.getResourceId(), primaryTagId));
            }
        }

        // Delete the secondary tag
        deleteTag(secondaryTagId);
    }
}
