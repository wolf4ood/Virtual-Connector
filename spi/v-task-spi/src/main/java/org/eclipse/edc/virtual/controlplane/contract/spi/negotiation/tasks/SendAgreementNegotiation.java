/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.virtual.controlplane.contract.spi.negotiation.tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.virtual.controlplane.tasks.ProcessTaskPayload;

@JsonDeserialize(builder = SendAgreementNegotiation.Builder.class)
public class SendAgreementNegotiation extends ContractNegotiationTaskPayload {

    private SendAgreementNegotiation() {
    }

    @Override
    public String name() {
        return "negotiation.agreement.send";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SendAgreementNegotiation, Builder> {

        private Builder() {
            super(new SendAgreementNegotiation());
        }

        @JsonCreator
        public static SendAgreementNegotiation.Builder newInstance() {
            return new SendAgreementNegotiation.Builder();
        }

        @Override
        public SendAgreementNegotiation.Builder self() {
            return this;
        }
    }
}
