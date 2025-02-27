package dk.gov.oio.saml.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensaml.core.config.InitializationException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPRedirectDeflateDecoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;

import dk.gov.oio.saml.util.ExternalException;
import dk.gov.oio.saml.util.InternalException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.velocity.VelocityEngine;

public abstract class SAMLHandler {
    private static final Logger log = LoggerFactory.getLogger(SAMLHandler.class);

    public abstract void handleGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ExternalException, InternalException, InitializationException;
    public abstract void handlePost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ExternalException, InternalException, IOException;

    MessageContext<SAMLObject> decodeGet(HttpServletRequest httpServletRequest) throws InternalException, ExternalException {
        try {
            log.debug("Decoding message as HTTPRedirect");

            HTTPRedirectDeflateDecoder decoder = new HTTPRedirectDeflateDecoder();
            decoder.setHttpServletRequest(httpServletRequest);

            decoder.initialize();
            decoder.decode();
            return decoder.getMessageContext();
        }
        catch (ComponentInitializationException e) {
            throw new InternalException("Could not initialize decoder", e);
        }
        catch (MessageDecodingException e) {
            throw new ExternalException("Could not decode request", e);
        }
    }

    MessageContext<SAMLObject> decodePost(HttpServletRequest httpServletRequest) throws InternalException, ExternalException {
        try {
            log.debug("Decoding message as HTTP Post");

            HTTPPostDecoder decoder = new HTTPPostDecoder();
            decoder.setHttpServletRequest(httpServletRequest);

            decoder.initialize();
            decoder.decode();
            return decoder.getMessageContext();
        }
        catch (ComponentInitializationException e) {
            throw new InternalException("Could not initialize decoder", e);
        }
        catch (MessageDecodingException e) {
            throw new ExternalException("Could not decode request", e);
        }
    }

    void sendGet(HttpServletResponse httpServletResponse, MessageContext<SAMLObject> message) throws ComponentInitializationException, MessageEncodingException {
        log.debug("Encoding, deflating and sending message (HTTPRedirect)");

        HTTPRedirectDeflateEncoder encoder = new HTTPRedirectDeflateEncoder();

        encoder.setHttpServletResponse(httpServletResponse);
        encoder.setMessageContext(message);

        encoder.initialize();
        encoder.encode();
    }

    void sendPost(HttpServletResponse httpServletResponse, MessageContext<SAMLObject> message) throws ComponentInitializationException, MessageEncodingException {
        log.debug("Encoding and sending message (HTTPPost)");

        HTTPPostEncoder encoder = new HTTPPostEncoder();

        encoder.setHttpServletResponse(httpServletResponse);
        encoder.setMessageContext(message);
        encoder.setVelocityEngine(VelocityEngine.newVelocityEngine());

        encoder.initialize();
        encoder.encode();
    }

    <T> T getSamlObject(MessageContext<SAMLObject> context, Class<T> clazz) throws ExternalException {
        SAMLObject samlObject = context.getMessage();
        if (samlObject == null) {
            throw new ExternalException("Saml message was null");
        }

        try {
            return clazz.cast(samlObject);
        } catch (ClassCastException e) {
            throw new ExternalException("Saml message was of the wrong type", e);
        }
    }
}
