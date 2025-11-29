package com.mydrive.demo.repository;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryRepository extends JpaRepository<Directory, Integer> {
  List<Directory> findByOwner(User owner);

  List<Directory> findByParentDirectoryId(Integer parentDirectoryId);

  List<Directory> findByOwnerAndParentDirectoryIsNull(User owner);

  List<Directory> findByOwnerAndParentDirectory(User owner, Directory parentDirectory);

  Optional<Directory> findByNameAndOwnerAndParentDirectory(String name, User owner, Directory parentDirectory);
}