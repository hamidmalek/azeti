package com.example.azeti.security

import com.google.common.annotations.VisibleForTesting
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitingFilter() : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    @Autowired
    private lateinit var rateLimitingProperties: RateLimitingProperties

    @Volatile
    var enabled: Boolean = true

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!enabled) {
            filterChain.doFilter(request, response)
            return
        }
        val username = request.userPrincipal?.name
        if (username == null) {
            filterChain.doFilter(request, response)
            return
        }

        val bucket = buckets.computeIfAbsent(username) {
            val limit = Bandwidth.simple(
                rateLimitingProperties.capacity,
                Duration.ofMinutes(rateLimitingProperties.durationMinutes)
            )
            println("HMD $limit")
            Bucket.builder()
                .addLimit(limit)
                .build()
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        }
    }

    @VisibleForTesting
    fun resetBucket(username: String) {
        buckets.remove(username)
    }
}

@Bean
fun rateLimitFilter(): FilterRegistrationBean<RateLimitingFilter> {
    val registration = FilterRegistrationBean(RateLimitingFilter())
    registration.order = Ordered.HIGHEST_PRECEDENCE + 1
    return registration
}

@Component
@ConfigurationProperties(prefix = "rate-limiting")
class RateLimitingProperties {
    var capacity: Long = 0
    var durationMinutes: Long = 1
}
