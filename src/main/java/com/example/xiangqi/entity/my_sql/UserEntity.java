package com.example.xiangqi.entity.my_sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(name = "users", schema = "xiangqi", uniqueConstraints = {
        @UniqueConstraint(name = "username", columnNames = {"username"})
})
@Inheritance(strategy = InheritanceType.JOINED)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "username", nullable = false, length = 50)
    String username;

    @Size(max = 75)
    @Column(name = "email", length = 75)
    String email;

    @Size(max = 255)
    @NotNull
    @Column(name = "password", nullable = false)
    String password;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("PLAYER")
    @Column(name = "role", nullable = false, length = 50)
    String role;
}