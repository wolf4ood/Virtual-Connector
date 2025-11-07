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

package org.eclipse.edc.virtualized.policy.cel.store;

import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.eclipse.edc.virtualized.policy.cel.model.CelPolicyExpression;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryCelPolicyExpressionStore implements CelPolicyExpressionStore {

    private final QueryResolver<CelPolicyExpression> queryResolver;
    private final Map<String, CelPolicyExpression> expressions = new ConcurrentHashMap<>();

    public InMemoryCelPolicyExpressionStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(CelPolicyExpression.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<Void> save(CelPolicyExpression expression) {
        return expressions.putIfAbsent(expression.id(), expression) == null
                ? StoreResult.success()
                : StoreResult.alreadyExists("CelPolicyExpression with id " + expression.id() + " already exists");
    }

    @Override
    public StoreResult<Void> update(CelPolicyExpression expression) {
        return expressions.replace(expression.id(), expression) != null
                ? StoreResult.success()
                : StoreResult.notFound("CelPolicyExpression with id " + expression.id() + " not found");
    }

    @Override
    public StoreResult<Void> delete(String id) {
        return expressions.remove(id) != null
                ? StoreResult.success()
                : StoreResult.notFound("CelPolicyExpression with id " + id + " not found");
    }

    @Override
    public List<CelPolicyExpression> query(QuerySpec querySpec) {
        return queryResolver.query(expressions.values().stream(), querySpec)
                .collect(Collectors.toList());
    }
}
