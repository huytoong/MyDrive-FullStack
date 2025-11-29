package com.mydrive.demo.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@lombok.Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Integer id;

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "full_name", length = 100)
  private String fullName;

  @Column(name = "storage_used")
  private Long storageUsed = 0L;

  @Column(name = "storage_limit")
  private Long storageLimit = 5368709120L; // 5GB default

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
  private Set<Directory> directories = new HashSet<>();

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
  private Set<File> files = new HashSet<>();

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
  private Set<SharedItem> sharedItems = new HashSet<>();

  @OneToMany(mappedBy = "sharedWith", cascade = CascadeType.ALL)
  private Set<SharedItem> sharedWithMe = new HashSet<>();

}