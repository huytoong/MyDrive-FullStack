package com.mydrive.demo.controller;

import com.mydrive.demo.entity.User;
import com.mydrive.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
  @Autowired
  private UserService userService;

  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Map<String, Object> response = new HashMap<>();
    response.put("id", user.getId());
    response.put("username", user.getUsername());
    response.put("email", user.getEmail());
    response.put("fullName", user.getFullName());
    response.put("storageUsed", user.getStorageUsed());
    response.put("storageLimit", user.getStorageLimit());

    return ResponseEntity.ok(response);
  }

  @PutMapping("/me")
  public ResponseEntity<?> updateCurrentUser(@RequestBody Map<String, String> updateRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    Optional<User> userOptional = userService.findByUsername(username);
    if (userOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    User user = userOptional.get();

    // Update only allowed fields
    if (updateRequest.containsKey("fullName")) {
      user.setFullName(updateRequest.get("fullName"));
    }

    if (updateRequest.containsKey("email")) {
      // Check if email is already taken by another user
      if (!user.getEmail().equals(updateRequest.get("email")) &&
          userService.existsByEmail(updateRequest.get("email"))) {
        return ResponseEntity.badRequest().body("Email is already in use");
      }
      user.setEmail(updateRequest.get("email"));
    }

    User updatedUser = userService.update(user);

    Map<String, Object> response = new HashMap<>();
    response.put("id", updatedUser.getId());
    response.put("username", updatedUser.getUsername());
    response.put("email", updatedUser.getEmail());
    response.put("fullName", updatedUser.getFullName());
    response.put("storageUsed", updatedUser.getStorageUsed());
    response.put("storageLimit", updatedUser.getStorageLimit());

    return ResponseEntity.ok(response);
  }
}
