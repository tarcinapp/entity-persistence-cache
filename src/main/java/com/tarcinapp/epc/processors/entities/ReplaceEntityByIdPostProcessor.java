package com.tarcinapp.epc.processors.entities;

import com.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PUT /entities/{id} (replaceEntityById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class ReplaceEntityByIdPostProcessor extends AbstractWriteLockPostProcessor {}
