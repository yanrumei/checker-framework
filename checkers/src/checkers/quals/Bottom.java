package checkers.quals;

import java.lang.annotation.Target;

/**
 * A special annotation intended solely for representing the bottom type in
 * the qualifier hierarchy.
 * This qualifier is only used automatically if the existing qualifiers do not have a
 * bottom type.
 * Other type systems could reuse this qualifier instead of introducing their own
 * dedicated bottom qualifier. The programmer would then use methods like
 * {@link checkers.types.TreeAnnotator#addTreeKind(Kind, AnnotationMirror)} to
 * add implicit annotations and needs to manually add the bottom qualifier to the
 * qualifier hierarchy.
 *
 * <p>
 * Programmers cannot write this qualifier in source code.
 * 
 * @see checkers.types.QualifierHierarchy#getBottomAnnotations()
 */
@TypeQualifier
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface Bottom { }
