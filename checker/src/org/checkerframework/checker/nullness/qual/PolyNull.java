package org.checkerframework.checker.nullness.qual;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * A polymorphic qualifier for the non-null type system.
 *
 * <p>
 * Any method written using {@link PolyNull} conceptually has two versions: one
 * in which every instance of {@link PolyNull} has been replaced by
 * {@link NonNull}, and one in which every instance of {@link PolyNull} has been
 * replaced by {@link Nullable}.
 *
 * @checker_framework_manual #nullness-checker Nullness Checker
 */
@Documented
@TypeQualifier
@PolymorphicQualifier(Nullable.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface PolyNull {
}
