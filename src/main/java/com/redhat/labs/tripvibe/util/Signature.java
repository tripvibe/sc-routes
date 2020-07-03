package com.redhat.labs.tripvibe.util;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

@ApplicationScoped
public class Signature {

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
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        byte[] signatureBytes = mac.doFinal(uriBytes);
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
