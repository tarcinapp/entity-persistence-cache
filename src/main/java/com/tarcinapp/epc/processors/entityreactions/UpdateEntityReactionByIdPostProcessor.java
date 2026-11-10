package com.tarcinapp.epc.processors.entityreactions;

import com.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PATCH /entityreactions/{id} (updateEntityReactionById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class UpdateEntityReactionByIdPostProcessor extends AbstractWriteLockPostProcessor {}
