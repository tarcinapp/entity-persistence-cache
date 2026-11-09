package io.tarcinapp.epc.processors.listreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for DELETE /list-reactions/{id} (deleteListReactionById).
 * Releases any acquired locks (none in practice since DELETE bypasses lock acquisition).
 */
@ApplicationScoped
public class DeleteListReactionByIdPostProcessor extends AbstractWriteLockPostProcessor {}
