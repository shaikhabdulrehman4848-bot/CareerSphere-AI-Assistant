package com.careersphere.repository;

import com.careersphere.entity.Entities.InterviewQuestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    Optional<InterviewQuestion> findByIdAndInterviewId(Long id, Long interviewId);
}
