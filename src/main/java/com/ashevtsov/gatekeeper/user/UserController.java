package com.ashevtsov.gatekeeper.user;

import com.ashevtsov.gatekeeper.common.dto.PageResponse;
import com.ashevtsov.gatekeeper.user.dto.CreateUserRequest;
import com.ashevtsov.gatekeeper.user.dto.UpdateUserRequest;
import com.ashevtsov.gatekeeper.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CRUD пользователей — создание, поиск, обновление, soft delete
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Управление пользователями")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пользователя")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Список пользователей (с поиском)")
    public PageResponse<UserResponse> getAll(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(userService.getAll(tenantId, search, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }
}
