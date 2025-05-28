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
@Table(name = "actions", schema = "xiangqi", indexes = {
        @Index(name = "match_id", columnList = "match_id")
})
public class ActionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    MatchEntity matchEntity;

    @Size(max = 50)
    @NotNull
    @Column(name = "chess_piece", nullable = false, length = 50)
    String chessPiece;

    @NotNull
    @Column(name = "move_number", nullable = false)
    Integer moveNumber;

    @Size(max = 5)
    @NotNull
    @Column(name = "from_position", nullable = false, length = 5)
    String fromPosition;

    @Size(max = 5)
    @NotNull
    @Column(name = "to_position", nullable = false, length = 5)
    String toPosition;

    @Column(name = "move_time")
    Instant moveTime;

}