package com.mydrive.demo.controller;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.File;
import com.mydrive.demo.entity.User;
import com.mydrive.demo.service.DirectoryService;
import com.mydrive.demo.service.FileService;
import com.mydrive.demo.service.SharedItemService;
import com.mydrive.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {
  @Autowired
  private FileService fileService;

  @Autowired
  private UserService userService;

  @Autowired
  private DirectoryService directoryService;

  @Autowired
  private SharedItemService sharedItemService;

  // Thêm JwtTokenProvider để giải mã token
  @Autowired
  private com.mydrive.demo.security.JwtTokenProvider jwtTokenProvider;

  @Autowired
  private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

  @GetMapping
  public ResponseEntity<?> getAllFiles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();
    List<File> files = fileService.findByOwner(user);

    List<Map<String, Object>> response = files.stream()
        .map(this::convertToMap)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/directory/{directoryId}")
  public ResponseEntity<?> getFilesByDirectory(@PathVariable Integer directoryId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<Directory> directoryOptional = directoryService.findById(directoryId);
    if (directoryOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Directory directory = directoryOptional.get();

    // Check if user is the owner
    if (!directory.getOwner().getId().equals(user.getId())) {
      return ResponseEntity.status(403).body("Access denied");
    }

    List<File> files = fileService.findByOwnerAndDirectory(user, directory);

    List<Map<String, Object>> response = files.stream()
        .map(this::convertToMap)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getFile(@PathVariable Integer id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<File> fileOptional = fileService.findById(id);
    if (fileOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    File file = fileOptional.get();

    // Cho phép truy cập nếu là owner hoặc được share (đệ quy theo cha thư mục)
    boolean isOwner = file.getOwner().getId().equals(user.getId());
    boolean isSharedWith = sharedItemService
        .hasRecursiveDirectoryAccess(file.getDirectory() != null ? file.getDirectory().getId() : null, user.getId());
    boolean isFileShared = sharedItemService
        .findByItemTypeAndItemIdAndSharedWith(com.mydrive.demo.entity.SharedItem.ItemType.file, file.getId(), user)
        .isPresent();
    if (!isOwner && !isFileShared && !isSharedWith) {
      return ResponseEntity.status(403).body("Access denied");
    }

    return ResponseEntity.ok(convertToMap(file));
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<?> downloadFile(@PathVariable Integer id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<File> fileOptional = fileService.findById(id);
    if (fileOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    File file = fileOptional.get();

    // LOGIC CŨ: chỉ cho phép owner hoặc file được share trực tiếp với quyền phù hợp
    // (ví dụ: view hoặc edit)
    // KHÔNG cho phép download nếu chỉ được share qua thư mục cha hoặc share
    // view/edit đều như nhau
    // Cho phép download nếu là owner hoặc được share (dù quyền view hay edit, share
    // trực tiếp hoặc qua thư mục)
    boolean isOwner = file.getOwner().getId().equals(user.getId());
    boolean isSharedWith = sharedItemService
        .hasRecursiveDirectoryAccess(file.getDirectory() != null ? file.getDirectory().getId() : null, user.getId());
    boolean isFileShared = sharedItemService
        .findByItemTypeAndItemIdAndSharedWith(com.mydrive.demo.entity.SharedItem.ItemType.file, file.getId(), user)
        .isPresent();
    // Không kiểm tra quyền view/edit, chỉ cần được share là có thể download
    if (!isOwner && !isFileShared && !isSharedWith) {
      return ResponseEntity.status(403).body("Access denied");
    }

    try {
      Resource resource = fileService.loadFileAsResource(file);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
          .header(HttpHeaders.CONTENT_TYPE, file.getType())
          .body(resource);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Could not download the file: " + e.getMessage());
    }
  }

  @PostMapping("/upload")
  public ResponseEntity<?> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "directoryId", required = false) Integer directoryId) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    // Check if user has enough storage
    if (!userService.hasEnoughStorage(user.getId(), file.getSize())) {
      return ResponseEntity.badRequest().body("Not enough storage space");
    }

    // Check if directory exists if directoryId is provided
    Directory directory = null;
    if (directoryId != null) {
      Optional<Directory> directoryOptional = directoryService.findById(directoryId);
      if (directoryOptional.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      directory = directoryOptional.get();

      // Check if user is the owner of the directory
      if (!directory.getOwner().getId().equals(user.getId())) {
        return ResponseEntity.status(403).body("Access denied");
      }
    } else {
      // Use root directory if no directoryId is provided
      List<Directory> rootDirectories = directoryService.findRootDirectoriesByOwner(user);
      if (!rootDirectories.isEmpty()) {
        directory = rootDirectories.get(0);
      }
    }

    try {
      File uploadedFile = fileService.uploadFile(file, user, directory);
      return ResponseEntity.ok(convertToMap(uploadedFile));
    } catch (IOException e) {
      return ResponseEntity.badRequest().body("Could not upload the file: " + e.getMessage());
    }
  }

  @PostMapping(value = "/upload-folder", consumes = "multipart/form-data")
  public ResponseEntity<?> uploadFolder(
      @RequestParam("files") List<MultipartFile> files,
      @RequestParam("paths") String[] paths,
      @RequestParam(value = "directoryId", required = false) Integer directoryId,
      @RequestParam(value = "token", required = false) String token) {

    // DEBUG LOGGING
    System.out.println("==== [uploadFolder] ====");
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("Authentication: " + auth);
    System.out.println("Username: " + (auth != null ? auth.getName() : "null"));
    System.out.println(
        "Token from FormData: " + (token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : "null"));
    System.out.println("=======================");

    if (files.size() != paths.length) {
      return ResponseEntity.badRequest().body("Files and paths count mismatch");
    }

    // Nếu không có auth trong SecurityContext, nhưng có token từ FormData
    // Cần thêm mã để xác thực token và đặt vào SecurityContext
    // (Đây là hardcode tạm trong ví dụ này)
    if ((auth == null || auth.getName().equals("anonymousUser")) && token != null && !token.isEmpty()) {
      try {
        System.out.println("Using token from FormData: " + token);

        // Giải mã token để lấy username
        String tokenWithoutBearer = token;
        if (token.startsWith("Bearer ")) {
          tokenWithoutBearer = token.substring(7);
        }

        // Sử dụng JwtTokenProvider để giải mã token
        String username = jwtTokenProvider.getUsernameFromToken(tokenWithoutBearer);
        System.out.println("Extracted username from token: " + username);

        // Kiểm tra user có tồn tại không
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found with extracted username: " + username));

        System.out.println("User found by token: " + user.getUsername());

        Directory parentDirectory = null;
        if (directoryId != null) {
          Optional<Directory> directoryOptional = directoryService.findById(directoryId);
          if (directoryOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
          }
          parentDirectory = directoryOptional.get();
          if (!parentDirectory.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Access denied");
          }
        } else {
          // Use root directory if no directoryId is provided
          List<Directory> rootDirectories = directoryService.findRootDirectoriesByOwner(user);
          if (!rootDirectories.isEmpty()) {
            parentDirectory = rootDirectories.get(0);
          }
        }

        try {
          List<File> uploadedFiles = fileService.uploadFolder(files, java.util.Arrays.asList(paths), user,
              parentDirectory);
          List<Map<String, Object>> response = uploadedFiles.stream().map(this::convertToMap)
              .collect(Collectors.toList());
          return ResponseEntity.ok(response);
        } catch (IOException e) {
          return ResponseEntity.badRequest().body("Could not upload the folder: " + e.getMessage());
        }
      } catch (Exception e) {
        return ResponseEntity.status(403).body("Invalid token");
      }
    }

    // Original authentication flow
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    User user = userService.findByUsername(username).orElseThrow();

    Directory parentDirectory = null;
    if (directoryId != null) {
      Optional<Directory> directoryOptional = directoryService.findById(directoryId);
      if (directoryOptional.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      parentDirectory = directoryOptional.get();
      if (!parentDirectory.getOwner().getId().equals(user.getId())) {
        return ResponseEntity.status(403).body("Access denied");
      }
    } else {
      // Use root directory if no directoryId is provided
      List<Directory> rootDirectories = directoryService.findRootDirectoriesByOwner(user);
      if (!rootDirectories.isEmpty()) {
        parentDirectory = rootDirectories.get(0);
      }
    }

    try {
      List<File> uploadedFiles = fileService.uploadFolder(files, java.util.Arrays.asList(paths), user, parentDirectory);
      List<Map<String, Object>> response = uploadedFiles.stream().map(this::convertToMap).collect(Collectors.toList());
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      return ResponseEntity.badRequest().body("Could not upload the folder: " + e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteFile(@PathVariable Integer id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userService.findByUsername(username).orElseThrow();

    Optional<File> fileOptional = fileService.findById(id);
    if (fileOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    File file = fileOptional.get();

    // Check if user is the owner
    if (!file.getOwner().getId().equals(user.getId())) {
      return ResponseEntity.status(403).body("Access denied");
    }

    try {
      fileService.delete(id);
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      return ResponseEntity.badRequest().body("Could not delete the file: " + e.getMessage());
    }
  }

  private Map<String, Object> convertToMap(File file) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", file.getId());
    map.put("name", file.getName());
    map.put("type", file.getType());
    map.put("size", file.getSize());
    map.put("directoryId", file.getDirectory() != null ? file.getDirectory().getId() : null);
    map.put("createdAt", file.getCreatedAt());
    map.put("updatedAt", file.getUpdatedAt());
    return map;
  }
}
