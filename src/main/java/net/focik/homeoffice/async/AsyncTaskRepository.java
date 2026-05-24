package net.focik.homeoffice.async;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTask, String> {
    boolean existsByJobTypeAndStatusAndCreatedAtGreaterThanEqual(
            String jobType, AsyncTaskStatus status, java.time.LocalDateTime createdAt);
}
