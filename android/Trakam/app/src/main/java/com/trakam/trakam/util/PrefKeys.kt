package com.trakam.trakam.util

object PrefKeys {
    object Server {
        const val KEY_HOST = "pref_key_server_host"
        const val KEY_PORT = "pref_key_server_port"

        object Default {
            const val HOST = "192.168.0.189"
            const val PORT = "8080"
        }
    }

    object LiveFeed {
        const val KEY_PORT = "pref_key_live_feed_port"

        object Default {
            const val PORT = "8090"
        }
    }
}