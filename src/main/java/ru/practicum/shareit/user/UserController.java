package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDto> getAllUsers() {
        log.info("GetMapping/Получение всех пользователей");
        return userService.getAllUsers();
    }

    @GetMapping(value = "/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        log.info("GetMapping/Получение пользователя по id: " + id);
        return userService.getUserById(id);
    }

    @PostMapping
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        log.info("PostMapping/Создание пользователя: " + userDto);
        return userService.createUser(userDto);
    }

    @DeleteMapping(value = "/{id}")
    public void removeUser(@PathVariable Long id) {
        log.info("DeleteMapping/Удаление пользователя по id: " + id);
        userService.removeUser(id);
    }

    @PatchMapping(value = "/{id}")
    public UserDto patchUser(
            @Valid
            @RequestBody UserDto userDto,
            @PathVariable Long id) {
        log.info("PatchMapping/Обновление пользователя с id: " + id +
                " обновляемая часть: " + userDto);
        return userService.patchUser(userDto, id);
    }
}