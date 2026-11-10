package com.tarcinapp.epc.lock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * CDI bean that manages Redis distributed scope locks for write operations.
 *
 * <h3>Non-blocking requirement</h3>
 * EPC runs on the Quarkus Vert.x event loop. All lock operations use
 * Redisson's {@code RLock.tryLock(waitTime, leaseTime, unit)} in a way
 * that is called from within Camel processor threads.  Camel processor
 * threads are <em>not</em> Vert.x event-loop threads — they are worker
 * threads managed by the Camel thread pool.  Therefore blocking
 * {@code tryLock} is acceptable here.
 *
 * <p>If EPC is ever moved to a fully reactive Camel setup, replace with
 * {@code RLockReactive.tryLockAsync(waitTime, leaseTime, unit)}.</p>
 *
 * <h3>Redis unavailability</h3>
 * Lock acquisition is required for correctness. If Redis is unavailable,
 * this service throws a {@link RedisUnavailableException} which the
 * processor maps to HTTP 503.
 *
 * <h3>Metrics</h3>
 * <ul>
 *   <li>{@code epc.lock.acquired} — counter, tag {@code lock_type}</li>
 *   <li>{@code epc.lock.released} — counter, tag {@code lock_type}</li>
 *   <li>{@code epc.lock.timeout}  — counter, tag {@code lock_type}</li>
 *   <li>{@code epc.lock.wait_time} — timer, tag {@code lock_type}</li>
 *   <li>{@code epc.lock.held_time} — timer, tag {@code lock_type}</li>
 * </ul>
 */
@ApplicationScoped
public class LockService {

    private static final Logger LOG = Logger.getLogger(LockService.class);

    @ConfigProperty(name = "epc.lock.wait-time", defaultValue = "3000")
    long waitTimeMs;

    @ConfigProperty(name = "epc.lock.lease-time", defaultValue = "30000")
    long leaseTimeMs;

    @ConfigProperty(name = "epc.redis.url", defaultValue = "redis://localhost:6379")
    String redisUrl;

    @Inject
    MeterRegistry meterRegistry;

    private RedissonClient redisson;

    @PostConstruct
    void init() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress(redisUrl)
              .setConnectionMinimumIdleSize(1)
              .setConnectionPoolSize(10);
        redisson = Redisson.create(config);
        LOG.infof("LockService initialised with Redis at %s (wait=%dms, lease=%dms).",
                redisUrl, waitTimeMs, leaseTimeMs);
    }

    /**
     * Acquires all locks in the provided list, in order.
     *
     * <p>If any lock cannot be acquired within {@code wait-time}, all
     * already-acquired locks are released and a {@link LockTimeoutException}
     * is thrown — the caller should return HTTP 423 Locked.</p>
     *
     * <p>If Redis is unreachable, a {@link RedisUnavailableException} is
     * thrown — the caller should return HTTP 503.</p>
     *
     * @param lockKeys  ordered list of Redis lock key strings
     * @param lockType  label for metrics ("uniqueness", "limit", "global")
     * @return list of acquired RLock handles (must be passed to {@link #releaseAll})
     */
    public List<RLock> acquireAll(List<String> lockKeys, String lockType) {
        List<RLock> acquired = new ArrayList<>();
        Timer.Sample sample = Timer.start(meterRegistry);

        for (String key : lockKeys) {
            LOG.debugf("Acquiring lock: %s", key);
            try {
                RLock lock = redisson.getLock(key);
                boolean gotLock = lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS);
                if (!gotLock) {
                    // Release all already-acquired locks and signal 423
                    LOG.warnf("Lock timeout for key '%s' (lock_type=%s) — releasing %d already-held locks.",
                              key, lockType, acquired.size());
                    releaseAll(acquired, lockType);
                    sample.stop(Timer.builder("epc.lock.wait_time")
                            .tag("lock_type", lockType).register(meterRegistry));
                    counter("epc.lock.timeout", lockType).increment();
                    throw new LockTimeoutException(
                            "Could not acquire lock within " + waitTimeMs + " ms: " + key);
                }
                acquired.add(lock);
                LOG.debugf("Lock acquired: %s", key);
                counter("epc.lock.acquired", lockType).increment();
            } catch (LockTimeoutException e) {
                throw e;
            } catch (Exception e) {
                LOG.errorf(e, "Redis unavailable while acquiring lock '%s'.", key);
                releaseAll(acquired, lockType);
                throw new RedisUnavailableException(
                        "Redis unavailable during lock acquisition: " + e.getMessage(), e);
            }
        }

        sample.stop(Timer.builder("epc.lock.wait_time")
                .tag("lock_type", lockType).register(meterRegistry));
        return acquired;
    }

    /**
     * Releases all locks in the provided list. Safe to call with an empty list.
     * This should be called in a {@code doFinally} block.
     *
     * @param locks    list of acquired RLock handles
     * @param lockType label for metrics
     */
    public void releaseAll(List<RLock> locks, String lockType) {
        for (RLock lock : locks) {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    LOG.debugf("Lock released: %s", lock.getName());
                    counter("epc.lock.released", lockType).increment();
                }
            } catch (Exception e) {
                // Log and continue — we must not throw from a finally block
                LOG.warnf(e, "Failed to release lock '%s'.", lock.getName());
            }
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private Counter counter(String name, String lockType) {
        return Counter.builder(name)
                .tag("lock_type", lockType)
                .register(meterRegistry);
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    /** Thrown when a lock could not be acquired within wait-time → HTTP 423. */
    public static class LockTimeoutException extends RuntimeException {
        public LockTimeoutException(String message) { super(message); }
    }

    /** Thrown when Redis is unreachable → HTTP 503. */
    public static class RedisUnavailableException extends RuntimeException {
        public RedisUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
