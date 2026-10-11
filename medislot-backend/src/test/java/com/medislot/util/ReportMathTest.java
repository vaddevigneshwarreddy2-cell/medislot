package com.medislot.util;

import com.medislot.dto.DashboardDtos.CountStat;
import com.medislot.entity.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportMathTest {

    @Test
    void ratesAreRoundedAndSafeForZero() {
        assertEquals(33.3, ReportMath.rate(1, 3));
        assertEquals(0.0, ReportMath.rate(0, 0));
    }

    @Test
    void granularityGrowsWithTheRange() {
        assertEquals("day", ReportMath.autoGranularity(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30)));
        assertEquals("week", ReportMath.autoGranularity(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 29)));
        assertEquals("month", ReportMath.autoGranularity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 27)));
    }

    @Test
    void bucketStartsMatchPostgresTruncation() {
        assertEquals(3, ReportMath.bucketStarts(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), "day").size());
        // 14 Oct 2026 is a Wednesday, so its week starts on Monday 12 Oct.
        assertEquals(List.of(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26)),
                ReportMath.bucketStarts(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 28), "week"));
        assertEquals(List.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1)),
                ReportMath.bucketStarts(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 2), "month"));
    }

    @Test
    void aggregateSumsPerNameAndSortsBusiestFirst() {
        List<Object[]> rows = List.of(
                new Object[]{1L, "Dr A", AppointmentStatus.COMPLETED, 2L},
                new Object[]{1L, "Dr A", AppointmentStatus.CANCELLED, 1L},
                new Object[]{2L, "Dr B", AppointmentStatus.NO_SHOW, 5L});
        List<CountStat> out = ReportMath.aggregate(rows);
        assertEquals("Dr B", out.get(0).name());
        assertEquals(5, out.get(0).booked());
        assertEquals(3, out.get(1).booked());
        assertEquals(2, out.get(1).completed());
        assertEquals(1, out.get(1).cancelled());
    }

    @Test
    void csvCellsNeutraliseFormulasAndQuoteSpecialCharacters() {
        assertEquals("'=cmd", ReportMath.cell("=cmd"));
        assertEquals("'@x", ReportMath.cell("@x"));
        assertEquals("\"Smith, Dr\"", ReportMath.cell("Smith, Dr"));
        assertEquals("\"say \"\"hi\"\"\"", ReportMath.cell("say \"hi\""));
        assertEquals("", ReportMath.cell(null));
    }
}