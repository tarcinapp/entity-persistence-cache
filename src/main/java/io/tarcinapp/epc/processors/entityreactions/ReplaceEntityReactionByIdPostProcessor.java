package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PUT /entityreactions/{id} (replaceEntityReactionById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class ReplaceEntityReactionByIdPostProcessor extends AbstractWriteLockPostProcessor {}
