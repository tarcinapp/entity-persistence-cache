package io.tarcinapp.epc.processors.entities;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PATCH /entities/{id} (updateEntityById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class UpdateEntityByIdPostProcessor extends AbstractWriteLockPostProcessor {}
