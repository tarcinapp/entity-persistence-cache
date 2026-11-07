package io.tarcinapp.epc.processors.entities;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for POST /entities (createEntity).
 * Releases all scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class CreateEntityPostProcessor extends AbstractWriteLockPostProcessor {}
