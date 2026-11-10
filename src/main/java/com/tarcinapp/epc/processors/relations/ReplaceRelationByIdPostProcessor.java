package com.tarcinapp.epc.processors.relations;

import com.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for PUT /relations/{id} (replaceRelationById).
 * Releases scope locks acquired by the matching pre-processor.
 */
@ApplicationScoped
public class ReplaceRelationByIdPostProcessor extends AbstractWriteLockPostProcessor {}
