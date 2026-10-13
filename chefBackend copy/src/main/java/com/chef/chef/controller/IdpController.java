package com.chef.chef.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chef.chef.model.IDP;
import com.chef.chef.service.IdpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/idp")
@RequiredArgsConstructor
public class IdpController {

    private final IdpService idpService;

    @PostMapping
    public IDP createIdp(@RequestBody IDP idp) {
        return idpService.createIdp(idp);
    }

    @GetMapping("/designer/{designerId}")
    public List<IDP> getIdpsByDesigner(
            @PathVariable String designerId) {
        return idpService.getIdpsByDesigner(designerId);
    }

    @GetMapping("/{id}")
    public IDP getIdpById(@PathVariable String id) {
        return idpService.getIdpById(id);
    }

    @PutMapping("/{id}")
    public IDP updateIdp(
            @PathVariable String id,
            @RequestParam String designerId,
            @RequestBody IDP updatedIdp) {
        return idpService.updateIdp(id, designerId, updatedIdp);
    }

    @DeleteMapping("/{id}")
    public void deleteIdp(
            @PathVariable String id,
            @RequestParam String designerId) {
        idpService.deleteIdp(id, designerId);
    }
}