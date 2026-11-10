package com.tarcinapp.epc.processors.lists;

import com.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for DELETE /lists/{id} (deleteListById).
 * Releases any acquired locks (none in practice since DELETE bypasses lock acquisition).
 */
@ApplicationScoped
public class DeleteListByIdPostProcessor extends AbstractWriteLockPostProcessor {}
