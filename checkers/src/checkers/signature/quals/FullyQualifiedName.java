package checkers.signature.quals;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents a fully-qualified name as defined in the <a href="http://java.sun.com/docs/books/jls/third_edition/html/names.html#6.7">Java Language Specification, section 6.7</a>.
 * <p>
 * For example, in
 * <pre>
 *  package checkers.signature;
 *  public class SignatureChecker {
 *    private class Inner {}
 *  }
 * </pre>
 * the fully-qualified names for the two types are checkers.signature.SignatureChecker
 * and checkers.signature.SignatureChecker.Inner.
 * <p>
 * Binary names and fully qualified names are same for top-level classes
 * and only differ by a '$' vs. '.' for inner classes.
 */
@TypeQualifier
@SubtypeOf(UnannotatedString.class)
@ImplicitFor(stringPatterns="^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\[\\])*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FullyQualifiedName {}
