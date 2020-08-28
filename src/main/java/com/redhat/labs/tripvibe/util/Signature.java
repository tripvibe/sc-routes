package com.redhat.labs.tripvibe.util;

import com.redhat.labs.tripvibe.DepartureResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

@ApplicationScoped
public class Signature {

    private final Logger log = LoggerFactory.getLogger(Signature.class);

    @ConfigProperty(name = "com.redhat.labs.tripvibe.developerId")
    public String developerId;

    @ConfigProperty(name = "com.redhat.labs.tripvibe.privateKey")
    public String privateKey;

    @ConfigProperty(name = "baseURL", defaultValue = "https://timetableapi.ptv.vic.gov.au")
    public String baseURL;

    /**
     * Generate signature for PTV Timetable API Call
     *
     * @param uri- Request URI with parameters
     * @return Signature
     * @throws Exception
     */
    public String generate(final String uri) {

        String HMAC_SHA1_ALGORITHM = "HmacSHA1";
        StringBuffer uriWithDeveloperID = new StringBuffer().append(uri).append(uri.contains("?") ? "&" : "?")
                .append("devid=" + developerId);
        byte[] keyBytes = privateKey.getBytes();
        byte[] uriBytes = uriWithDeveloperID.toString().getBytes();
        Key signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1_ALGORITHM);
        byte[] signatureBytes = new byte [0];
        try {
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            signatureBytes = mac.doFinal(uriBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("signature error: " + e.getMessage());
        }
        StringBuffer signature = new StringBuffer(signatureBytes.length * 2);
        for (byte signatureByte : signatureBytes) {
            int intVal = signatureByte & 0xff;
            if (intVal < 0x10) {
                signature.append("0");
            }
            signature.append(Integer.toHexString(intVal));
        }
        return signature.toString().toUpperCase();
    }
}
