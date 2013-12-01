package org.debian.maven.packager;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.debian.maven.util.Strings;

public class DebianDependencies {

    public final EnumMap<Type, Set<DebianDependency>> deps = new EnumMap<Type, Set<DebianDependency>>(Type.class);

    public DebianDependencies() {
        for (Type type : Type.values()) {
            deps.put(type, new TreeSet<DebianDependency>());
        }
    }

    public void add(Type type, DebianDependency dependency) {
        this.deps.get(type).add(dependency);
    }

    public void add(Type type, Collection<DebianDependency> dependencies) {
        get(type).addAll(dependencies);
    }

    public Set<DebianDependency> get(Type type) {
        return deps.get(type);
    }

    public void putInProperties(Properties depVars) {
        for (Type type : Type.values()) {
            depVars.put(type.substvarName, Strings.join(deps.get(type), ", "));
        }
    }

    public static enum Type {
        COMPILE("maven.CompileDepends"),
        TEST("maven.TestDepends"),
        RUNTIME("maven.Depends"),
        OPTIONAL("maven.OptionalDepends"),
        DOC_RUNTIME("maven.DocDepends"),
        DOC_OPTIONAL("maven.DocOptionalDepends");

        public final String substvarName;

        Type(String substvarName) {
            this.substvarName = substvarName;
        }
    }

}
