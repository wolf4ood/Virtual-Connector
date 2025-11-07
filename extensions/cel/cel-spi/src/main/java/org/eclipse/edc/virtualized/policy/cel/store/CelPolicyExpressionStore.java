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

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.virtualized.policy.cel.model.CelPolicyExpression;

import java.util.List;

public interface CelPolicyExpressionStore {

    StoreResult<Void> save(CelPolicyExpression expression);

    StoreResult<Void> update(CelPolicyExpression expression);

    StoreResult<Void> delete(String id);

    List<CelPolicyExpression> query(QuerySpec querySpec);

}
