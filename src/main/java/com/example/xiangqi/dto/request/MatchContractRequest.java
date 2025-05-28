package com.example.xiangqi.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchContractRequest {
    ContractPlayerRequest player1;

    ContractPlayerRequest player2;
}
