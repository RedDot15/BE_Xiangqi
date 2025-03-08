package com.example.xiangqi.controller;

import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.helper.ResponseObject;
import com.example.xiangqi.service.MatchService;
import com.example.xiangqi.service.PlayerService;
import com.example.xiangqi.validation.group.Create;
import com.example.xiangqi.validation.group.Update;
import jakarta.validation.groups.Default;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.example.xiangqi.helper.ResponseBuilder.buildResponse;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
@RequestMapping("/api/match")
public class MatchController {

}
