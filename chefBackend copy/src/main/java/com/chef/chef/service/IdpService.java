package com.chef.chef.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.chef.chef.model.posts;
import com.chef.chef.repository.IdpRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdpService {

    private final IdpRepository idpRepository;

    // Get all projects or filter by style
    public List<posts> getProjects(String style) {
        if (style == null || style.isBlank()) {
            return idpRepository.findAll();
        }

        return idpRepository.findByStyle(style);
    }

    // Get available styles from all projects
    public List<String> getStyles() {
        return idpRepository.findAll()
                .stream()
                .map(posts::getStyle)
                .filter(style -> style != null && !style.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}