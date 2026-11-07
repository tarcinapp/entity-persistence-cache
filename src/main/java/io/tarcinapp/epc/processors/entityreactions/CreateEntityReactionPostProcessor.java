package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /entityreactions (createEntityReaction).
 * Releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateEntityReactionPostProcessor extends AbstractWriteLockPostProcessor {}
