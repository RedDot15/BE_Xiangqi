package com.example.xiangqi.entity.my_sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "players", schema = "xiangqi")
@PrimaryKeyJoinColumn(name = "id")
public class PlayerEntity extends UserEntity {
    @NotNull
    @ColumnDefault("1200")
    @Column(name = "rating", nullable = false)
    Integer rating;

    @PrePersist void control() {
        setRating(1200);
        setRole("PLAYER");
    }
}