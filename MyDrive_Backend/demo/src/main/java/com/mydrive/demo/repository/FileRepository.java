package com.mydrive.demo.repository;

import com.mydrive.demo.entity.Directory;
import com.mydrive.demo.entity.File;
import com.mydrive.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {
  List<File> findByOwner(User owner);

  List<File> findByDirectory(Directory directory);

  List<File> findByOwnerAndDirectory(User owner, Directory directory);

  Optional<File> findByNameAndOwnerAndDirectory(String name, User owner, Directory directory);
}
