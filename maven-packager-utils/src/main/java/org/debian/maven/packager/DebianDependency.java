package org.debian.maven.packager;

public class DebianDependency {

    private final String packageName;
    private final String minimumVersion;

    public DebianDependency(String pkgname, String minimumVersion) {
        super();
        if(pkgname.contains(" ")) throw new IllegalArgumentException("Not a valid package name: " + pkgname);
        this.packageName = pkgname;
        this.minimumVersion = minimumVersion;
    }

    public DebianDependency(String pkgname) {
        this(pkgname, "");
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
}