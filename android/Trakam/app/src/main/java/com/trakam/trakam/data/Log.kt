package com.trakam.trakam.data

import java.util.*

data class Log(val uuid: String,
               val firstName: String,
               val lastName: String,
               val blacklisted: Boolean,
               val timestamp: Date)
