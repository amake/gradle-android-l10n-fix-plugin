package org.madlonkay.testlibrary;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class ResourceTest {
    @Test
    public void resourcePresent() {
        assertNotEquals(0, TestLibrary.resource);
    }
}
