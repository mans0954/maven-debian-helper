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

    @Test(expected=IllegalArgumentException.class)
    public void testCheckPackageNameDoesNotAcceptNull() {
        checkPackageName(null);
    }

    @Test
    public void testEquals() {
        DebianDependency dep1 = new DebianDependency("maven-debian-helper", "1.0");
        DebianDependency dep2 = new DebianDependency("maven-debian-helper", "1.0");
        DebianDependency dep3 = new DebianDependency("maven-repo-helper", "1.0");
        DebianDependency dep4 = new DebianDependency("maven-repo-helper", "2.0");
        
        assertFalse(dep1.equals(""));
        assertTrue(dep1.equals(dep1));
        assertTrue(dep1.equals(dep2));
        assertFalse(dep2.equals(dep3));
        assertFalse(dep3.equals(dep4));
    }

    @Test
    public void testToString() {
        DebianDependency dep1 = new DebianDependency("maven-debian-helper", "1.0");
        assertEquals("maven-debian-helper (>= 1.0)", dep1.toString());
        
        DebianDependency dep2 = new DebianDependency("maven-debian-helper");
        assertEquals("maven-debian-helper", dep2.toString());
    }
    
    @Test
    public void testCompare() {
        DebianDependency dep1 = new DebianDependency("maven-debian-helper", "1.0");
        DebianDependency dep2 = new DebianDependency("maven-repo-helper", "1.0");
        
        assertEquals(0, dep1.compareTo(dep1));
        assertTrue(dep1.compareTo(dep2) < 0);
    }
}
