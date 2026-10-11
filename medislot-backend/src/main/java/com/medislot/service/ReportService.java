package com.medislot.service;

import com.medislot.dto.ReportDtos.*;
import com.medislot.exception.ApiException;
import com.medislot.repository.DashboardQueryRepository;
import com.medislot.repository.ReportQueryRepository;
import com.medislot.security.AuthUser;
import com.medislot.util.ReportMath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Hospital admin reports. Always limited to the admin's own hospital. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Set<String> UNITS = Set.of("day", "week", "month");

    private final AdminScope scope;
    private final ReportQueryRepository reports;
    private final DashboardQueryRepository dash;
    private final AuditService audit;
    private final ZoneId zone;

    public ReportService(AdminScope scope, ReportQueryRepository reports, DashboardQueryRepository dash,
                         AuditService audit, @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.scope = scope;
        this.reports = reports;
        this.dash = dash;
        this.audit = audit;
        this.zone = ZoneId.of(timezone);
    }

    public Report build(AuthUser actor, LocalDate from, LocalDate to, String granularity) {
        Long hospitalId = scope.hospitalId(actor);
        LocalDate end = to == null ? LocalDate.now(zone) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) throw bad("INVALID_RANGE", "The start date must not be after the end date.");
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw bad("INVALID_RANGE", "A report can cover at most 366 days.");
        }
        String unit = (granularity == null || granularity.isBlank() || "auto".equals(granularity))
                ? ReportMath.autoGranularity(start, end) : granularity;
        if (!UNITS.contains(unit)) throw bad("INVALID_GRANULARITY", "granularity must be day, week, month or auto.");

        Map<String, long[]> sums = new HashMap<>();
        for (Object[] r : reports.series(hospitalId, start, end, unit)) {
            long[] s = sums.computeIfAbsent((String) r[0], k -> new long[4]);
            long n = ((Number) r[2]).longValue();
            s[0] += n;
            String status = (String) r[1];
            if ("COMPLETED".equals(status)) s[1] += n;
            else if ("CANCELLED".equals(status)) s[2] += n;
            else if ("NO_SHOW".equals(status)) s[3] += n;
        }
        List<Bucket> series = ReportMath.bucketStarts(start, end, unit).stream().map(d -> {
            long[] s = sums.getOrDefault(d.toString(), new long[4]);
            return new Bucket(d.toString(), s[0], s[1], s[2], s[3]);
        }).toList();

        long total = series.stream().mapToLong(Bucket::booked).sum();
        long completed = series.stream().mapToLong(Bucket::completed).sum();
        long cancelled = series.stream().mapToLong(Bucket::cancelled).sum();
        long noShow = series.stream().mapToLong(Bucket::noShow).sum();

        Object[] wait = reports.averageWait(hospitalId, start, end);
        Double avg = wait[0] == null ? null : Math.round(((Number) wait[0]).doubleValue() * 10.0) / 10.0;
        long samples = wait[1] == null ? 0 : ((Number) wait[1]).longValue();

        Summary summary = new Summary(total, completed, cancelled, noShow, dash.checkedIn(hospitalId, start, end),
                ReportMath.rate(cancelled, total), ReportMath.rate(noShow, total), avg, samples);
        return new Report(start, end, unit, summary, series,
                ReportMath.aggregate(dash.byDoctor(hospitalId, start, end)),
                ReportMath.aggregate(dash.byDepartment(hospitalId, start, end)));
    }

    /** Writable on purpose: every export is recorded in the audit log. */
    @Transactional
    public String export(AuthUser actor, LocalDate from, LocalDate to, String granularity) {
        Report r = build(actor, from, to, granularity);
        audit.log(actor.id(), actor.email(), "REPORT_EXPORTED", "REPORT", null,
                "from=" + r.from() + ",to=" + r.to(), null);
        return ReportMath.csv(r);
    }

    private static ApiException bad(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}