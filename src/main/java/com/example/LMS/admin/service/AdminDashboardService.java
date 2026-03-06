package com.example.LMS.admin.service;

import com.example.LMS.admin.repository.AdminDashboardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {
    private final AdminDashboardRepository repository;

    public AdminDashboardService(AdminDashboardRepository repository) {
        this.repository = repository;
    }

    public DashboardData load(String period) {
        String safePeriod = normalizePeriod(period);

        long totalUsers = repository.totalUsers();
        long totalEnrollments = repository.totalEnrollments();
        long totalCourses = repository.totalCourses();
        double completionRate = repository.completionRate();
        double examPassRate = repository.examPassRate();

        TrendData trendData = buildTrendData(safePeriod);

        List<AdminDashboardRepository.CategoryPoint> courseCategory = repository.courseCategoryDistribution();
        List<AdminDashboardRepository.CategoryPoint> enrollCategory = repository.enrollmentCategoryDistribution();

        List<AdminDashboardRepository.PopularCourseRow> topCourses = repository.popularCoursesTop10();
        long unansweredQnaCount = repository.unansweredQnaCount();
        List<AdminDashboardRepository.UnansweredQnaRow> unansweredQna = repository.unansweredQnaTop10();
        List<AdminDashboardRepository.LowRatingCourseRow> lowRatingCourses = repository.lowRatingCourses();

        return new DashboardData(
                safePeriod,
                totalUsers,
                totalEnrollments,
                totalCourses,
                round2(completionRate),
                round2(examPassRate),
                trendData,
                courseCategory,
                enrollCategory,
                topCourses,
                unansweredQnaCount,
                unansweredQna,
                lowRatingCourses
        );
    }

    private TrendData buildTrendData(String period) {
        return switch (period) {
            case "monthly" -> monthlyTrend();
            case "yearly" -> yearlyTrend();
            default -> dailyTrend();
        };
    }

    private TrendData dailyTrend() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        Map<String, Long> signupMap = repository.signupTrendDaily(from).stream()
                .collect(Collectors.toMap(AdminDashboardRepository.TrendPoint::bucket, AdminDashboardRepository.TrendPoint::count));
        Map<String, Long> enrollMap = repository.enrollmentTrendDaily(from).stream()
                .collect(Collectors.toMap(AdminDashboardRepository.TrendPoint::bucket, AdminDashboardRepository.TrendPoint::count));

        List<String> labels = new ArrayList<>();
        List<Long> signups = new ArrayList<>();
        List<Long> enrollments = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = from.plusDays(i);
            String key = d.toString();
            labels.add(d.format(DateTimeFormatter.ofPattern("MM-dd")));
            signups.add(signupMap.getOrDefault(key, 0L));
            enrollments.add(enrollMap.getOrDefault(key, 0L));
        }

        return new TrendData(labels, signups, enrollments);
    }

    private TrendData monthlyTrend() {
        YearMonth now = YearMonth.now();
        YearMonth from = now.minusMonths(5);

        Map<String, Long> signupMap = repository.signupTrendMonthly(from.toString()).stream()
                .collect(Collectors.toMap(AdminDashboardRepository.TrendPoint::bucket, AdminDashboardRepository.TrendPoint::count));
        Map<String, Long> enrollMap = repository.enrollmentTrendMonthly(from.toString()).stream()
                .collect(Collectors.toMap(AdminDashboardRepository.TrendPoint::bucket, AdminDashboardRepository.TrendPoint::count));

        List<String> labels = new ArrayList<>();
        List<Long> signups = new ArrayList<>();
        List<Long> enrollments = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            YearMonth ym = from.plusMonths(i);
            String key = ym.toString();
            labels.add(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            signups.add(signupMap.getOrDefault(key, 0L));
            enrollments.add(enrollMap.getOrDefault(key, 0L));
        }

        return new TrendData(labels, signups, enrollments);
    }

    private TrendData yearlyTrend() {
        int now = LocalDate.now().getYear();
        int from = now - 4;

        Map<String, Long> signupMap = repository.signupTrendYearly(from).stream()
                .collect(Collectors.toMap(AdminDashboardRepository.TrendPoint::bucket, AdminDashboardRepository.TrendPoint::count));
        Map<String, Long> enrollMap = repository.enrollmentTrendYearly(from).stream()
                .collect(Collectors.toMap(AdminDashboardRepository.TrendPoint::bucket, AdminDashboardRepository.TrendPoint::count));

        List<String> labels = new ArrayList<>();
        List<Long> signups = new ArrayList<>();
        List<Long> enrollments = new ArrayList<>();

        for (int y = from; y <= now; y++) {
            String key = String.valueOf(y);
            labels.add(key);
            signups.add(signupMap.getOrDefault(key, 0L));
            enrollments.add(enrollMap.getOrDefault(key, 0L));
        }

        return new TrendData(labels, signups, enrollments);
    }

    private static String normalizePeriod(String period) {
        if (period == null) return "daily";
        return switch (period.toLowerCase()) {
            case "monthly", "yearly" -> period.toLowerCase();
            default -> "daily";
        };
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record TrendData(List<String> labels, List<Long> signupCounts, List<Long> enrollmentCounts) {}

    public record DashboardData(
            String period,
            long totalUsers,
            long totalEnrollments,
            long totalCourses,
            double completionRate,
            double examPassRate,
            TrendData trend,
            List<AdminDashboardRepository.CategoryPoint> courseCategory,
            List<AdminDashboardRepository.CategoryPoint> enrollmentCategory,
            List<AdminDashboardRepository.PopularCourseRow> topCourses,
            long unansweredQnaCount,
            List<AdminDashboardRepository.UnansweredQnaRow> unansweredQna,
            List<AdminDashboardRepository.LowRatingCourseRow> lowRatingCourses
    ) {}
}
