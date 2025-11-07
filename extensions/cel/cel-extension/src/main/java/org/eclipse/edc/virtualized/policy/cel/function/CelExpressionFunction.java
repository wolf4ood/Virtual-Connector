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

package org.eclipse.edc.virtualized.policy.cel.function;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.virtualized.policy.cel.engine.CelPolicyExpressionEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CelExpressionFunction<C extends ParticipantAgentPolicyContext>(
        CelPolicyExpressionEngine engine) implements DynamicAtomicConstraintRuleFunction<Permission, C> {


    @Override
    public boolean evaluate(Object leftOperand, Operator operator, Object rightOperand, Permission permission, C c) {
        return engine.evaluateExpression(leftOperand.toString(), operator, rightOperand, toParams(c));
    }

    @Override
    public boolean canHandle(Object leftOperand) {
        return engine.canEvaluate(leftOperand.toString());
    }

    private Map<String, Object> toParams(C context) {
        return Map.of("agent", Map.ofEntries(
                Map.entry("id", context.participantAgent().getIdentity()),
                Map.entry("attributes", context.participantAgent().getAttributes()),
                Map.entry("claims", toClaimsMap(context.participantAgent().getClaims()))
        ));
    }


    private Map<String, Object> toClaimsMap(Map<String, Object> claims) {
        var mappedClaims = new HashMap<>(claims);
        mappedClaims.put("vc", toVcList(claims.get("vc")));
        return mappedClaims;
    }

    private List<Map<String, Object>> toVcList(Object vcClaim) {
        if (vcClaim instanceof List<?> vcList) {
            return vcList.stream()
                    .filter(item -> item instanceof VerifiableCredential)
                    .map(item -> toMap((VerifiableCredential) item))
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> toMap(VerifiableCredential credential) {
        var cred = new HashMap<String, Object>();
        cred.put("id", credential.getId());
        cred.put("type", credential.getType());
        cred.put("credentialSubject", credential.getCredentialSubject().stream().map(this::toMap).collect(Collectors.toList()));
        cred.put("issuer", credential.getIssuer().id());
        cred.put("issuanceDate", credential.getIssuanceDate());
        return cred;
    }

    private Map<String, Object> toMap(CredentialSubject subject) {
        return subject.getClaims();
    }

}
