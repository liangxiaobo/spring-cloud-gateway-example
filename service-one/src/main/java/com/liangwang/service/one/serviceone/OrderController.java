package com.liangwang.service.one.serviceone;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RequestMapping("/order")
@RestController
public class OrderController {

    @RequestMapping("/info")
    public String orderInfo() {
        return "order info date : " + new Date().toString();
    }
}
