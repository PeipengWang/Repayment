package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "示例接口", description = "用于测试 Knife4j 的简单接口")
@RestController
public class HelloController {

    @Operation(summary = "问候接口", description = "根据传入的 name 返回问候语")
    @GetMapping("/api/hello")
    public String hello(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }
}