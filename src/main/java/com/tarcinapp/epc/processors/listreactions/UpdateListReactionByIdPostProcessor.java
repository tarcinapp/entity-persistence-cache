package com.tarcinapp.epc.processors.listreactions;

import com.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PATCH /listreactions/{id} (updateListReactionById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class UpdateListReactionByIdPostProcessor extends AbstractWriteLockPostProcessor {}
