package com.github.xemiru.mcbomberman.framework.module;

import com.github.xemiru.mcbomberman.framework.Game;
import com.github.xemiru.mcbomberman.framework.GameState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a {@link GameModule} that should be initialized upon transitioning to a {@link GameState} with fields tagged
 * with this annotation.
 * <p/>
 * Before the containing GameState receives a call to {@link GameState#onInit()}, the module type as declared by the
 * field this annotation is attached to is instantiated and registered. If the host {@link Game} already possesses a
 * module of this type, a call to {@link GameModule#reset()} is triggered.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegModule {

    /**
     * Denotes whether the module is persistent.
     * <p/>
     * See {@link GameModule} to learn about persistency.
     */
    boolean persistent() default false;

}
