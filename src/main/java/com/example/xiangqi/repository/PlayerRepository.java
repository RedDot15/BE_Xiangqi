package com.example.xiangqi.repository;

import com.example.xiangqi.entity.PlayerEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    Optional<PlayerEntity> findByUsername(@Size(max = 50) @NotNull String username);

    @Query("SELECT p.rating FROM PlayerEntity p WHERE p.id = :id")
    Optional<Integer> findRatingById(@NotNull Long id);
}
