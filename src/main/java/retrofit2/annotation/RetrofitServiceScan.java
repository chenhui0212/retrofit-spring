package retrofit2.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Use this annotation to register Retrofit service interfaces when using Java Config.
 *
 * <p>
 * Either {@link #basePackageClasses} or {@link #basePackages} (or its alias {@link #value}) may be specified to
 * define specific packages to scan. If specific packages are not defined, scanning will occur from the package of
 * the class that declares this annotation.
 *
 * <p>
 * Configuration example:
 * </p>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;RetrofitScan("org.square.retrofit.service")
 * public class AppConfig {
 * // ...
 * }
 * </pre>
 *
 * @author Chen Hui
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RetrofitServiceScannerRegistrar.class)
public @interface RetrofitServiceScan {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
     * {@code @RetrofitScan("org.my.pkg")} instead of {@code @RetrofitScan(basePackages = "org.my.pkg"})}.
     *
     * @return base package names
     */
    String[] value() default {};

    /**
     * Base packages to scan for Retrofit service interfaces. Note that only interfaces with at least one method
     * will be registered; concrete classes will be ignored.
     *
     * @return base package names for scanning retrofit service interface
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components.
     * The package of each class specified will be scanned.
     * <p>
     * Consider creating a special no-op marker class or interface in each package that serves no purpose other than
     * being referenced by this attribute.
     *
     * @return classes that indicate base package for scanning retrofit service interface
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Specifies which {@code Retrofit} to use in the case that there is more than one in the spring context.
     * Usually this is only needed when you have more than one retrofit.
     *
     * @return the bean name of {@code Retrofit}
     */
    String retrofitRef() default "";

    /**
     * Whether enable lazy initialization of retrofit service proxy bean.
     *
     * <p>
     * Default is {@code false}.
     * </p>
     *
     * @return set {@code true} to enable lazy initialization
     */
    String lazyInitialization() default "";

}
