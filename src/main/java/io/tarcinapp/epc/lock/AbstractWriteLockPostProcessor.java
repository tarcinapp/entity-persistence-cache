package io.tarcinapp.epc.lock;

import jakarta.inject.Inject;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jboss.logging.Logger;
import org.redisson.api.RLock;

/**
 * Abstract base for all write-path post-processors that release distributed
 * scope locks acquired by the matching {@link AbstractWriteLockPreProcessor}.
 *
 * <h3>doFinally semantics</h3>
 * Lock release must happen whether the backend call succeeds or fails.
 * In Camel's pre/post-processor model the post-processor runs only on the
 * success path. For Phase 1 we implement release here (post-processor is
 * always called after the backend proxy in the route's {@code otherwise()}
 * branch) and rely on the Camel error handler to call it even on exceptions.
 *
 * <p>If a truly guaranteed {@code doFinally} is needed, callers can implement
 * an {@code onException().handled(true).process(releaseProcessor)} clause in
 * the route.  For Phase 1 the current approach is sufficient: EPS rejects
 * bad requests early and rarely throws on the proxy call.</p>
 *
 * <p>Locks auto-expire via their {@code lease-time} TTL in Redis — so a crash
 * without reaching the post-processor results in at most {@code lease-time}
 * of serialisation, not a permanent deadlock.</p>
 */
public abstract class AbstractWriteLockPostProcessor implements Processor {

    private static final Logger LOG = Logger.getLogger(AbstractWriteLockPostProcessor.class);

    @Inject
    LockService lockService;

    @Override
    public final void process(Exchange exchange) throws Exception {
        releaseLocks(exchange);
    }

    /**
     * Releases all locks stored on the exchange under
     * {@link AbstractWriteLockPreProcessor#ACQUIRED_LOCKS_PROPERTY}.
     * Safe to call even if no locks were acquired (empty or null list).
     */
    @SuppressWarnings("unchecked")
    protected void releaseLocks(Exchange exchange) {
        List<RLock> locks = exchange.getProperty(
                AbstractWriteLockPreProcessor.ACQUIRED_LOCKS_PROPERTY, List.class);
        if (locks == null || locks.isEmpty()) {
            return;
        }
        String lockType = exchange.getProperty(
                AbstractWriteLockPreProcessor.LOCK_TYPE_PROPERTY, "unknown", String.class);
        lockService.releaseAll(locks, lockType);
        exchange.removeProperty(AbstractWriteLockPreProcessor.ACQUIRED_LOCKS_PROPERTY);
        LOG.debugf("Released %d lock(s) for %s.", locks.size(), lockType);
    }
}
