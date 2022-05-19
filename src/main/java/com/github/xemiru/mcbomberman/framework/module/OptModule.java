package com.github.xemiru.mcbomberman.framework.module;

import com.github.xemiru.mcbomberman.framework.Game;
import com.github.xemiru.mcbomberman.framework.GameState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Denotes the use of a {@link GameModule} that should be provided upon transitioning to a {@link GameState} with fields
 * tagged with this annotation.
 * <p/>
 * This differs from @{@link GModule} in that registration of the target module is not required prior to a transition to
 * a GameState. Instead, fields with this annotation should take a type of {@link Optional} that holds the target module
 * type. If the module is present within the host {@link Game}, the field is populated with an Optional holding the
 * latter. The field is otherwise provided an empty Optional.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptModule {
}
