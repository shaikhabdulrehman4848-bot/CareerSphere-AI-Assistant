package com.careersphere.repository;

import com.careersphere.entity.Entities.StudyPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    @EntityGraph(attributePaths = "topics")
    Optional<StudyPlan> findByUserId(Long userId);
}
