package com.liangwang.gateway.test.gatewayclient;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallBackController {

    @RequestMapping("/user/fallback")
    public Mono<String> fallback() {
        return Mono.just("service error, jump fallback");
    }
}
