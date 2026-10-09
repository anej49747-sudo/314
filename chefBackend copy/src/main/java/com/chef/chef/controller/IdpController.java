package com.chef.chef.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chef.chef.model.posts;
import com.chef.chef.service.IdpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/idp")
@RequiredArgsConstructor
public class IdpController {

    private final IdpService idpService;

    // GET /api/idp
    // GET /api/idp?style=Modern
    @GetMapping
    public List<posts> getProjects(
            @RequestParam(value = "style", required = false)
            String style) {

        return idpService.getProjects(style);
    }

    // GET /api/idp/styles
    @GetMapping("/styles")
    public List<String> getStyles() {
        return idpService.getStyles();
    }
}