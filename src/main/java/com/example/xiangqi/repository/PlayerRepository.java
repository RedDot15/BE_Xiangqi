package com.example.xiangqi.repository;

import com.example.xiangqi.entity.my_sql.PlayerEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    @Query("SELECT p FROM PlayerEntity p WHERE (:role IS NULL OR p.role = :role)")
    Page<PlayerEntity> findAll(Pageable pageable, @Param("role") String role);

    Optional<PlayerEntity> findByUsername(@Size(max = 50) @NotNull String username);

    @Query("SELECT p.rating FROM PlayerEntity p WHERE p.id = :id")
    Optional<Integer> findRatingById(@NotNull Long id);

    @Query("SELECT p.id FROM PlayerEntity p WHERE p.role = :role")
    Optional<Long> findIdByRole(@Param("role") String role);

    @Query("SELECT p.role FROM PlayerEntity p WHERE p.id = :id")
    Optional<String> findRoleById(@NotNull Long id);

}
