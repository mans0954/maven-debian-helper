package org.debian.maven.packager;

import static org.junit.Assert.*;

import org.junit.Test;

public class DebianDependencyTest {

    @Test
    public void testValidDebianPackageName() {
        new DebianDependency("a-");
        new DebianDependency("1-");
        new DebianDependency("allthecharacters-+.0123456789");
        new DebianDependency("a-b");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullDebianPackageName() {
        new DebianDependency(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyDebianPackageName() {
        new DebianDependency("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDebianPackageNameWithUppercase() {
        new DebianDependency("aaA"); // no uppercase
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDebianPackageNameWithSpace() {
        new DebianDependency("aa a"); // no space
    }

    @Test(expected=IllegalArgumentException.class)
    public void testTooShortDebianPackageName() {
        new DebianDependency("a"); // minimum 2 characters
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDebianPackageNameWithNonAlphanumericFirstChar() {
        new DebianDependency("-aa"); // must start with alphanumeric
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDebianPackageNameWithNonAscii() {
        new DebianDependency("aaß"); // only basic ascii alphabet
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDebianPackageNameWithUnderscore() {
        new DebianDependency("a_a"); // no underscores
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDebianPackageNameWithNullVersion() {
        new DebianDependency("foo", null);
    }

    @Test
    public void testGetPackageName() {
        String name = "input-name+with.weird5name";
        DebianDependency dependency = new DebianDependency(name);
        assertEquals(name, dependency.getPackageName());
    }

    @Test
    public void testEquals() {
        DebianDependency dep1 = new DebianDependency("maven-debian-helper", "1.0");
        DebianDependency dep2 = new DebianDependency("maven-debian-helper", "1.0");
        DebianDependency dep3 = new DebianDependency("maven-repo-helper", "1.0");
        DebianDependency dep4 = new DebianDependency("maven-repo-helper", "2.0");
        
        assertFalse(dep1.equals(""));    // different class
        assertTrue(dep1.equals(dep1));   // same instance
        assertTrue(dep1.equals(dep2));   // same dependency, different instances
        assertFalse(dep2.equals(dep3));  // different dependencies
        assertFalse(dep3.equals(dep4));  // same dependency, different versions
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
