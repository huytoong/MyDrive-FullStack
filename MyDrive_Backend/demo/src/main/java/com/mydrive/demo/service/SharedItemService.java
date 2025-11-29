package com.mydrive.demo.service;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.SharedItem;
import com.mydrive.demo.entity.User;
import com.mydrive.demo.repository.DirectoryRepository;
import com.mydrive.demo.repository.SharedItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SharedItemService {
  private final SharedItemRepository sharedItemRepository;
  private final DirectoryRepository directoryRepository;

  public SharedItemService(SharedItemRepository sharedItemRepository, DirectoryRepository directoryRepository) {
    this.sharedItemRepository = sharedItemRepository;
    this.directoryRepository = directoryRepository;
  }

  public List<SharedItem> findAll() {
    return sharedItemRepository.findAll();
  }

  public Optional<SharedItem> findById(Integer id) {
    return sharedItemRepository.findById(id);
  }

  public List<SharedItem> findByOwner(User owner) {
    return sharedItemRepository.findByOwner(owner);
  }

  public List<SharedItem> findBySharedWith(User sharedWith) {
    return sharedItemRepository.findBySharedWith(sharedWith);
  }

  public List<SharedItem> findByItemTypeAndItemId(SharedItem.ItemType itemType, Integer itemId) {
    return sharedItemRepository.findByItemTypeAndItemId(itemType, itemId);
  }

  public Optional<SharedItem> findByItemTypeAndItemIdAndSharedWith(
      SharedItem.ItemType itemType, Integer itemId, User sharedWith) {
    return sharedItemRepository.findByItemTypeAndItemIdAndSharedWith(itemType, itemId, sharedWith);
  }

  @Transactional
  public SharedItem create(SharedItem sharedItem) {
    return sharedItemRepository.save(sharedItem);
  }

  @Transactional
  public SharedItem update(SharedItem sharedItem) {
    return sharedItemRepository.save(sharedItem);
  }

  @Transactional
  public void delete(Integer id) {
    sharedItemRepository.deleteById(id);
  }

  /**
   * Check if a user has access to a specific item
   */
  public boolean hasViewAccess(SharedItem.ItemType itemType, Integer itemId, Integer userId) {
    List<SharedItem> sharedItems = sharedItemRepository.findByItemTypeAndItemId(itemType, itemId);

    return sharedItems.stream()
        .anyMatch(item -> item.getSharedWith().getId().equals(userId));
  }

  /**
   * Check if a user has edit access to a specific item
   */
  public boolean hasEditAccess(SharedItem.ItemType itemType, Integer itemId, Integer userId) {
    List<SharedItem> sharedItems = sharedItemRepository.findByItemTypeAndItemId(itemType, itemId);

    return sharedItems.stream()
        .anyMatch(item -> item.getSharedWith().getId().equals(userId) &&
            item.getPermissionLevel() == SharedItem.PermissionLevel.edit);
  }

  /**
   * Remove all shares for a specific item
   */
  @Transactional
  public void removeAllShares(SharedItem.ItemType itemType, Integer itemId) {
    List<SharedItem> sharedItems = sharedItemRepository.findByItemTypeAndItemId(itemType, itemId);
    sharedItemRepository.deleteAll(sharedItems);
  }

  /**
   * Check if a user has view access to a directory (recursively up to parent)
   */
  public boolean hasRecursiveDirectoryAccess(Integer directoryId, Integer userId) {
    // Check direct share
    List<SharedItem> sharedItems = sharedItemRepository.findByItemTypeAndItemId(SharedItem.ItemType.directory,
        directoryId);
    boolean direct = sharedItems.stream().anyMatch(item -> item.getSharedWith().getId().equals(userId));
    if (direct)
      return true;
    // Check parent
    Optional<Directory> dirOpt = directoryRepository.findById(directoryId);
    if (dirOpt.isPresent()) {
      Directory dir = dirOpt.get();
      if (dir.getParentDirectory() != null) {
        return hasRecursiveDirectoryAccess(dir.getParentDirectory().getId(), userId);
      }
    }
    return false;
  }
}
