package com.example.xiangqi.service.http_client;

import com.example.xiangqi.dto.response.OutboundUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outbound-user-info", url = "${spring.security.oauth2.client.google.userinfo.base-url}")
public interface OutboundUserInfoClient {
    @GetMapping(value = "${spring.security.oauth2.client.google.userinfo.path}")
    OutboundUserInfoResponse getUserInfo(@RequestParam("alt") String alt,
                                         @RequestHeader("Authorization") String authorizationHeader);
}
