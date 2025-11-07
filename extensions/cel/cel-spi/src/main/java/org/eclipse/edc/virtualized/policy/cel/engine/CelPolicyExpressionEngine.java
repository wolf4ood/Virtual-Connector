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

package org.eclipse.edc.virtualized.policy.cel.engine;

import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Map;


public interface CelPolicyExpressionEngine {

    boolean canEvaluate(String leftOperand);

    ServiceResult<Void> validate(String expression);

    boolean evaluateExpression(Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params);
}
