/*
 * User: ophir
 * Date: Mar 28, 2009
 * Time: 12:52:22 AM
 */
package com.michaelhradek.aurkitu.reflections;

public class ReflectionsException extends RuntimeException {

    public ReflectionsException(String message) {
        super(message);
    }

    public ReflectionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReflectionsException(Throwable cause) {
        super(cause);
    }
}
