package net.focik.homeoffice.async;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "async_tasks")
public class AsyncTask {

    @Id
    @Column(name = "job_id", length = 36, nullable = false)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AsyncTaskStatus status;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "processed")
    private int processed;

    @Column(name = "total")
    private int total;

    @Column(name = "message")
    private String message;

    @Column(name = "textract_result_json", columnDefinition = "LONGTEXT")
    private String textractResultJson;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "async_task_errors", joinColumns = @JoinColumn(name = "job_id"))
    private List<AsyncTaskError> errors = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "duplicates")
    private int duplicates;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
