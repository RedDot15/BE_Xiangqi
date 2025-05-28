package com.example.xiangqi.repository;

import com.example.xiangqi.entity.my_sql.ActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends JpaRepository<ActionEntity, Long> {
}
