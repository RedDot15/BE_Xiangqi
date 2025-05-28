package com.example.xiangqi.repository;

import com.example.xiangqi.entity.my_sql.MatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, Long> {
    @Query("SELECT m FROM MatchEntity m WHERE (:userId IS NULL OR m.blackPlayerEntity.id = :userId OR m.redPlayerEntity.id = :userId) AND m.result NOT IN ('PLAYING...', 'Match cancel.')")
    Page<MatchEntity> findAllFinished(Pageable pageable, @Param("userId") Long userId);
}
