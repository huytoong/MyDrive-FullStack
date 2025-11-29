package com.mydrive.demo.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "directories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Directory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "directory_id")
  private Integer id;

  @Column(name = "directory_name", nullable = false)
  private String name;

  @ManyToOne
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @ManyToOne
  @JoinColumn(name = "parent_directory_id")
  private Directory parentDirectory;

  @OneToMany(mappedBy = "parentDirectory", cascade = CascadeType.ALL)
  private Set<Directory> subdirectories = new HashSet<>();

  @OneToMany(mappedBy = "directory", cascade = CascadeType.ALL)
  private Set<File> files = new HashSet<>();

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}