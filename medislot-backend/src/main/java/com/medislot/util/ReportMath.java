package com.medislot.util;

import com.medislot.dto.DashboardDtos.CountStat;
import com.medislot.dto.ReportDtos.Bucket;
import com.medislot.dto.ReportDtos.Report;
import com.medislot.entity.AppointmentStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Pure report helpers, kept separate so they are easy to test. */
public final class ReportMath {

    private ReportMath() {}

    public static double rate(long part, long total) {
        return total == 0 ? 0.0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    public static String autoGranularity(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 31) return "day";
        return days <= 183 ? "week" : "month";
    }

    /** Start date of every bucket in the range, matching PostgreSQL date_trunc (weeks start on Monday). */
    public static List<LocalDate> bucketStarts(LocalDate from, LocalDate to, String unit) {
        List<LocalDate> out = new ArrayList<>();
        LocalDate d = switch (unit) {
            case "week" -> from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> from.withDayOfMonth(1);
            default -> from;
        };
        while (!d.isAfter(to)) {
            out.add(d);
            d = switch (unit) {
                case "week" -> d.plusWeeks(1);
                case "month" -> d.plusMonths(1);
                default -> d.plusDays(1);
            };
        }
        return out;
    }

    /** Rows of [id, name, AppointmentStatus, count] into one CountStat per name, busiest first. */
    public static List<CountStat> aggregate(List<Object[]> rows) {
        Map<Long, long[]> sums = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Object[] r : rows) {
            Long id = (Long) r[0];
            names.put(id, (String) r[1]);
            AppointmentStatus status = (AppointmentStatus) r[2];
            long n = ((Number) r[3]).longValue();
            long[] s = sums.computeIfAbsent(id, k -> new long[4]);
            s[0] += n;
            if (status == AppointmentStatus.COMPLETED) s[1] += n;
            else if (status == AppointmentStatus.CANCELLED) s[2] += n;
            else if (status == AppointmentStatus.NO_SHOW) s[3] += n;
        }
        return sums.entrySet().stream()
                .map(e -> new CountStat(names.get(e.getKey()), e.getValue()[0], e.getValue()[1],
                        e.getValue()[2], e.getValue()[3]))
                .sorted(Comparator.comparingLong(CountStat::booked).reversed()).toList();
    }

    /** One CSV cell. Leading = + - @ are neutralised so a spreadsheet never runs a name as a formula. */
    public static String cell(String value) {
        String s = value == null ? "" : value;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static String csv(Report r) {
        StringBuilder sb = new StringBuilder();
        sb.append("MediSlot report,").append(r.from()).append(',').append(r.to()).append('\n');
        sb.append("metric,value\n");
        var s = r.summary();
        sb.append("total,").append(s.total()).append('\n');
        sb.append("completed,").append(s.completed()).append('\n');
        sb.append("cancelled,").append(s.cancelled()).append('\n');
        sb.append("no_show,").append(s.noShow()).append('\n');
        sb.append("checked_in,").append(s.checkedIn()).append('\n');
        sb.append("cancellation_rate_percent,").append(s.cancellationRate()).append('\n');
        sb.append("no_show_rate_percent,").append(s.noShowRate()).append('\n');
        sb.append("avg_wait_minutes,").append(s.avgWaitMinutes() == null ? "" : s.avgWaitMinutes()).append('\n');
        sb.append('\n').append("section,name,booked,completed,cancelled,no_show\n");
        for (Bucket b : r.series()) {
            sb.append("period,").append(cell(b.label())).append(',').append(b.booked()).append(',')
                    .append(b.completed()).append(',').append(b.cancelled()).append(',').append(b.noShow()).append('\n');
        }
        for (CountStat c : r.doctors()) row(sb, "doctor", c);
        for (CountStat c : r.departments()) row(sb, "department", c);
        return sb.toString();
    }

    private static void row(StringBuilder sb, String section, CountStat c) {
        sb.append(section).append(',').append(cell(c.name())).append(',').append(c.booked()).append(',')
                .append(c.completed()).append(',').append(c.cancelled()).append(',').append(c.noShow()).append('\n');
    }
}