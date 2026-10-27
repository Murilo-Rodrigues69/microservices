package isakids.nfcelab.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoginAttemptService {

    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockoutDuration;

    public LoginAttemptService(
            @Value("${app.security.login-attempt.max-failures:5}") int maxFailures,
            @Value("${app.security.login-attempt.lockout-minutes:15}") long lockoutMinutes) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("app.security.login-attempt.max-failures precisa ser maior que zero.");
        }
        if (lockoutMinutes < 1) {
            throw new IllegalArgumentException("app.security.login-attempt.lockout-minutes precisa ser maior que zero.");
        }
        this.maxFailures = maxFailures;
        this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);
    }

    public boolean isBlocked(String username, HttpServletRequest request) {
        String key = keyFor(username, request);
        Instant now = Instant.now();
        attempts.computeIfPresent(key, (ignored, state) -> state.isStale(now, lockoutDuration) ? null : state);
        AttemptState state = attempts.get(key);
        return state != null && state.isLocked(now);
    }

    public Duration remainingLockout(String username, HttpServletRequest request) {
        AttemptState state = attempts.get(keyFor(username, request));
        if (state == null || state.lockedUntil() == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(Instant.now(), state.lockedUntil());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public void recordFailure(String username, HttpServletRequest request) {
        String key = keyFor(username, request);
        Instant now = Instant.now();
        attempts.compute(key, (ignored, state) -> {
            if (state != null && state.isLocked(now)) {
                return state;
            }

            int failures = state == null || state.isStale(now, lockoutDuration) ? 1 : state.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(lockoutDuration) : null;
            return new AttemptState(failures, lockedUntil, now);
        });
    }

    public void recordSuccess(String username, HttpServletRequest request) {
        attempts.remove(keyFor(username, request));
    }

    void clearAttempts() {
        attempts.clear();
    }

    private String keyFor(String username, HttpServletRequest request) {
        return normalizeUsername(username) + "|" + resolveClientIp(request);
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private record AttemptState(int failures, Instant lockedUntil, Instant lastFailureAt) {

        boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean isStale(Instant now, Duration lockoutDuration) {
            if (isLocked(now)) {
                return false;
            }
            return lastFailureAt.plus(lockoutDuration).isBefore(now);
        }
    }
}
