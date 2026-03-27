package com.diary.repository;

import com.diary.entity.Task;
import com.diary.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
        SELECT t FROM Task t
        JOIN FETCH t.user u
        WHERE u.id = :userId
          AND (:status IS NULL OR t.status = :status)
        """)
    Page<Task> findByUserIdAndOptionalStatus(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            Pageable pageable);


    @Query("""
        SELECT t FROM Task t
        JOIN FETCH t.user u
        WHERE (:status IS NULL OR t.status = :status)
        """)
    Page<Task> findAllWithOptionalStatus(
            @Param("status") TaskStatus status,
            Pageable pageable);


    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.id = :id")
    Optional<Task> findByIdWithUser(@Param("id") Long id);
}
