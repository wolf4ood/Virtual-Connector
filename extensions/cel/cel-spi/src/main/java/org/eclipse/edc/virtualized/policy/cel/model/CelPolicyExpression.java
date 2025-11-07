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

package org.eclipse.edc.virtualized.policy.cel.model;

import java.time.Clock;

public record CelPolicyExpression(String id, String leftOperand, String expression,
                                  String description, Long createdAt, Long updatedAt) {


    public CelPolicyExpression(String id, String leftOperand, String expression, String description) {
        this(id, leftOperand, expression, description, Clock.systemUTC().millis(), Clock.systemUTC().millis());
    }

}
