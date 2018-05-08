package org.madlonkay.testlibrary2;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class ResourceTest {
    @Test
    public void resourcePresent() {
        assertNotEquals(0, TestLibrary2.resource);
    }
}
