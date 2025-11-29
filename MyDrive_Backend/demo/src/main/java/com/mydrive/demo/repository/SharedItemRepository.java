package com.mydrive.demo.repository;

import com.mydrive.demo.entity.SharedItem;
import com.mydrive.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedItemRepository extends JpaRepository<SharedItem, Integer> {
  List<SharedItem> findByOwner(User owner);

  List<SharedItem> findBySharedWith(User sharedWith);

  List<SharedItem> findByItemTypeAndItemId(SharedItem.ItemType itemType, Integer itemId);

  Optional<SharedItem> findByItemTypeAndItemIdAndSharedWith(SharedItem.ItemType itemType, Integer itemId,
      User sharedWith);
}
