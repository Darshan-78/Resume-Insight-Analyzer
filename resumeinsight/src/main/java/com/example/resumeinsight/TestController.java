package com.example.resumeinsight;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class TestController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello your Api is working";
    }

    @PostMapping("/test")
    public String testPost(@RequestBody String data) {
        return "Received: " + data;
    }
}