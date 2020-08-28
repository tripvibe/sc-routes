package com.redhat.labs.tripvibe.util;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TestSignature {

    @Test
    void testSignature() {
        Signature signature = new Signature();
        signature.developerId = "devid";
        signature.privateKey = "apikey";
        Assertions.assertTrue(signature.generate("foobar").contains("1CF31532AE3528CBF0361C3FD785938BF30D418D"));
    }
}
