package com.medislot.dto;

import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {}

    public record NextAppointment(Long appointmentId, String doctorName, String hospitalName, LocalDate date,
                                  String time, String status) {}

    public record PatientDashboard(long upcoming, long completed, long cancelled, long noShows, long followUps,
                                   long unreadNotifications, NextAppointment next) {}

    public record DoctorDashboard(LocalDate date, long total, long expected, long waiting, long inConsultation,
                                  long completed, long noShow, long upcomingLater) {}

    public record CountStat(String name, long booked, long completed, long cancelled, long noShow) {}

    public record AdminDashboard(String period, LocalDate from, LocalDate to, long total, long completed,
                                 long cancelled, long noShow, long checkedIn, double cancellationRate,
                                 int activeDoctors, List<CountStat> doctors, List<CountStat> departments) {}
}