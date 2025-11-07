/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.virtualized.policy.cel.service;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.virtualized.policy.cel.engine.CelPolicyExpressionEngine;
import org.eclipse.edc.virtualized.policy.cel.model.CelPolicyExpression;
import org.eclipse.edc.virtualized.policy.cel.store.CelPolicyExpressionStore;

import java.util.List;

public class CelPolicyExpressionServiceImpl implements CelPolicyExpressionService {

    private final CelPolicyExpressionStore store;
    private final TransactionContext tx;
    private final CelPolicyExpressionEngine engine;

    public CelPolicyExpressionServiceImpl(CelPolicyExpressionStore store, TransactionContext tx, CelPolicyExpressionEngine engine) {
        this.store = store;
        this.tx = tx;
        this.engine = engine;
    }

    @Override
    public ServiceResult<Void> save(CelPolicyExpression expression) {
        return tx.execute(() -> {
            var validationResult = engine.validate(expression.expression());
            if (validationResult.failed()) {
                return validationResult;
            }
            var result = store.save(expression);
            if (result.succeeded()) {
                return ServiceResult.success();
            } else {
                return ServiceResult.from(result);
            }
        });
    }

    @Override
    public ServiceResult<Void> update(CelPolicyExpression expression) {
        return tx.execute(() -> {
            var validationResult = engine.validate(expression.expression());
            if (validationResult.failed()) {
                return validationResult;
            }
            var result = store.update(expression);
            if (result.succeeded()) {
                return ServiceResult.success();
            } else {
                return ServiceResult.from(result);
            }
        });
    }

    @Override
    public ServiceResult<Void> delete(String id) {
        return tx.execute(() -> store.delete(id)
                .flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<List<CelPolicyExpression>> query(QuerySpec querySpec) {
        return ServiceResult.success(store.query(querySpec));
    }
}
