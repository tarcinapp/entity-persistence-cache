package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for DELETE /entity-reactions/{id} (deleteEntityReactionById).
 * Releases any acquired locks (none in practice since DELETE bypasses lock acquisition).
 */
@ApplicationScoped
public class DeleteEntityReactionByIdPostProcessor extends AbstractWriteLockPostProcessor {}
