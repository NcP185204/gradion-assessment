package com.gradion.backend.repository;

import com.gradion.backend.model.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {
    List<Character> findByProjectIdOrderByDisplayOrder(Long projectId);
    long countByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
