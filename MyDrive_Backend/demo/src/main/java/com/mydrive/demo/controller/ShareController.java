package com.mydrive.demo.controller;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.File;
import com.mydrive.demo.entity.SharedItem;
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
@RequestMapping("/api/shared")
public class ShareController {
  @Autowired
  private SharedItemService sharedItemService;

  @Autowired
  private UserService userService;

  @Autowired
  private FileService fileService;

  @Autowired
  private DirectoryService directoryService;

  @GetMapping("/with-me")
  public ResponseEntity<?> getItemsSharedWithMe() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();
    List<SharedItem> sharedItems = sharedItemService.findBySharedWith(user);

    List<Map<String, Object>> response = sharedItems.stream()
        .map(this::convertToMap)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/by-me")
  public ResponseEntity<?> getItemsSharedByMe() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();
    List<SharedItem> sharedItems = sharedItemService.findByOwner(user);

    List<Map<String, Object>> response = sharedItems.stream()
        .map(this::convertToMap)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/file/{fileId}")
  public ResponseEntity<?> shareFile(
      @PathVariable Integer fileId,
      @RequestBody Map<String, Object> shareRequest) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User owner = userService.findByUsername(username).orElseThrow();

    String sharedWithUsername = (String) shareRequest.get("username");
    String permissionLevel = (String) shareRequest.get("permissionLevel");

    // Validate inputs
    if (sharedWithUsername == null || sharedWithUsername.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("Username to share with cannot be empty");
    }

    // Check if file exists
    Optional<File> fileOptional = fileService.findById(fileId);
    if (fileOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    File file = fileOptional.get();

    // Check if user is the owner
    if (!file.getOwner().getId().equals(owner.getId())) {
      return ResponseEntity.status(403).body("Access denied");
    }

    // Check if user to share with exists
    Optional<User> sharedWithOptional = userService.findByUsername(sharedWithUsername);
    if (sharedWithOptional.isEmpty()) {
      return ResponseEntity.badRequest().body("User to share with does not exist");
    }

    User sharedWith = sharedWithOptional.get();

    // Check if not sharing with self
    if (owner.getId().equals(sharedWith.getId())) {
      return ResponseEntity.badRequest().body("Cannot share with yourself");
    }

    // Check if already shared
    if (sharedItemService.findByItemTypeAndItemIdAndSharedWith(
        SharedItem.ItemType.file, fileId, sharedWith).isPresent()) {
      return ResponseEntity.badRequest().body("File is already shared with this user");
    }

    // Create shared item
    SharedItem sharedItem = new SharedItem();
    sharedItem.setItemType(SharedItem.ItemType.file);
    sharedItem.setItemId(fileId);
    sharedItem.setOwner(owner);
    sharedItem.setSharedWith(sharedWith);

    // Set permission level if provided
    if ("edit".equalsIgnoreCase(permissionLevel)) {
      sharedItem.setPermissionLevel(SharedItem.PermissionLevel.edit);
    } else {
      sharedItem.setPermissionLevel(SharedItem.PermissionLevel.view);
    }

    SharedItem createdSharedItem = sharedItemService.create(sharedItem);

    return ResponseEntity.ok(convertToMap(createdSharedItem));
  }

  @PostMapping("/directory/{directoryId}")
  public ResponseEntity<?> shareDirectory(
      @PathVariable Integer directoryId,
      @RequestBody Map<String, Object> shareRequest) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User owner = userService.findByUsername(username).orElseThrow();

    String sharedWithUsername = (String) shareRequest.get("username");
    String permissionLevel = (String) shareRequest.get("permissionLevel");

    // Validate inputs
    if (sharedWithUsername == null || sharedWithUsername.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("Username to share with cannot be empty");
    }

    // Check if directory exists
    Optional<Directory> directoryOptional = directoryService.findById(directoryId);
    if (directoryOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Directory directory = directoryOptional.get();

    // Check if user is the owner
    if (!directory.getOwner().getId().equals(owner.getId())) {
      return ResponseEntity.status(403).body("Access denied");
    }

    // Check if user to share with exists
    Optional<User> sharedWithOptional = userService.findByUsername(sharedWithUsername);
    if (sharedWithOptional.isEmpty()) {
      return ResponseEntity.badRequest().body("User to share with does not exist");
    }

    User sharedWith = sharedWithOptional.get();

    // Check if not sharing with self
    if (owner.getId().equals(sharedWith.getId())) {
      return ResponseEntity.badRequest().body("Cannot share with yourself");
    }

    // Check if already shared
    if (sharedItemService.findByItemTypeAndItemIdAndSharedWith(
        SharedItem.ItemType.directory, directoryId, sharedWith).isPresent()) {
      return ResponseEntity.badRequest().body("Directory is already shared with this user");
    }

    // Create shared item
    SharedItem sharedItem = new SharedItem();
    sharedItem.setItemType(SharedItem.ItemType.directory);
    sharedItem.setItemId(directoryId);
    sharedItem.setOwner(owner);
    sharedItem.setSharedWith(sharedWith);

    // Set permission level if provided
    if ("edit".equalsIgnoreCase(permissionLevel)) {
      sharedItem.setPermissionLevel(SharedItem.PermissionLevel.edit);
    } else {
      sharedItem.setPermissionLevel(SharedItem.PermissionLevel.view);
    }

    SharedItem createdSharedItem = sharedItemService.create(sharedItem);

    return ResponseEntity.ok(convertToMap(createdSharedItem));
  }

  @DeleteMapping("/{shareId}")
  public ResponseEntity<?> removeSharing(@PathVariable Integer shareId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<SharedItem> sharedItemOptional = sharedItemService.findById(shareId);
    if (sharedItemOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    SharedItem sharedItem = sharedItemOptional.get();

    // Check if user is the owner of the shared item
    if (!sharedItem.getOwner().getId().equals(user.getId())) {
      return ResponseEntity.status(403).body("Access denied");
    }

    sharedItemService.delete(shareId);

    return ResponseEntity.ok().build();
  }

  private Map<String, Object> convertToMap(SharedItem sharedItem) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", sharedItem.getId());
    map.put("itemType", sharedItem.getItemType().toString());
    map.put("itemId", sharedItem.getItemId());
    map.put("permissionLevel", sharedItem.getPermissionLevel().toString());

    Map<String, Object> ownerMap = new HashMap<>();
    ownerMap.put("id", sharedItem.getOwner().getId());
    ownerMap.put("username", sharedItem.getOwner().getUsername());

    Map<String, Object> sharedWithMap = new HashMap<>();
    sharedWithMap.put("id", sharedItem.getSharedWith().getId());
    sharedWithMap.put("username", sharedItem.getSharedWith().getUsername());

    map.put("owner", ownerMap);
    map.put("sharedWith", sharedWithMap);
    map.put("createdAt", sharedItem.getCreatedAt());

    // Add file or directory name for shared item
    if (sharedItem.getItemType() == SharedItem.ItemType.file) {
      fileService.findById(sharedItem.getItemId()).ifPresent(file -> map.put("itemName", file.getName()));
    } else if (sharedItem.getItemType() == SharedItem.ItemType.directory) {
      directoryService.findById(sharedItem.getItemId()).ifPresent(dir -> map.put("itemName", dir.getName()));
    }

    return map;
  }
}
