package com.michaelhradek.aurkitu.core;

/**
 * @author m.hradek
 *
 */
public class Utilities {

    /**
     * @param type The class which needs to be tested if it is a primative. Also, double and Double are both considered primative within this context.
     * @return boolean
     */
    public static boolean isLowerCaseType(Class<?> type) {
        return (type.isPrimitive() && type != void.class) || type == Double.class || type == Float.class
                || type == Long.class || type == Integer.class || type == Short.class
                || type == Character.class || type == Byte.class || type == Boolean.class
                || type == String.class;
    }
}
