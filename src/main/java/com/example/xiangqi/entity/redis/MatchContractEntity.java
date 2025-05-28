package com.example.xiangqi.entity.redis;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchContractEntity {
    ContractPlayerEntity player1;

    ContractPlayerEntity player2;
}
