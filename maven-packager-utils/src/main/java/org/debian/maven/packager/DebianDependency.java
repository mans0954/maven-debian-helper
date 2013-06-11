package org.debian.maven.packager;

import java.util.regex.Pattern;

import static org.debian.maven.util.Preconditions.*;

public class DebianDependency implements Comparable<DebianDependency> {

    private static final Pattern VALID_DEBIAN_PACKAGE_NAME = Pattern
            .compile("^[a-z0-9][a-z0-9+-.]+$");

    private final String packageName;
    private final String minimumVersion;

    public DebianDependency(String packageName, String minimumVersion) {
        super();
        this.packageName = checkPackageName(packageName);
        this.minimumVersion = checkNotNull(minimumVersion);
    }

    public DebianDependency(String packageName) {
        this(packageName, "");
    }

    @Override
    public String toString() {
        if(minimumVersion.isEmpty())
            return packageName;
        return packageName + " (>= " + minimumVersion + ")";
    }

    public String getPackageName() {
        return packageName;
    }

    /**
     * Check whether packageName is valid according to the Debian Policy 5.6.1.
     * 
     * Package names must consist only of lower case letters (a-z), digits
     * (0-9), plus (+) and minus (-) signs, and periods (.). They must be at
     * least two characters long and must start with an alphanumeric character.
     */
    public static boolean isValidDebianPackageName(String packageName) {
        return VALID_DEBIAN_PACKAGE_NAME.matcher(packageName).matches();
    }

    public static String checkPackageName(String packageName) {
        if (packageName == null || !isValidDebianPackageName(checkNotEmpty(packageName))) {
            throw new IllegalArgumentException("Not a valid package name: " + packageName);
        }
        return packageName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((minimumVersion == null) ? 0 : minimumVersion.hashCode());
        result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DebianDependency other = (DebianDependency) obj;
        if (minimumVersion == null) {
            if (other.minimumVersion != null) return false;
        } else if (!minimumVersion.equals(other.minimumVersion)) return false;
        if (packageName == null) {
            if (other.packageName != null) return false;
        } else if (!packageName.equals(other.packageName)) return false;
        return true;
    }

    @Override
    public int compareTo(DebianDependency other) {
        if(equals(other)) return 0;

        return toString().compareTo(other.toString());
    }
}