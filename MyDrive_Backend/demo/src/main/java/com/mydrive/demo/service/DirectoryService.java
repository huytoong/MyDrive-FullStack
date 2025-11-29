package com.mydrive.demo.service;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.User;
import com.mydrive.demo.repository.DirectoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DirectoryService {
  private final DirectoryRepository directoryRepository;

  @Autowired
  public DirectoryService(DirectoryRepository directoryRepository) {
    this.directoryRepository = directoryRepository;
  }

  public List<Directory> findAll() {
    return directoryRepository.findAll();
  }

  public Optional<Directory> findById(Integer id) {
    return directoryRepository.findById(id);
  }

  public List<Directory> findByOwner(User owner) {
    return directoryRepository.findByOwner(owner);
  }

  public List<Directory> findByParentDirectoryId(Integer parentDirectoryId) {
    return directoryRepository.findByParentDirectoryId(parentDirectoryId);
  }

  public List<Directory> findRootDirectoriesByOwner(User owner) {
    return directoryRepository.findByOwnerAndParentDirectoryIsNull(owner);
  }

  public List<Directory> findByOwnerAndParentDirectory(User owner, Directory parentDirectory) {
    return directoryRepository.findByOwnerAndParentDirectory(owner, parentDirectory);
  }

  public Optional<Directory> findByNameAndOwnerAndParentDirectory(String name, User owner, Directory parentDirectory) {
    return directoryRepository.findByNameAndOwnerAndParentDirectory(name, owner, parentDirectory);
  }

  @Transactional
  public Directory create(Directory directory) {
    return directoryRepository.save(directory);
  }

  @Transactional
  public Directory update(Directory directory) {
    return directoryRepository.save(directory);
  }

  @Transactional
  public void delete(Integer id) {
    directoryRepository.deleteById(id);
  }

  public boolean isDirectoryOwner(Integer directoryId, Integer userId) {
    return directoryRepository.findById(directoryId)
        .map(directory -> directory.getOwner().getId().equals(userId))
        .orElse(false);
  }

  /**
   * Check if a directory is a subdirectory of another directory (recursively)
   */
  public boolean isSubdirectory(Directory parent, Directory child) {
    if (child.getParentDirectory() == null) {
      return false;
    }

    if (child.getParentDirectory().getId().equals(parent.getId())) {
      return true;
    }

    return isSubdirectory(parent, child.getParentDirectory());
  }

  /**
   * Get full path of directory
   */
  public String getFullPath(Directory directory) {
    if (directory.getParentDirectory() == null) {
      return "/" + directory.getName();
    }
    return getFullPath(directory.getParentDirectory()) + "/" + directory.getName();
  }
}
