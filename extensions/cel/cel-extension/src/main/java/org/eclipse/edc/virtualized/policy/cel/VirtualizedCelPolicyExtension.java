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

package org.eclipse.edc.virtualized.policy.cel;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.virtualized.policy.cel.engine.CelPolicyExpressionEngine;
import org.eclipse.edc.virtualized.policy.cel.engine.CelPolicyExpressionEngineImpl;
import org.eclipse.edc.virtualized.policy.cel.function.CelExpressionFunction;
import org.eclipse.edc.virtualized.policy.cel.service.CelPolicyExpressionService;
import org.eclipse.edc.virtualized.policy.cel.service.CelPolicyExpressionServiceImpl;
import org.eclipse.edc.virtualized.policy.cel.store.CelPolicyExpressionStore;

import java.util.Set;

import static org.eclipse.edc.virtualized.policy.cel.VirtualizedCelPolicyExtension.NAME;

@Extension(NAME)
public class VirtualizedCelPolicyExtension implements ServiceExtension {

    public static final String NAME = " EDC-V Common Expression Language Policy Extension";

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private CelPolicyExpressionStore celPolicyExpressionStore;

    @Inject
    private TransactionContext transactionContext;

    private CelPolicyExpressionEngine celPolicyExpressionEngine;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {

        var scopes = Set.of(TransferProcessPolicyContext.TRANSFER_SCOPE, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE,
                CatalogPolicyContext.CATALOG_SCOPE);

        ruleBindingRegistry.dynamicBind((k) -> {
            if (celPolicyExpressionEngine.canEvaluate(k)) {
                return scopes;
            }
            return Set.of();
        });
        bindPermissionFunction(new CelExpressionFunction<>(policyExpressionEngine()), TransferProcessPolicyContext.class);
        bindPermissionFunction(new CelExpressionFunction<>(policyExpressionEngine()), ContractNegotiationPolicyContext.class);
        bindPermissionFunction(new CelExpressionFunction<>(policyExpressionEngine()), CatalogPolicyContext.class);
    }

    @Provider
    public CelPolicyExpressionEngine policyExpressionEngine() {
        if (celPolicyExpressionEngine == null) {
            celPolicyExpressionEngine = new CelPolicyExpressionEngineImpl(transactionContext, celPolicyExpressionStore, monitor);
        }
        return celPolicyExpressionEngine;
    }

    @Provider
    public CelPolicyExpressionService policyExpressionService() {
        return new CelPolicyExpressionServiceImpl(celPolicyExpressionStore, transactionContext, policyExpressionEngine());
    }

    private <C extends PolicyContext> void bindPermissionFunction(DynamicAtomicConstraintRuleFunction<Permission, C> function, Class<C> contextClass) {

        policyEngine.registerFunction(contextClass, Permission.class, function);
    }

}
