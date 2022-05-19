package com.github.xemiru.mcbomberman.framework.module;

import com.github.xemiru.mcbomberman.framework.GameState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes the use of a {@link GameModule} that should be provided upon transitioning to a {@link GameState} with fields
 * tagged with this annotation.
 * <p/>
 * Care should be taken that GameStates prior to ones holding this annotation properly register their modules through
 * the use of the @{@link RegModule} annotation. A GameState cannot make use of a module that has yet to be registered
 * to its host game, and will raise an exception in the transition if it attempts to do so.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GModule {
}
