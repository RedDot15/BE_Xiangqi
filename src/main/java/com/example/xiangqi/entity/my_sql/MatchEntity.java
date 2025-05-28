package com.example.xiangqi.entity.my_sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "matchs", schema = "xiangqi", indexes = {
        @Index(name = "matchs_ibfk_1_idx", columnList = "red_player_id"),
        @Index(name = "matchs_ibfk_2_idx", columnList = "black_player_id")
})
public class MatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "red_player_id", nullable = false)
    PlayerEntity redPlayerEntity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "black_player_id", nullable = false)
    PlayerEntity blackPlayerEntity;

    @Size(max = 20)
    @Column(name = "result", length = 20)
    String result;

    @Column(name = "start_time")
    Instant startTime;

    @Column(name = "end_time")
    Instant endTime;

    @PrePersist void control(){
        setStartTime(Instant.now());
        setResult("PLAYING...");
    }
}