package com.tarcinapp.epc.processors.relations;

import com.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for DELETE /relations/{id} (deleteRelationById).
 * Releases any acquired locks (none in practice since DELETE bypasses lock acquisition).
 */
@ApplicationScoped
public class DeleteRelationByIdPostProcessor extends AbstractWriteLockPostProcessor {}
