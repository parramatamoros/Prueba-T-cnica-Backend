package com.diary.service;

import com.diary.dto.request.TaskRequest;
import com.diary.dto.response.TaskResponse;
import com.diary.entity.Task;
import com.diary.entity.User;
import com.diary.entity.enums.Role;
import com.diary.entity.enums.TaskStatus;
import com.diary.exception.ResourceNotFoundException;
import com.diary.exception.TaskAccessDeniedException;
import com.diary.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;


    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(User currentUser, TaskStatus status, Pageable pageable) {
        Page<Task> tasks;

        if (currentUser.getRole() == Role.ADMIN) {
            tasks = taskRepository.findAllWithOptionalStatus(status, pageable);
        } else {
            tasks = taskRepository.findByUserIdAndOptionalStatus(
                    currentUser.getId(), status, pageable);
        }

        return tasks.map(TaskResponse::from);
    }


    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, User currentUser) {
        Task task = findTaskOrThrow(id);
        checkReadAccess(task, currentUser);
        return TaskResponse.from(task);
    }


    @Transactional
    public TaskResponse createTask(TaskRequest request, User currentUser) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING)
                .user(currentUser)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Tarea creada [id={}] por usuario [{}]", saved.getId(), currentUser.getUsername());
        return TaskResponse.from(saved);
    }


    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, User currentUser) {
        Task task = findTaskOrThrow(id);
        checkWriteAccess(task, currentUser);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        Task updated = taskRepository.save(task);
        log.info("Tarea actualizada [id={}] por usuario [{}]", id, currentUser.getUsername());
        return TaskResponse.from(updated);
    }

    @Transactional
    public void deleteTask(Long id, User currentUser) {
        Task task = findTaskOrThrow(id);
        checkDeleteAccess(task, currentUser);  // Dueño o ADMIN pueden eliminar

        taskRepository.delete(task);
        log.info("Tarea eliminada [id={}] por usuario [{}]", id, currentUser.getUsername());
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarea no encontrada con id: " + id));
    }

    private void checkReadAccess(Task task, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN
                && !task.getUser().getId().equals(currentUser.getId())) {
            throw new TaskAccessDeniedException(
                    "No tienes permisos para ver la tarea con id: " + task.getId());
        }
    }

    private void checkWriteAccess(Task task, User currentUser) {
        if (!task.getUser().getId().equals(currentUser.getId())) {
            throw new TaskAccessDeniedException(
                    "No tienes permisos para modificar la tarea con id: " + task.getId());
        }
    }

    private void checkDeleteAccess(Task task, User currentUser) {
        boolean isOwner = task.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new TaskAccessDeniedException(
                    "No tienes permisos para eliminar la tarea con id: " + task.getId());
        }
    }
}
