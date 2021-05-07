package retrofit2.annotation;


import java.lang.annotation.*;

/**
 * Marker interface for Retrofit service.
 * <p>
 *
 * @author Chen Hui
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RetrofitService {
}
