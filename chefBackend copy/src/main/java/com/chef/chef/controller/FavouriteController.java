package com.chef.chef.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chef.chef.model.Favourite;
import com.chef.chef.model.IDP;
import com.chef.chef.service.FavouriteService;

@RestController
@RequestMapping("/api/favourites")
public class FavouriteController {

    private final FavouriteService favouriteService;

    public FavouriteController(FavouriteService favouriteService) {
        this.favouriteService = favouriteService;
    }

    @PostMapping("/{customerId}/{idpId}")
    public Favourite addFavourite(
            @PathVariable String customerId,
            @PathVariable String idpId) {
        return favouriteService.addFavourite(customerId, idpId);
    }

    @GetMapping("/{customerId}")
    public List<IDP> getFavourites(
            @PathVariable String customerId) {
        return favouriteService.getFavourites(customerId);
    }

    @DeleteMapping("/{customerId}/{idpId}")
    public void removeFavourite(
            @PathVariable String customerId,
            @PathVariable String idpId) {
        favouriteService.removeFavourite(customerId, idpId);
    }
}