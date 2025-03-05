package daomephsta.unpick.impl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks internal classes and methods which have been exposed to API in the past and therefore need backwards compat
 * considerations.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface LegacyExposed
{
}
