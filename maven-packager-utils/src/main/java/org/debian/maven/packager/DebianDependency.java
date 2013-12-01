package org.debian.maven.packager;

import java.util.regex.Pattern;

/**
 * A dependency on a Debian package, with an optional minimum version.
 * 
 * @since 1.8
 */
public class DebianDependency implements Comparable<DebianDependency> {

    /**
     * Pattern for valid package names according to the Debian Policy 5.6.1.
     *
     * Package names must consist only of lower case letters (a-z), digits
     * (0-9), plus (+) and minus (-) signs, and periods (.). They must be at
     * least two characters long and must start with an alphanumeric character.
     */
    private static final Pattern DEBIAN_PACKAGE_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9+-.]+$");

    /** The name of the Debian package */
    private final String packageName;

    /** The minimum version required (blank if not specified) */
    private final String minimumVersion;

    /**
     * Creates a dependency on the specified package.
     * 
     * @param packageName    the name of the Debian package
     * @param minimumVersion the minimum version required, empty if none
     * @throws IllegalArgumentException if the package name or the minimum version is not valid
     */
    public DebianDependency(String packageName, String minimumVersion) throws IllegalArgumentException {
        // check the validity of the package name
        if (packageName == null || !DEBIAN_PACKAGE_NAME_PATTERN.matcher(packageName).matches()) {
            throw new IllegalArgumentException("Invalid package name: " + packageName);
        }
        
        if (minimumVersion == null) {
            throw new IllegalArgumentException("Invalid minimum version: " + minimumVersion);
        }
        
        this.packageName = packageName;
        this.minimumVersion = minimumVersion;
    }

    /**
     * Creates a non version dependency on the specified package.
     * 
     * @param packageName the name of the Debian package
     * @throws IllegalArgumentException if the package name is not valid
     */
    public DebianDependency(String packageName) throws IllegalArgumentException {
        this(packageName, "");
    }

    /**
     * Returns the name of the Debian package.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Returns the string representation of the dependency using the syntax
     * of the Debian control files:
     * 
     * <pre>
     *     foo (>= 1.0)
     * </pre>
     */
    public String toString() {
        return minimumVersion.isEmpty() ? packageName : packageName + " (>= " + minimumVersion + ")";
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DebianDependency)) {
            return false;
        }
        
        DebianDependency that = (DebianDependency) obj;
        
        if (!minimumVersion.equals(that.minimumVersion)) {
            return false;
        }
        if (!packageName.equals(that.packageName)) {
            return false;
        }
        
        return true;
    }

    @Override
    public int compareTo(DebianDependency other) {
        if (equals(other)) {
            return 0;
        }

        return toString().compareTo(other.toString());
    }
}
