package checkers.propkey;

import checkers.basetype.BaseTypeChecker;
import checkers.propkey.quals.PropertyKey;
import checkers.quals.Bottom;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import javacutils.AnnotationUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

/**
 * This AnnotatedTypeFactory adds PropertyKey annotations to String literals
 * that contain values from lookupKeys.
 *
 * @author wmdietl
 */
public class PropertyKeyAnnotatedTypeFactory extends BasicAnnotatedTypeFactory {

    private final Set<String> lookupKeys;

    public PropertyKeyAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.lookupKeys = Collections.unmodifiableSet(buildLookupKeys());

        // Reuse the framework Bottom annotation and make it the default for the
        // null literal.
        AnnotationMirror BOTTOM = AnnotationUtils.fromClass(elements, Bottom.class);

        this.postInit();

        this.treeAnnotator.addTreeKind(Tree.Kind.NULL_LITERAL, BOTTOM);
        this.typeAnnotator.addTypeName(java.lang.Void.class, BOTTOM);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
           return new KeyLookupTreeAnnotator(this, PropertyKey.class);
    }


    /**
     * This TreeAnnotator checks for every String literal whether it is included in the lookup
     * keys. If it is, the given annotation is added to the literal; otherwise, nothing happens.
     * Subclasses of this AnnotatedTypeFactory can directly reuse this class and use a different
     * annotation as parameter.
     */
    protected class KeyLookupTreeAnnotator extends TreeAnnotator {
        AnnotationMirror theAnnot;

        public KeyLookupTreeAnnotator(BasicAnnotatedTypeFactory atf, Class<? extends Annotation> annot) {
            super(atf);
            theAnnot = AnnotationUtils.fromClass(elements, annot);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotatedInHierarchy(theAnnot)
                && tree.getKind() == Tree.Kind.STRING_LITERAL
                && strContains(lookupKeys, tree.getValue().toString())) {
                type.addAnnotation(theAnnot);
            }
            // A possible extension is to record all the keys that have been used and
            // in the end output a list of keys that were not used in the program,
            // possibly pointing to the opposite problem, keys that were supposed to
            // be used somewhere, but have not been, maybe because of copy-and-paste errors.
            return super.visitLiteral(tree, type);
        }

        // Result of binary op might not be a property key.
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.removeAnnotation(theAnnot);
            return null; // super.visitBinary(node, type);
        }

        // Result of unary op might not be a property key.
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            type.removeAnnotation(theAnnot);
            return null; // super.visitCompoundAssignment(node, type);
        }
    }

    /**
     * Instead of a precise comparison, we incrementally remove leading dot-separated
     * strings until we find a match.
     * For example if messages contains "y.z" and we look for "x.y.z" we find a match
     * after removing the first "x.".
     *
     * Compare to SourceChecker.fullMessageOf.
     */
    private static boolean strContains(Set<String> messages, String messageKey) {
        String key = messageKey;

        do {
            if (messages.contains(key)) {
                return true;
            }

            int dot = key.indexOf('.');
            if (dot < 0) return false;
            key = key.substring(dot + 1);
        } while (true);
    }


    /**
     * Returns a set of the valid keys that can be used.
     */
    public Set<String> getLookupKeys() {
        return this.lookupKeys;
    }

    private Set<String> buildLookupKeys() {
        Set<String> result = new HashSet<String>();

        if (checker.hasOption("propfiles")) {
            result.addAll( keysOfPropertyFiles(checker.getOption("propfiles")) );
        }
        if (checker.hasOption("bundlenames")) {
            result.addAll( keysOfResourceBundle(checker.getOption("bundlenames")) );
        }

        return result;
    }

    private Set<String> keysOfPropertyFiles(String names) {
        String[] namesArr = names.split(":");

        if (namesArr == null) {
            System.err.println("Couldn't parse the properties files: <" + names + ">");
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<String>();

        for (String name : namesArr) {
            try {
                Properties prop = new Properties();

                InputStream in = null;

                ClassLoader cl = this.getClass().getClassLoader();
                if (cl == null) {
                    // the class loader is null if the system class loader was
                    // used
                    cl = ClassLoader.getSystemClassLoader();
                }
                in = cl.getResourceAsStream(name);

                if (in == null) {
                    // if the classloader didn't manage to load the file, try
                    // whether a FileInputStream works. For absolute paths this
                    // might help.
                    try {
                        in = new FileInputStream(name);
                    } catch (FileNotFoundException e) {
                        // ignore
                    }
                }

                if (in == null) {
                    System.err.println("Couldn't find the properties file: " + name);
                    // report(Result.failure("propertykeychecker.filenotfound",
                    // name), null);
                    // return Collections.emptySet();
                    continue;
                }

                prop.load(in);
                result.addAll(prop.stringPropertyNames());
            } catch (Exception e) {
                // TODO: is there a nicer way to report messages, that are not
                // connected to an AST node?
                // One cannot use report, because it needs a node.
                System.err.println("Exception in PropertyKeyChecker.keysOfPropertyFile: " + e);
                e.printStackTrace();
            }
        }

        return result;
    }

    private Set<String> keysOfResourceBundle(String bundleNames) {
        String[] namesArr = bundleNames.split(":");

        if (namesArr == null) {
            System.err.println("Couldn't parse the resource bundles: <" + bundleNames + ">");
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<String>();

        for (String bundleName : namesArr) {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
            if (bundle == null) {
                System.err.println("Couldn't find the resource bundle: <" + bundleName
                    + "> for locale <" + Locale.getDefault() + ">");
                continue;
            }

            result.addAll(bundle.keySet());
        }
        return result;
    }

    @Override
    public GraphQualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new GraphQualifierHierarchy(factory, AnnotationUtils.fromClass(elements, Bottom.class));
    }
}
