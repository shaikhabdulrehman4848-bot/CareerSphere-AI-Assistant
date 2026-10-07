package com.careersphere.repository;

import com.careersphere.entity.Entities.StudyTopic;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {
    Optional<StudyTopic> findByIdAndPlanUserId(Long id, Long userId);
}
