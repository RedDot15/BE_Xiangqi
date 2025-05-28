package com.example.xiangqi.controller;

import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.MatchContractService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.xiangqi.helper.ResponseBuilder.buildResponse;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("/api/match-contracts")
public class MatchContractController {
    MatchContractService matchContractService;

    @PatchMapping("{matchContractId}/accept")
    public ResponseEntity<ResponseObject> accept(@PathVariable String matchContractId) {
        // Accept contract
        matchContractService.accept(matchContractId);
        // Return
        return buildResponse(HttpStatus.OK, "Accept contract completed.", null);
    }
}
