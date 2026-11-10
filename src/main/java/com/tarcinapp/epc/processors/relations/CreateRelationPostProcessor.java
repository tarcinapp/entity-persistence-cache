package com.tarcinapp.epc.processors.relations;

import com.tarcinapp.epc.cache.AbstractCreateCacheEvictPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /relations (createRelation).
 * Phase 2: evicts any stale 404 cache entry for the newly created record's _id.
 * Phase 1: releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateRelationPostProcessor extends AbstractCreateCacheEvictPostProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
