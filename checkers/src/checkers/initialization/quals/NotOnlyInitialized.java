package checkers.initialization.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A declaration annotation for fields that indicates that the values the given
 * field can store might not be {@link Initialized} (but
 * {@link UnderInitializion} or {@link UnkownInitialization} instead). This is
 * necessary to make allow circular initialization as supported by
 * freedom-before-commitment.
 *
 * @author Stefan Heule
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotOnlyInitialized {
}
