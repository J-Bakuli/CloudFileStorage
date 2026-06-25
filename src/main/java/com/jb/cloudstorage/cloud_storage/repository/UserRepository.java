package com.jb.cloudstorage.cloud_storage.repository;

import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByUsername(String username);
}
