package com.medislot.service;

import com.medislot.dto.HospitalDtos.*;
import com.medislot.security.AuthUser;

/** Hospital administration (own hospital for HOSPITAL_ADMIN, any hospital for SUPER_ADMIN). */
public interface HospitalManagementService {
    Detail getOwn(AuthUser actor);
    Detail updateOwnProfile(AuthUser actor, ProfileRequest request);
    Detail updateOwnLocation(AuthUser actor, LocationRequest request);

    Detail create(AuthUser actor, CreateRequest request);
    Detail updateProfile(AuthUser actor, Long hospitalId, ProfileRequest request);
    Detail updateLocation(AuthUser actor, Long hospitalId, LocationRequest request);
    Detail setActive(AuthUser actor, Long hospitalId, boolean active);
}