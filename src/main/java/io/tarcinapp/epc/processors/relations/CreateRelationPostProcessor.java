package io.tarcinapp.epc.processors.relations;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /relations (createRelation).
 * Releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateRelationPostProcessor extends AbstractWriteLockPostProcessor {}
