package io.tarcinapp.epc.processors.listreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /listreactions (createListReaction).
 * Releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateListReactionPostProcessor extends AbstractWriteLockPostProcessor {}
