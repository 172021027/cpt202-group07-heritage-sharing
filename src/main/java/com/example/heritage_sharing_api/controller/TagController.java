package com.example.heritage_sharing_api.controller;

import com.example.heritage_sharing_api.entity.Tag;
import com.example.heritage_sharing_api.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin("*")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tag> getTagById(@PathVariable Long id) {
        Tag tag = tagService.getTagById(id);
        if (tag != null) {
            return ResponseEntity.ok(tag);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Tag> createTag(@RequestBody Tag tag) {
        try {
            Tag saved = tagService.saveTag(tag);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tag> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        try {
            Tag existing = tagService.getTagById(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }
            tag.setTagId(id);
            Tag updated = tagService.saveTag(tag);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        try {
            Tag existing = tagService.getTagById(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }
            tagService.deleteTag(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<Map<String, String>> mergeTags(
            @RequestParam Long secondaryTagId,
            @RequestParam Long primaryTagId) {
        try {
            tagService.mergeTags(secondaryTagId, primaryTagId);
            return ResponseEntity.ok(Map.of("message", "Tags merged successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
