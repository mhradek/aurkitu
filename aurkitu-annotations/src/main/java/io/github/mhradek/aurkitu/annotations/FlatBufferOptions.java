package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Has no effect and is not read by the plugin; kept so existing code still compiles.
 * Field options live on {@link FlatBufferFieldOptions} (e.g. {@code useFullName}).
 *
 * @author m.hradek
 * @deprecated has never had an effect; use {@link FlatBufferFieldOptions}. It will be removed in 2.0.
 */
@Deprecated(since = "1.0.1", forRemoval = true)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferOptions {

}
