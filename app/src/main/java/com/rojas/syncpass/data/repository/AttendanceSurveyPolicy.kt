package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.session.Session

/**
 * A monitor may only survey an attendance they registered or scanned themselves.
 * Coordinators may survey any attendance. The null fallback keeps attendance rows
 * cached before registeredByUserId was introduced usable for the monitor's own
 * authenticated attendance.
 */
fun AttendanceEntity.canBeSurveyedBy(session: Session): Boolean {
    if (session.isCoordinator) return true
    if (registeredByUserId == session.userId) return true
    return registeredByUserId == null && userId == session.userId
}
