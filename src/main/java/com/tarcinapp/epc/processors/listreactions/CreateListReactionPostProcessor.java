package com.tarcinapp.epc.processors.listreactions;

import com.tarcinapp.epc.cache.AbstractCreateCacheEvictPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /list-reactions (createListReaction).
 * Phase 2: evicts any stale 404 cache entry for the newly created record's _id.
 * Phase 1: releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateListReactionPostProcessor extends AbstractCreateCacheEvictPostProcessor {
    @Override
    protected String recordType() { return "list-reaction"; }
}
