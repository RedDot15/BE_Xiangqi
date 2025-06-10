package com.example.xiangqi.controller.rest;

import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.my_sql.HistoryService;
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
@RequestMapping("/api/histories")
public class HistoryController {
    HistoryService historyService;

    @GetMapping("")
    public ResponseEntity<ResponseObject> getAllByUserId(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long userId) {
        // Fetch match list
        return buildResponse(HttpStatus.OK, "Histories fetch successfully.", historyService.getAllByUserId(page, size, userId));
    }
}
