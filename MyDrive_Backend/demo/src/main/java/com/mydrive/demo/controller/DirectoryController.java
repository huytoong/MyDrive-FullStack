package com.mydrive.demo.controller;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.File;
import com.mydrive.demo.entity.User;
import com.mydrive.demo.service.DirectoryService;
import com.mydrive.demo.service.FileService;
import com.mydrive.demo.service.SharedItemService;
import com.mydrive.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/directories")
public class DirectoryController {
  @Autowired
  private DirectoryService directoryService;

  @Autowired
  private UserService userService;

  @Autowired
  private SharedItemService sharedItemService;

  @Autowired
  private FileService fileService;

  @GetMapping
  public ResponseEntity<?> getRootDirectories() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();
    List<Directory> directories = directoryService.findRootDirectoriesByOwner(user);

    List<Map<String, Object>> response = directories.stream()
        .map(this::convertToMap)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getDirectoryContents(@PathVariable Integer id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<Directory> directoryOptional = directoryService.findById(id);
    if (directoryOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Directory directory = directoryOptional.get();

    // Cho phép truy cập nếu là owner hoặc được share (đệ quy theo cha)
    boolean isOwner = directory.getOwner().getId().equals(user.getId());
    boolean isSharedWith = sharedItemService
        .hasRecursiveDirectoryAccess(directory.getId(), user.getId());
    if (!isOwner && !isSharedWith) {
      return ResponseEntity.status(403).body("Access denied");
    }

    List<Directory> subdirectories = directoryService.findByOwnerAndParentDirectory(directory.getOwner(), directory);

    Map<String, Object> response = new HashMap<>();
    response.put("id", directory.getId());
    response.put("name", directory.getName());
    response.put("parentId", directory.getParentDirectory() != null ? directory.getParentDirectory().getId() : null);
    response.put("path", directoryService.getFullPath(directory));
    response.put("subdirectories", subdirectories.stream()
        .map(this::convertToMap)
        .collect(Collectors.toList()));

    // Lấy tất cả file trong thư mục (không phân biệt owner)
    List<File> files = fileService.findByDirectory(directory);

    List<Map<String, Object>> fileResponses = files.stream()
        .map(file -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", file.getId());
          map.put("name", file.getName());
          map.put("type", file.getType());
          map.put("size", file.getSize());
          map.put("uploadDate", file.getCreatedAt());
          return map;
        })
        .collect(Collectors.toList());
    response.put("files", fileResponses);

    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<?> createDirectory(@RequestBody Map<String, Object> createRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    String name = (String) createRequest.get("name");
    Integer parentId = (Integer) createRequest.get("parentId");

    if (name == null || name.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("Directory name cannot be empty");
    }

    // Check if parent directory exists if parentId is provided
    Directory parentDirectory = null;
    if (parentId != null) {
      Optional<Directory> parentOptional = directoryService.findById(parentId);
      if (parentOptional.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      parentDirectory = parentOptional.get();

      // Check if user is the owner of the parent directory
      if (!parentDirectory.getOwner().getId().equals(user.getId())) {
        return ResponseEntity.status(403).body("Access denied");
      }
    }

    // Check if directory with same name already exists in the same location
    if (directoryService.findByNameAndOwnerAndParentDirectory(name, user, parentDirectory).isPresent()) {
      return ResponseEntity.badRequest().body("Directory with the same name already exists");
    }

    // Create new directory
    Directory newDirectory = new Directory();
    newDirectory.setName(name);
    newDirectory.setOwner(user);
    newDirectory.setParentDirectory(parentDirectory);

    Directory createdDirectory = directoryService.create(newDirectory);

    return ResponseEntity.ok(convertToMap(createdDirectory));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateDirectory(@PathVariable Integer id, @RequestBody Map<String, String> updateRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<Directory> directoryOptional = directoryService.findById(id);
    if (directoryOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Directory directory = directoryOptional.get();

    // Check if user is the owner
    if (!directory.getOwner().getId().equals(user.getId())) {
      return ResponseEntity.status(403).body("Access denied");
    }

    String newName = updateRequest.get("name");
    if (newName != null && !newName.trim().isEmpty()) {
      // Check if directory with same name already exists in the same location
      if (directoryService.findByNameAndOwnerAndParentDirectory(newName, user, directory.getParentDirectory())
          .isPresent()) {
        return ResponseEntity.badRequest().body("Directory with the same name already exists");
      }

      directory.setName(newName);
      Directory updatedDirectory = directoryService.update(directory);
      return ResponseEntity.ok(convertToMap(updatedDirectory));
    }

    return ResponseEntity.badRequest().body("No valid fields to update");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteDirectory(@PathVariable Integer id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<Directory> directoryOptional = directoryService.findById(id);
    if (directoryOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Directory directory = directoryOptional.get();

    // Check if user is the owner
    if (!directory.getOwner().getId().equals(user.getId())) {
      return ResponseEntity.status(403).body("Access denied");
    }

    // Check if it's not the root directory
    if (directory.getParentDirectory() == null) {
      return ResponseEntity.badRequest().body("Cannot delete the root directory");
    }

    directoryService.delete(id);

    return ResponseEntity.ok().build();
  }

  private Map<String, Object> convertToMap(Directory directory) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", directory.getId());
    map.put("name", directory.getName());
    map.put("parentId", directory.getParentDirectory() != null ? directory.getParentDirectory().getId() : null);
    return map;
  }
}
