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
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.virtualized.policy.cel.model.CelPolicyExpression;
import org.eclipse.edc.virtualized.policy.cel.store.CelPolicyExpressionStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CelPolicyExpressionEngineImplTest {
    private final CelPolicyExpressionStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final CelPolicyExpressionEngineImpl registry = new CelPolicyExpressionEngineImpl(transactionContext, store, mock());


    @Test
    void shouldEvaluate_simpleExpression() {
        when(store.query(any())).thenReturn(List.of(expression("agent.id == 'agent-123'")));


        Map<String, Object> claims = Map.of("agent", Map.of("id", "agent-123"));
        var result = registry.evaluateExpression("test", Operator.EQ, "null", claims);

        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluate_credential() {

        var params = createParams("agent-123");

        var expression = """
                agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", params);

        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluate_credential_withMultipleConditions() {

        var params = createParams("agent-123");

        var expression = """
                agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now && cs.membershipType == 'gold'))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", params);

        assertThat(result).isTrue();
    }

    @Test
    void shouldFailToEvaluate_whenMissingKeys() {

        var params = createParams("agent-123");

        var expression = """
                agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now && cs.missingClaim == 'gold'))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, "null", params);

        assertThat(result).isFalse();
    }


    @ParameterizedTest
    @ArgumentsSource(InputProvider.class)
    void shouldEvaluate_withInput(String rightOperand, boolean expectedEvaluation) {

        var params = createParams("agent-123");

        var expression = """
                agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) > timestamp(input)))
                """;

        when(store.query(any())).thenReturn(List.of(expression(expression)));

        var result = registry.evaluateExpression("test", Operator.EQ, rightOperand, params);

        assertThat(result).isEqualTo(expectedEvaluation);
    }

    private CelPolicyExpression expression(String expr) {
        return new CelPolicyExpression("id", "test", expr, "desc");
    }

    private @NotNull Map<String, Object> credential() {
        return Map.of(
                "id", "credential-456",
                "type", List.of("VerifiableCredential", "MembershipCredential"),
                "credentialSubject", List.of(Map.of(
                        "id", "subject-789",
                        "membershipStartDate", "2023-01-01T00:00:00Z",
                        "membershipType", "gold"
                )));
    }

    private Map<String, Object> createParams(String id) {
        return Map.of("agent", createAgentParams(id));
    }

    private @NotNull Map<String, Object> createAgentParams(String id) {

        return Map.of("id", id,
                "claims", Map.of("vc", List.of(credential())));

    }


    private static class InputProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments("2022-01-01T00:00:00Z", true),
                    arguments("2024-01-01T00:00:00Z", false)
            );
        }
    }


}
