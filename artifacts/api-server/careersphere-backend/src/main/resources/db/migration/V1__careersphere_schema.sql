CREATE TABLE skills (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  category VARCHAR(48) NOT NULL,
  CONSTRAINT pk_skills PRIMARY KEY (id),
  CONSTRAINT uk_skills_name UNIQUE (name)
);

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  phone VARCHAR(30),
  location VARCHAR(120),
  education VARCHAR(180),
  graduation_year INTEGER,
  target_role VARCHAR(120),
  experience_level VARCHAR(32) NOT NULL,
  github_url VARCHAR(500),
  linkedin_url VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_skills (
  user_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  CONSTRAINT pk_user_skills PRIMARY KEY (user_id, skill_id),
  CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id)
);

CREATE TABLE job_roles (
  id VARCHAR(80) NOT NULL,
  name VARCHAR(120) NOT NULL,
  CONSTRAINT pk_job_roles PRIMARY KEY (id)
);

CREATE TABLE job_role_skills (
  job_role_id VARCHAR(80) NOT NULL,
  skill_id BIGINT NOT NULL,
  CONSTRAINT pk_job_role_skills PRIMARY KEY (job_role_id, skill_id),
  CONSTRAINT fk_job_role_skills_role FOREIGN KEY (job_role_id) REFERENCES job_roles(id) ON DELETE CASCADE,
  CONSTRAINT fk_job_role_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id)
);

CREATE TABLE resumes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  target_role VARCHAR(120) NOT NULL,
  pdf_content LONGBLOB NOT NULL,
  extracted_text LONGTEXT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT pk_resumes PRIMARY KEY (id),
  CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX ix_resumes_user_created ON resumes(user_id, created_at);

CREATE TABLE resume_analysis (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resume_id BIGINT NOT NULL,
  ats_score INTEGER NOT NULL,
  overall_score INTEGER NOT NULL,
  technical_skills VARCHAR(12000),
  soft_skills VARCHAR(6000),
  missing_skills VARCHAR(6000),
  strengths VARCHAR(6000),
  weaknesses VARCHAR(6000),
  suggestions VARCHAR(12000),
  recommended_technologies VARCHAR(6000),
  recommended_roles VARCHAR(6000),
  keywords VARCHAR(6000),
  project_suggestions VARCHAR(6000),
  assessment_mode VARCHAR(16) NOT NULL,
  assessment_note VARCHAR(500),
  CONSTRAINT pk_resume_analysis PRIMARY KEY (id),
  CONSTRAINT uk_resume_analysis_resume UNIQUE (resume_id),
  CONSTRAINT fk_resume_analysis_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
);

CREATE TABLE interviews (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role VARCHAR(120) NOT NULL,
  experience_level VARCHAR(32) NOT NULL,
  difficulty VARCHAR(16) NOT NULL,
  question_count INTEGER NOT NULL,
  status VARCHAR(16) NOT NULL,
  overall_score DOUBLE,
  strengths VARCHAR(4000),
  weaknesses VARCHAR(4000),
  created_at TIMESTAMP NOT NULL,
  assessment_mode VARCHAR(16) NOT NULL,
  assessment_note VARCHAR(500),
  CONSTRAINT pk_interviews PRIMARY KEY (id),
  CONSTRAINT fk_interviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX ix_interviews_user_created ON interviews(user_id, created_at);

CREATE TABLE interview_questions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  question_number INTEGER NOT NULL,
  category VARCHAR(48) NOT NULL,
  prompt VARCHAR(1200) NOT NULL,
  CONSTRAINT pk_interview_questions PRIMARY KEY (id),
  CONSTRAINT uk_interview_question_number UNIQUE (interview_id, question_number),
  CONSTRAINT fk_interview_questions_interview FOREIGN KEY (interview_id) REFERENCES interviews(id) ON DELETE CASCADE
);

CREATE TABLE interview_answers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  answer_text LONGTEXT NOT NULL,
  score DOUBLE NOT NULL,
  technical_correctness DOUBLE NOT NULL,
  completeness DOUBLE NOT NULL,
  clarity DOUBLE NOT NULL,
  relevance DOUBLE NOT NULL DEFAULT 0.0,
  communication DOUBLE NOT NULL,
  feedback LONGTEXT NOT NULL,
  assessment_mode VARCHAR(16) NOT NULL,
  CONSTRAINT pk_interview_answers PRIMARY KEY (id),
  CONSTRAINT uk_interview_answers_question UNIQUE (question_id),
  CONSTRAINT fk_interview_answers_question FOREIGN KEY (question_id) REFERENCES interview_questions(id) ON DELETE CASCADE
);

CREATE TABLE study_plans (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_role VARCHAR(120) NOT NULL,
  CONSTRAINT pk_study_plans PRIMARY KEY (id),
  CONSTRAINT uk_study_plan_user UNIQUE (user_id),
  CONSTRAINT fk_study_plans_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE study_topics (
  id BIGINT NOT NULL AUTO_INCREMENT,
  study_plan_id BIGINT NOT NULL,
  week INTEGER NOT NULL,
  title VARCHAR(120) NOT NULL,
  description VARCHAR(800) NOT NULL,
  completed BOOLEAN NOT NULL,
  order_index INTEGER NOT NULL,
  CONSTRAINT pk_study_topics PRIMARY KEY (id),
  CONSTRAINT fk_study_topics_plan FOREIGN KEY (study_plan_id) REFERENCES study_plans(id) ON DELETE CASCADE
);
CREATE INDEX ix_study_topics_plan_order ON study_topics(study_plan_id, order_index);
