package com.emircankirez.mobileproject.notification

import java.util.concurrent.atomic.AtomicInteger

object NotificationID {
    private val c = AtomicInteger(0)
    val id: Int
        get() = c.incrementAndGet()
}