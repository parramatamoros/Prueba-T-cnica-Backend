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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @InjectMocks private TaskService taskService;

    private User ownerUser;
    private User otherUser;
    private User adminUser;
    private Task task;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().id(1L).username("owner").role(Role.USER).build();
        otherUser = User.builder().id(2L).username("other").role(Role.USER).build();
        adminUser = User.builder().id(3L).username("admin").role(Role.ADMIN).build();

        task = Task.builder()
                .id(10L)
                .title("Comprar leche")
                .description("2 litros")
                .status(TaskStatus.PENDING)
                .user(ownerUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskRequest = new TaskRequest();
        taskRequest.setTitle("Comprar leche");
        taskRequest.setDescription("2 litros");
    }


    @Nested
    @DisplayName("getTasks()")
    class GetTasksTests {

        @Test
        @DisplayName("USER debe ver solo sus propias tareas")
        void userShouldSeeOwnTasksOnly() {
            // given
            Page<Task> page = new PageImpl<>(List.of(task));
            given(taskRepository.findByUserIdAndOptionalStatus(eq(1L), isNull(), any(Pageable.class)))
                    .willReturn(page);

            // when
            Page<TaskResponse> result = taskService.getTasks(ownerUser, null, Pageable.unpaged());

            // then
            assertThat(result.getContent()).hasSize(1);
            then(taskRepository).should(never()).findAllWithOptionalStatus(any(), any());
        }

        @Test
        @DisplayName("ADMIN debe ver todas las tareas")
        void adminShouldSeeAllTasks() {
            // given
            Page<Task> page = new PageImpl<>(List.of(task));
            given(taskRepository.findAllWithOptionalStatus(isNull(), any(Pageable.class)))
                    .willReturn(page);

            // when
            Page<TaskResponse> result = taskService.getTasks(adminUser, null, Pageable.unpaged());

            // then
            assertThat(result.getContent()).hasSize(1);
            then(taskRepository).should(never()).findByUserIdAndOptionalStatus(any(), any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // getTaskById()
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTaskById()")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Owner puede ver su propia tarea")
        void ownerCanSeeOwnTask() {
            given(taskRepository.findByIdWithUser(10L)).willReturn(Optional.of(task));
            TaskResponse response = taskService.getTaskById(10L, ownerUser);
            assertThat(response.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("ADMIN puede ver tarea de cualquier usuario")
        void adminCanSeeAnyTask() {
            given(taskRepository.findByIdWithUser(10L)).willReturn(Optional.of(task));
            TaskResponse response = taskService.getTaskById(10L, adminUser);
            assertThat(response.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("USER no puede ver tarea de otro usuario")
        void userCannotSeeOtherUserTask() {
            given(taskRepository.findByIdWithUser(10L)).willReturn(Optional.of(task));
            assertThatThrownBy(() -> taskService.getTaskById(10L, otherUser))
                    .isInstanceOf(TaskAccessDeniedException.class);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la tarea no existe")
        void shouldThrowWhenTaskNotFound() {
            given(taskRepository.findByIdWithUser(99L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> taskService.getTaskById(99L, ownerUser))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // createTask()
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createTask()")
    class CreateTaskTests {

        @Test
        @DisplayName("Debe crear tarea con estado PENDING por defecto")
        void shouldCreateTaskWithDefaultPendingStatus() {
            given(taskRepository.save(any(Task.class))).willAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(1L);
                t.setCreatedAt(LocalDateTime.now());
                t.setUpdatedAt(LocalDateTime.now());
                return t;
            });

            TaskResponse response = taskService.createTask(taskRequest, ownerUser);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(response.getOwnerUsername()).isEqualTo("owner");
        }

        @Test
        @DisplayName("Debe respetar el estado enviado en el request")
        void shouldRespectStatusFromRequest() {
            taskRequest.setStatus(TaskStatus.IN_PROGRESS);
            given(taskRepository.save(any(Task.class))).willAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(2L);
                t.setCreatedAt(LocalDateTime.now());
                t.setUpdatedAt(LocalDateTime.now());
                return t;
            });

            TaskResponse response = taskService.createTask(taskRequest, ownerUser);
            assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // deleteTask()
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteTask()")
    class DeleteTaskTests {

        @Test
        @DisplayName("Owner puede eliminar su propia tarea")
        void ownerCanDeleteOwnTask() {
            given(taskRepository.findByIdWithUser(10L)).willReturn(Optional.of(task));
            assertThatCode(() -> taskService.deleteTask(10L, ownerUser)).doesNotThrowAnyException();
            then(taskRepository).should().delete(task);
        }

        @Test
        @DisplayName("ADMIN puede eliminar tarea de cualquier usuario")
        void adminCanDeleteAnyTask() {
            given(taskRepository.findByIdWithUser(10L)).willReturn(Optional.of(task));
            assertThatCode(() -> taskService.deleteTask(10L, adminUser)).doesNotThrowAnyException();
            then(taskRepository).should().delete(task);
        }

        @Test
        @DisplayName("USER no puede eliminar tarea de otro usuario")
        void userCannotDeleteOtherUserTask() {
            given(taskRepository.findByIdWithUser(10L)).willReturn(Optional.of(task));
            assertThatThrownBy(() -> taskService.deleteTask(10L, otherUser))
                    .isInstanceOf(TaskAccessDeniedException.class);
            then(taskRepository).should(never()).delete(any());
        }
    }
}
