package com.example.api.controller;

import com.example.api.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final List<User> users = new ArrayList<>(List.of(
        new User(1, "John Doe", "john@example.com"),
        new User(2, "Jane Smith", "jane@example.com")
    ));

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", users
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable int id) {
        Optional<User> user = users.stream()
            .filter(u -> u.getId() == id)
            .findFirst();

        if (user.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", user.get()
            ));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "success", false,
            "message", "User not found"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        user.setId(users.size() + 1);
        users.add(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", user
        ));
    }
}
