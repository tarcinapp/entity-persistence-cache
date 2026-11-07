package io.tarcinapp.epc.processors.lists;

import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PATCH /lists/{id} (updateListById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class UpdateListByIdPostProcessor extends AbstractWriteLockPostProcessor {}
