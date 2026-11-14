package com.yezdiconnect.app.alerts

enum class AlertKind { CALL, MESSAGE }

/** An incoming call or message to show on the dash. [from] is the caller/sender as shown by Android. */
data class PhoneAlert(val kind: AlertKind, val from: String, val app: String, val postedAt: Long)
