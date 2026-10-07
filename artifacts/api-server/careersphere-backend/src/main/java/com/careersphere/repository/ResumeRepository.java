package com.careersphere.repository;

import com.careersphere.entity.Entities.Resume;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    @EntityGraph(attributePaths = "analysis")
    List<Resume> findByUserIdOrderByCreatedAtDesc(Long userId);
    @EntityGraph(attributePaths = "analysis")
    Optional<Resume> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}
