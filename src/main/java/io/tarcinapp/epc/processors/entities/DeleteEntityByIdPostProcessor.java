package io.tarcinapp.epc.processors.entities;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for DELETE /entities/{id} (deleteEntityById).
 * Releases any acquired locks (none in practice since DELETE bypasses lock acquisition).
 */
@ApplicationScoped
public class DeleteEntityByIdPostProcessor extends AbstractWriteLockPostProcessor {}
