package org.apereo.cas.adaptors.duo.web.flow;

import org.apereo.cas.web.flow.AbstractCasWebflowConfigurer;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;

/**
 * This is {@link DuoMultifactorTrustWebflowConfigurer}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
public class DuoMultifactorTrustWebflowConfigurer extends AbstractCasWebflowConfigurer {

    private FlowDefinitionRegistry flowDefinitionRegistry;

    @Override
    protected void doInitialize() throws Exception {
        registerMultifactorTrustedAuthenticationIntoWebflow(flowDefinitionRegistry, DuoMultifactorWebflowConfigurer.MFA_DUO_EVENT_ID);
    }

    public void setFlowDefinitionRegistry(final FlowDefinitionRegistry flowDefinitionRegistry) {
        this.flowDefinitionRegistry = flowDefinitionRegistry;
    }
}