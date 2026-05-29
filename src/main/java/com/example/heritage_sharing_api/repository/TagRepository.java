package com.example.heritage_sharing_api.repository;

import com.example.heritage_sharing_api.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTagNameIgnoreCase(String tagName);
}
