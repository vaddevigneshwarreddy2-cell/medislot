package com.medislot.dto;

import com.medislot.dto.DashboardDtos.CountStat;

import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {}

    public record Summary(long total, long completed, long cancelled, long noShow, long checkedIn,
                          double cancellationRate, double noShowRate, Double avgWaitMinutes, long waitSamples) {}

    public record Bucket(String label, long booked, long completed, long cancelled, long noShow) {}

    public record Report(LocalDate from, LocalDate to, String granularity, Summary summary, List<Bucket> series,
                         List<CountStat> doctors, List<CountStat> departments) {}
}