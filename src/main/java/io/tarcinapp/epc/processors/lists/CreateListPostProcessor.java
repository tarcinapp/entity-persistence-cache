package io.tarcinapp.epc.processors.lists;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /lists (createList).
 * Releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateListPostProcessor extends AbstractWriteLockPostProcessor {}
