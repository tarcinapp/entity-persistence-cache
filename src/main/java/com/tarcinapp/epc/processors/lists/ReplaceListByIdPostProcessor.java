package com.tarcinapp.epc.processors.lists;

import com.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PUT /lists/{id} (replaceListById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class ReplaceListByIdPostProcessor extends AbstractWriteLockPostProcessor {}
