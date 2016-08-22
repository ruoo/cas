package org.apereo.cas.support.saml.web.idp.profile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apereo.cas.support.saml.SamlIdPConstants;
import org.apereo.cas.support.saml.SamlProtocolConstants;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlRegisteredServiceServiceProviderMetadataFacade;
import org.jasig.cas.client.util.CommonUtils;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * This is {@link IdPInitiatedProfileHandlerController}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public class IdPInitiatedProfileHandlerController extends AbstractSamlProfileHandlerController {
    
    public IdPInitiatedProfileHandlerController() {
    }

    /**
     * Handle idp initiated sso requests.
     *
     * @param response the response
     * @param request  the request
     * @throws Exception the exception
     */
    @RequestMapping(path = SamlIdPConstants.ENDPOINT_SAML2_IDP_INIT_PROFILE_SSO, method = RequestMethod.GET)
    protected void handleIdPInitiatedSsoRequest(final HttpServletResponse response,
                                                final HttpServletRequest request) throws Exception {

        // The name (i.e., the entity ID) of the service provider.
        final String providerId = CommonUtils.safeGetParameter(request, "providerId");
        if (StringUtils.isBlank(providerId)) {
            logger.warn("No providerId parameter given in unsolicited SSO authentication request.");
            throw new MessageDecodingException("No providerId parameter given in unsolicited SSO authentication request.");
        }
        
        final SamlRegisteredService registeredService = verifySamlRegisteredService(providerId);
        final SamlRegisteredServiceServiceProviderMetadataFacade adaptor = getSamlMetadataFacadeFor(registeredService, providerId);
        
        // The URL of the response location at the SP (called the "Assertion Consumer Service")
        // but can be omitted in favor of the IdP picking the default endpoint location from metadata.
        String shire = CommonUtils.safeGetParameter(request, "shire");
        if (StringUtils.isBlank(shire)) {
            shire = adaptor.getAssertionConsumerService().getLocation();
        }
        if (StringUtils.isBlank(shire)) {
            logger.warn("Unable to resolve SP ACS URL for AuthnRequest construction for entityID: {}", providerId);
            throw new MessageDecodingException("Unable to resolve SP ACS URL for AuthnRequest construction");
        }
        
        // The target resource at the SP, or a state token generated by an SP to represent the resource.
        final String target = CommonUtils.safeGetParameter(request, "target");
        
        // A timestamp to help with stale request detection.
        final String time = CommonUtils.safeGetParameter(request, "time");
        
        final SAMLObjectBuilder builder = (SAMLObjectBuilder) configBean.getBuilderFactory().getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);
        final AuthnRequest authnRequest = (AuthnRequest) builder.buildObject();
        authnRequest.setAssertionConsumerServiceURL(shire);

        final SAMLObjectBuilder isBuilder = (SAMLObjectBuilder) configBean.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        final Issuer issuer = (Issuer) isBuilder.buildObject();
        issuer.setValue(providerId);
        authnRequest.setIssuer(issuer);
        
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        final SAMLObjectBuilder pBuilder = (SAMLObjectBuilder) configBean.getBuilderFactory().getBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        final NameIDPolicy nameIDPolicy = (NameIDPolicy) pBuilder.buildObject();
        nameIDPolicy.setAllowCreate(Boolean.valueOf(true));
        authnRequest.setNameIDPolicy(nameIDPolicy);

        final String id = "_" + String.valueOf(Math.abs(new SecureRandom().nextLong()));
        if (NumberUtils.isNumber(time)) {
            authnRequest.setID(id + time);
            authnRequest.setIssueInstant(new DateTime(TimeUnit.SECONDS.convert(Long.parseLong(time), TimeUnit.MILLISECONDS), 
                    ISOChronology.getInstanceUTC()));
        } else {
            authnRequest.setID(id);
            authnRequest.setIssueInstant(new DateTime(DateTime.now(), ISOChronology.getInstanceUTC()));
        }
        authnRequest.setForceAuthn(false);

        if (StringUtils.isNotBlank(target)) {
            request.setAttribute(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE, target);
        }
        initiateAuthenticationRequest(authnRequest, response, request);
    }
}