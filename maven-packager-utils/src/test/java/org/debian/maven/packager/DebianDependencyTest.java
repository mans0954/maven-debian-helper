package org.debian.maven.packager;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.debian.maven.packager.DebianDependency.checkPackageName;
import static org.debian.maven.packager.DebianDependency.isValidDebianPackageName;

public class DebianDependencyTest {

    @Test
    public void testIsValidDebianPackageName() {
        assertTrue(isValidDebianPackageName("a-"));
        assertTrue(isValidDebianPackageName("1-"));
        assertTrue(isValidDebianPackageName("allthecharacters-+.0123456789"));
        assertTrue(isValidDebianPackageName("a-b"));

        assertFalse(isValidDebianPackageName("aaA")); // no uppercase
        assertFalse(isValidDebianPackageName("aa a")); // no space
        assertFalse(isValidDebianPackageName("a")); // minimum 2 characters
        assertFalse(isValidDebianPackageName(""));
        assertFalse(isValidDebianPackageName("-aa")); // must start with alphanumeric 
        assertFalse(isValidDebianPackageName("aaß")); // only basic ascii alphabet
        assertFalse(isValidDebianPackageName("a_a")); // no underscores
    }

    @Test
    public void testCheckPackageNameReturnsInput() {
        String packageName = "input-name+with.weird5name";
        assertEquals(packageName, checkPackageName(packageName));
    }

    @Test(expected=NullPointerException.class)
    public void testCheckPackageNameDoesNotAcceptNull() {
        checkPackageName(null);
    }
}
