package com.mydrive.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "shared_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedItem {
  public enum ItemType {
    file, directory
  }

  public enum PermissionLevel {
    view, edit
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "share_id")
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false)
  private ItemType itemType;

  @Column(name = "item_id", nullable = false)
  private Integer itemId;

  @ManyToOne
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @ManyToOne
  @JoinColumn(name = "shared_with_id", nullable = false)
  private User sharedWith;

  @Enumerated(EnumType.STRING)
  @Column(name = "permission_level")
  private PermissionLevel permissionLevel = PermissionLevel.view;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;
}