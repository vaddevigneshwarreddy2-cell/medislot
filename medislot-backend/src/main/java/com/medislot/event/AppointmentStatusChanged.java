package com.medislot.event;

import com.medislot.entity.AppointmentStatus;

/** Published by the state machine for every status change. "from" is null for a newly created row. */
public record AppointmentStatusChanged(Long appointmentId, AppointmentStatus from, AppointmentStatus to,
                                       Long actorUserId) {}