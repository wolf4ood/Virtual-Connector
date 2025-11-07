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

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.internal.ProtoTimeUtils;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.virtualized.policy.cel.model.CelPolicyExpression;
import org.eclipse.edc.virtualized.policy.cel.store.CelPolicyExpressionStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CelPolicyExpressionEngineImpl implements CelPolicyExpressionEngine {

    private final TransactionContext ctx;
    private final CelPolicyExpressionStore store;
    private final Monitor monitor;

    private final CelCompiler celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
            .addVar("agent", SimpleType.DYN)
            .addVar("operator", SimpleType.STRING)
            .addVar("input", SimpleType.DYN)
            .addVar("now", SimpleType.TIMESTAMP)
            .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
            .setResultType(SimpleType.BOOL)
            .build();


    private final CelRuntime celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();

    public CelPolicyExpressionEngineImpl(TransactionContext ctx, CelPolicyExpressionStore store, Monitor monitor) {
        this.ctx = ctx;
        this.store = store;
        this.monitor = monitor;
    }

    @Override
    public ServiceResult<Void> validate(String expression) {
        try {
            compile(expression);
            return ServiceResult.success();
        } catch (IllegalArgumentException e) {
            return ServiceResult.badRequest(e.getMessage());
        }
    }

    @Override
    public boolean canEvaluate(String leftOperand) {
        return !fetch(leftOperand).isEmpty();
    }

    @Override
    public boolean evaluateExpression(Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params) {
        var expressions = fetchAndCompile(leftOperand.toString());
        if (expressions.isEmpty()) {
            monitor.severe("No expressions registered for left operand: " + leftOperand);
            return false;
        }
        var result = true;
        for (var ast : expressions) {
            try {
                result = evaluateAst(ast, operator, rightOperand, params);
                if (!result) {
                    break;
                }
            } catch (IllegalArgumentException e) {
                monitor.severe("Failed to evaluate expression for left operand: " + leftOperand + ". Reason: " + e.getMessage());
                return false;
            }

        }
        return result;
    }

    private Boolean evaluateAst(CelAbstractSyntaxTree ast, Operator operator, Object rightOperand, Map<String, Object> params) {
        try {
            var program = celRuntime.createProgram(ast);
            var newParams = new HashMap<>(params);
            newParams.put("now", ProtoTimeUtils.now());
            newParams.put("operator", operator.name());
            newParams.put("input", rightOperand);

            return (Boolean) program.eval(newParams);
        } catch (CelEvaluationException e) {
            // Report any evaluation errors, if present
            throw new IllegalArgumentException(
                    "Evaluation error has occurred. Reason: " + e.getMessage(), e);
        }
    }

    private List<CelAbstractSyntaxTree> fetchAndCompile(String leftOperand) {
        return fetch(leftOperand).stream()
                .map(expr -> compile(expr.expression()))
                .toList();
    }

    private List<CelPolicyExpression> fetch(String leftOperand) {
        return ctx.execute(() -> store.query(QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("leftOperand", "=", leftOperand))
                .build()));

    }

    private CelAbstractSyntaxTree compile(String expression) {
        CelAbstractSyntaxTree ast;
        try {
            // Parse the expression
            ast = celCompiler.parse(expression).getAst();
        } catch (CelValidationException e) {
            // Report syntactic errors, if present
            throw new IllegalArgumentException(
                    "Failed to parse expression. Reason: " + e.getMessage(), e);
        }

        try {
            // Type-check the expression for correctness
            ast = celCompiler.check(ast).getAst();
        } catch (CelValidationException e) {
            // Report semantic errors, if present.
            throw new IllegalArgumentException(
                    "Failed to type-check expression. Reason: " + e.getMessage(), e);
        }
        return ast;
    }
}
