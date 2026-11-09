package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.cache.AbstractCreateCacheEvictPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /entity-reactions (createEntityReaction).
 * Phase 2: evicts any stale 404 cache entry for the newly created record's _id.
 * Phase 1: releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateEntityReactionPostProcessor extends AbstractCreateCacheEvictPostProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
