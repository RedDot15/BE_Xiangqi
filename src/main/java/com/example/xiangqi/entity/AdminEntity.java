package com.example.xiangqi.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "admins", schema = "xiangqi")
@PrimaryKeyJoinColumn(name = "id")
public class AdminEntity extends UserEntity {

}