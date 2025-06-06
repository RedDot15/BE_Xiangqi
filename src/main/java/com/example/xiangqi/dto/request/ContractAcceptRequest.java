package com.example.xiangqi.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractAcceptRequest {
    @NotBlank(message = "Match contract ID is required.")
    String matchContractId;

    @NotNull(message = "Acceptor ID is required.")
    Long acceptorId;
}
