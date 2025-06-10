package com.example.xiangqi.repository;

import com.example.xiangqi.entity.my_sql.UserEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(@Size(max = 50) @NotNull String username);

    Optional<UserEntity> findByEmail(@Size(max = 75) @NotNull String email);
}
