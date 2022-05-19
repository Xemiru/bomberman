package com.github.xemiru.mcbomberman.util;

public class Utility {

    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

}
