package com.mydrive.demo.controller;

import com.mydrive.demo.entity.User;
import com.mydrive.demo.security.JwtTokenProvider;
import com.mydrive.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private UserService userService;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.get("username"),
            loginRequest.get("password"))
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtTokenProvider.generateToken((UserDetails) authentication.getPrincipal());

    User user = userService.findByUsername(loginRequest.get("username")).orElseThrow();

    Map<String, Object> response = new HashMap<>();
    response.put("token", jwt);
    response.put("id", user.getId());
    response.put("username", user.getUsername());
    response.put("email", user.getEmail());
    response.put("fullName", user.getFullName());
    response.put("storageUsed", user.getStorageUsed());
    response.put("storageLimit", user.getStorageLimit());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody User user) {
    if (userService.existsByUsername(user.getUsername())) {
      return ResponseEntity.badRequest().body("Username is already taken");
    }

    if (userService.existsByEmail(user.getEmail())) {
      return ResponseEntity.badRequest().body("Email is already in use");
    }

    User registeredUser = userService.register(user);

    Map<String, Object> response = new HashMap<>();
    response.put("message", "User registered successfully");
    response.put("id", registeredUser.getId());
    response.put("username", registeredUser.getUsername());

    return ResponseEntity.ok(response);
  }
}
