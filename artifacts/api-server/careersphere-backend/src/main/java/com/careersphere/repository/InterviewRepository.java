package com.careersphere.repository;

import com.careersphere.entity.Entities.Interview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    @EntityGraph(attributePaths = {"questions", "questions.answer"})
    List<Interview> findByUserIdOrderByCreatedAtDesc(Long userId);
    @EntityGraph(attributePaths = {"questions", "questions.answer"})
    Optional<Interview> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndStatus(Long userId, String status);
}
