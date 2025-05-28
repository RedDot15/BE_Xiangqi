package com.example.xiangqi.entity.redis;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractPlayerEntity {
    Long id;

    Boolean acceptStatus;
}
