package com.gradion.backend.repository;

import com.gradion.backend.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByProjectId(Long projectId);
    long countByProjectId(Long projectId);
}
