package com.chef.chef.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.chef.chef.repository.FavouriteRepository;
import com.chef.chef.repository.IdpRepository;
import com.chef.chef.repository.UserRepository;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final IdpRepository idpRepository;
    private final FavouriteRepository favouriteRepository;

    public ReportService(
            UserRepository userRepository,
            IdpRepository idpRepository,
            FavouriteRepository favouriteRepository) {
        this.userRepository = userRepository;
        this.idpRepository = idpRepository;
        this.favouriteRepository = favouriteRepository;
    }

    public Map<String, Object> generateReport(String period) {

        if (period == null
                || !List.of("daily", "weekly", "monthly")
                        .contains(period.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Period must be daily, weekly or monthly");
        }

        String selectedPeriod = period.toLowerCase();

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        int numberOfBuckets;

        switch (selectedPeriod) {
            case "daily" -> {
                startDate = today.minusDays(6);
                numberOfBuckets = 7;
            }
            case "weekly" -> {
                startDate = today
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .minusWeeks(7);
                numberOfBuckets = 8;
            }
            case "monthly" -> {
                startDate = today.withDayOfMonth(1).minusMonths(5);
                numberOfBuckets = 6;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported report period");
        }

        List<Map<String, Object>> trend = new ArrayList<>();

        long totalCustomers = 0L;
        long totalIdps = 0L;
        long totalFavourites = 0L;

        for (int i = 0; i < numberOfBuckets; i++) {

            LocalDate bucketStart;
            LocalDate bucketEnd;

            switch (selectedPeriod) {
                case "daily" -> {
                    bucketStart = startDate.plusDays(i);
                    bucketEnd = bucketStart.plusDays(1);
                }
                case "weekly" -> {
                    bucketStart = startDate.plusWeeks(i);
                    bucketEnd = bucketStart.plusWeeks(1);
                }
                case "monthly" -> {
                    bucketStart = startDate.plusMonths(i);
                    bucketEnd = bucketStart.plusMonths(1);
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported report period");
            }

            LocalDateTime start = bucketStart.atStartOfDay();
            LocalDateTime end = bucketEnd.atStartOfDay();

            Long customerCount = userRepository.countCustomersCreatedBetween(
                    "customer", start, end);

            Long idpCount = idpRepository.countIdpsCreatedBetween(
                    start, end);

            Long favouriteCount = favouriteRepository.countFavouritesCreatedBetween(
                    start, end);

            long customers = customerCount == null ? 0L : customerCount;
            long idps = idpCount == null ? 0L : idpCount;
            long favourites = favouriteCount == null ? 0L : favouriteCount;

            Map<String, Object> point = new LinkedHashMap<>();

            point.put("date", bucketStart.toString());
            point.put("customers", customers);
            point.put("idps", idps);
            point.put("favourites", favourites);

            trend.add(point);

            totalCustomers += customers;
            totalIdps += idps;
            totalFavourites += favourites;
        }

        Map<String, Object> report = new LinkedHashMap<>();

        report.put("period", selectedPeriod);
        report.put("startDate", startDate.toString());
        report.put("endDate", today.plusDays(1).toString());
        report.put("totalCustomers", totalCustomers);
        report.put("totalIdps", totalIdps);
        report.put("totalFavourites", totalFavourites);
        report.put("trend", trend);

        return report;
    }
}