package io.tarcinapp.epc;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.VertxMDC;
import io.quarkus.vertx.mdc.provider.LateBoundMDCProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDCProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * MDCProvider that works correctly on Vert.x worker threads.
 *
 * <p>Root problem: VertxMDC.getContext() calls
 * {@code VertxContext.getOrCreateDuplicatedContext(currentContext)}.
 * On a Vert.x worker thread the current context is a root {@code ContextImpl}
 * (not a {@code DuplicatedContext}), so every call to
 * {@code getOrCreateDuplicatedContext} calls {@code rootCtx.duplicate()}
 * which creates a brand-new {@code DuplicatedContext} with its own empty
 * {@code AtomicReferenceArray}.  Put and get therefore operate on different
 * arrays and the value is never visible.
 *
 * <p>Fix: maintain a per-thread map in an {@code InheritableThreadLocal} as a
 * parallel store.  All writes go to both the Vert.x context map (for
 * event-loop threads where VertxMDC works correctly) AND the thread-local.
 * All reads try the Vert.x context first; if the value is missing they fall
 * back to the thread-local.  This makes the bracket work on worker threads
 * without breaking event-loop behaviour.
 */
@ApplicationScoped
public class FallbackMdcProvider implements MDCProvider {

    private static final Logger LOG = Logger.getLogger(FallbackMdcProvider.class);

    private static final InheritableThreadLocal<Map<String, Object>> THREAD_LOCAL =
            new InheritableThreadLocal<>() {
                @Override
                protected Map<String, Object> initialValue() {
                    return new HashMap<>();
                }

                @Override
                protected Map<String, Object> childValue(Map<String, Object> parentValue) {
                    return parentValue == null ? new HashMap<>() : new HashMap<>(parentValue);
                }
            };

    /** Called once at startup — replaces VertxMDC as the active MDCProvider. */
    void onStart(@Observes StartupEvent ev) {
        LateBoundMDCProvider.setMDCProviderDelegate(this);
        LOG.infof("FallbackMdcProvider registered as MDCProvider delegate.");
    }

    // -------------------------------------------------------------------------
    // Write operations — store in both Vert.x context map and thread-local
    // -------------------------------------------------------------------------

    @Override
    public String put(String key, String value) {
        THREAD_LOCAL.get().put(key, value);
        return VertxMDC.INSTANCE.put(key, value);
    }

    @Override
    public Object putObject(String key, Object value) {
        THREAD_LOCAL.get().put(key, value);
        return VertxMDC.INSTANCE.putObject(key, value);
    }

    // -------------------------------------------------------------------------
    // Read operations — try Vert.x context first, fall back to thread-local
    // -------------------------------------------------------------------------

    @Override
    public String get(String key) {
        String val = VertxMDC.INSTANCE.get(key);
        if (val != null) return val;
        Object tl = THREAD_LOCAL.get().get(key);
        return tl == null ? null : tl.toString();
    }

    @Override
    public Object getObject(String key) {
        Object val = VertxMDC.INSTANCE.getObject(key);
        if (val != null) return val;
        return THREAD_LOCAL.get().get(key);
    }

    // -------------------------------------------------------------------------
    // Remove operations
    // -------------------------------------------------------------------------

    @Override
    public String remove(String key) {
        THREAD_LOCAL.get().remove(key);
        return VertxMDC.INSTANCE.remove(key);
    }

    @Override
    public Object removeObject(String key) {
        THREAD_LOCAL.get().remove(key);
        return VertxMDC.INSTANCE.removeObject(key);
    }

    // -------------------------------------------------------------------------
    // Bulk operations
    // -------------------------------------------------------------------------

    @Override
    public Map<String, String> copy() {
        Map<String, String> merged = new HashMap<>();
        // Thread-local first (lower priority)
        for (Map.Entry<String, Object> e : THREAD_LOCAL.get().entrySet()) {
            if (e.getValue() != null) merged.put(e.getKey(), e.getValue().toString());
        }
        // Vert.x context overrides (higher priority)
        merged.putAll(VertxMDC.INSTANCE.copy());
        return merged;
    }

    @Override
    public Map<String, Object> copyObject() {
        Map<String, Object> merged = new HashMap<>(THREAD_LOCAL.get());
        merged.putAll(VertxMDC.INSTANCE.copyObject());
        return merged;
    }

    @Override
    public void clear() {
        THREAD_LOCAL.get().clear();
        VertxMDC.INSTANCE.clear();
    }

    @Override
    public boolean isEmpty() {
        return THREAD_LOCAL.get().isEmpty() && VertxMDC.INSTANCE.isEmpty();
    }

}
