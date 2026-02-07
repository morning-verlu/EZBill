package org.kukro.ezbill

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform