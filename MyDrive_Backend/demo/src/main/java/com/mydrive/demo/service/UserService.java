package com.mydrive.demo.service;

import com.mydrive.demo.entity.User;
import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.repository.UserRepository;
import com.mydrive.demo.repository.DirectoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final DirectoryRepository directoryRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UserService(UserRepository userRepository,
      DirectoryRepository directoryRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.directoryRepository = directoryRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<User> findAll() {
    return userRepository.findAll();
  }

  public Optional<User> findById(Integer id) {
    return userRepository.findById(id);
  }

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Transactional
  public User register(User user) {
    // Encrypt password
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    // Set default storage limits
    if (user.getStorageLimit() == null) {
      user.setStorageLimit(5368709120L); // 5GB default
    }
    user.setStorageUsed(0L);

    // Save user
    User savedUser = userRepository.save(user);

    // Create root directory
    Directory rootDir = new Directory();
    rootDir.setName("Root");
    rootDir.setOwner(savedUser);
    rootDir.setParentDirectory(null);
    directoryRepository.save(rootDir);

    return savedUser;
  }

  @Transactional
  public User update(User user) {
    return userRepository.save(user);
  }

  @Transactional
  public void updateStorageUsed(Integer userId, Long additionalSize) {
    userRepository.findById(userId).ifPresent(user -> {
      user.setStorageUsed(user.getStorageUsed() + additionalSize);
      userRepository.save(user);
    });
  }

  public boolean hasEnoughStorage(Integer userId, Long fileSize) {
    return userRepository.findById(userId)
        .map(user -> (user.getStorageLimit() - user.getStorageUsed()) >= fileSize)
        .orElse(false);
  }

  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  @Transactional
  public void delete(Integer id) {
    userRepository.deleteById(id);
  }
}
