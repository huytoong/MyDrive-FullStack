package com.mydrive.demo.service;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.File;
import com.mydrive.demo.entity.User;
import com.mydrive.demo.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {
  private final FileRepository fileRepository;
  private final UserService userService;
  private final DirectoryService directoryService;
  private final Path fileStorageLocation;

  @Autowired
  public FileService(
      FileRepository fileRepository,
      UserService userService,
      DirectoryService directoryService,
      @Value("${file.upload.directory}") String uploadDir) {
    this.fileRepository = fileRepository;
    this.userService = userService;
    this.directoryService = directoryService;

    this.fileStorageLocation = Paths.get(uploadDir)
        .toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (IOException ex) {
      throw new RuntimeException("Could not create the directory for uploading files", ex);
    }
  }

  public List<File> findAll() {
    return fileRepository.findAll();
  }

  public Optional<File> findById(Integer id) {
    return fileRepository.findById(id);
  }

  public List<File> findByOwner(User owner) {
    return fileRepository.findByOwner(owner);
  }

  public List<File> findByDirectory(Directory directory) {
    return fileRepository.findByDirectory(directory);
  }

  public List<File> findByOwnerAndDirectory(User owner, Directory directory) {
    return fileRepository.findByOwnerAndDirectory(owner, directory);
  }

  public Optional<File> findByNameAndOwnerAndDirectory(String name, User owner, Directory directory) {
    return fileRepository.findByNameAndOwnerAndDirectory(name, owner, directory);
  }

  @Transactional
  public File uploadFile(MultipartFile file, User owner, Directory directory) throws IOException {
    // Check if user has enough storage
    if (!userService.hasEnoughStorage(owner.getId(), file.getSize())) {
      throw new RuntimeException("Not enough storage space");
    }

    // Normalize file name to avoid security issues
    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
    if (originalFileName.contains("..")) {
      throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
    }

    // Create a unique file name to prevent collisions
    String fileExtension = "";
    if (originalFileName.contains(".")) {
      fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

    // Create physical file
    Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

    // Save file metadata to database
    File fileEntity = new File();
    fileEntity.setName(originalFileName);
    fileEntity.setType(file.getContentType());
    fileEntity.setSize(file.getSize());
    fileEntity.setPath(uniqueFileName);
    fileEntity.setOwner(owner);
    fileEntity.setDirectory(directory);

    // Update user storage used
    userService.updateStorageUsed(owner.getId(), file.getSize());

    return fileRepository.save(fileEntity);
  }

  public Resource loadFileAsResource(File file) throws MalformedURLException {
    Path filePath = fileStorageLocation.resolve(file.getPath()).normalize();
    Resource resource = new UrlResource(filePath.toUri());

    if (resource.exists()) {
      return resource;
    } else {
      throw new RuntimeException("File not found: " + file.getPath());
    }
  }

  @Transactional
  public File update(File file) {
    return fileRepository.save(file);
  }

  @Transactional
  public void delete(Integer id) throws IOException {
    Optional<File> fileOptional = fileRepository.findById(id);
    if (fileOptional.isPresent()) {
      File file = fileOptional.get();

      // Delete physical file
      Path filePath = fileStorageLocation.resolve(file.getPath()).normalize();
      Files.deleteIfExists(filePath);

      // Update user storage used
      userService.updateStorageUsed(file.getOwner().getId(), -file.getSize());

      // Delete database entry
      fileRepository.delete(file);
    }
  }

  public boolean isFileOwner(Integer fileId, Integer userId) {
    return fileRepository.findById(fileId)
        .map(file -> file.getOwner().getId().equals(userId))
        .orElse(false);
  }

  @Transactional
  public List<File> uploadFolder(List<MultipartFile> files, List<String> paths, User owner, Directory parentDirectory)
      throws IOException {
    if (files.size() != paths.size()) {
      throw new IllegalArgumentException("Files and paths count mismatch");
    }
    List<File> uploadedFiles = new java.util.ArrayList<>();
    for (int i = 0; i < files.size(); i++) {
      MultipartFile file = files.get(i);
      String relPath = paths.get(i); // e.g. "subdir1/subdir2/file.txt"
      String[] parts = relPath.replace("\\", "/").split("/");
      Directory currentDir = parentDirectory;
      // Traverse and create directories as needed (all except last part)
      for (int j = 0; j < parts.length - 1; j++) {
        String dirName = parts[j];
        java.util.Optional<Directory> dirOpt = directoryService.findByNameAndOwnerAndParentDirectory(dirName, owner,
            currentDir);
        if (dirOpt.isPresent()) {
          currentDir = dirOpt.get();
        } else {
          Directory newDir = new Directory();
          newDir.setName(dirName);
          newDir.setOwner(owner);
          newDir.setParentDirectory(currentDir);
          currentDir = directoryService.create(newDir);
        }
      }
      // The file name is the last part
      String fileName = parts[parts.length - 1];
      // Always use the original MultipartFile
      File uploaded = uploadFile(file, owner, currentDir);
      // If the intended file name is different, update the File entity
      if (!uploaded.getName().equals(fileName)) {
        uploaded.setName(fileName);
        uploaded = update(uploaded);
      }
      uploadedFiles.add(uploaded);
    }
    return uploadedFiles;
  }
}
