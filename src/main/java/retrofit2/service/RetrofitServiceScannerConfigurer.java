package retrofit2.service;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

import static org.springframework.util.Assert.notNull;

/**
 * BeanDefinitionRegistryPostProcessor that searches recursively starting from a base package for interfaces and
 * registers them as {@code RetrofitServiceFactoryBean}. Note that only interfaces with at least one method will
 * be registered; concrete classes will be ignored.
 * <p>
 * The {@code basePackage} property can contain more than one package name, separated by either commas or semicolons.
 * <p>
 * This configurer enables autowire for all the beans that it creates so that they are automatically autowired with the
 * proper {@code Retrofit}. If there is more than one {@code Retrofit} in the application, however, autowiring cannot
 * be used. In this case you must explicitly specify an {@code Retrofit} to use via the <em>bean name</em> properties.
 * Bean names are used rather than actual objects because Spring does not initialize property placeholders until after
 * this class is processed.
 * <p>
 * Passing in an actual object which may require placeholders (i.e. DB user password) will fail. Using bean names defers
 * actual object creation until later in the startup process, after all placeholder substitution is completed. However,
 * note that this configurer does support property placeholders of its <em>own</em> properties. The
 * <code>basePackage</code> and bean name properties all support <code>${property}</code> style substitution.
 * <p>
 *
 * @author Chen Hui
 * @see RetrofitServiceFactoryBean
 * @see ClassPathRetrofitServiceScanner
 */
public class RetrofitServiceScannerConfigurer
        implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

    private String basePackage;

    private String lazyInitialization;

    private String retrofitBeanName;

    private ApplicationContext applicationContext;

    private String beanName;

    private boolean processPropertyPlaceHolders;


    /**
     * This property lets you set the base package for your mapper interface files.
     * <p>
     * You can set more than one package by using a semicolon or comma as a separator.
     * <p>
     * Mappers will be searched for recursively starting in the specified package(s).
     *
     * @param basePackage base package name
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Set whether enable lazy initialization for retrofit service bean.
     * <p>
     * Default is {@code false}.
     * </p>
     *
     * @param lazyInitialization Set the @{code true} to enable
     */
    public void setLazyInitialization(String lazyInitialization) {
        this.lazyInitialization = lazyInitialization;
    }

    /**
     * Specifies which {@code Retrofit} to use in the case that there is more than one in the spring context.
     * Usually this is only needed when you have more than one retrofit.
     * <p>
     * Note bean names are used, not bean references. This is because the scanner loads early during the start process
     * and it is too early to build retrofit object instances.
     *
     * @param retrofitBeanName Bean name of the {@code Retrofit}
     */
    public void setRetrofitBeanName(String retrofitBeanName) {
        this.retrofitBeanName = retrofitBeanName;
    }

    /**
     * Specifies a flag that whether execute a property placeholder processing or not.
     * <p>
     * The default is {@literal false}. This means that a property placeholder processing does not execute.
     *
     * @param processPropertyPlaceHolders a flag that whether execute a property placeholder processing or not
     */
    public void setProcessPropertyPlaceHolders(boolean processPropertyPlaceHolders) {
        this.processPropertyPlaceHolders = processPropertyPlaceHolders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        notNull(this.basePackage, "Property 'basePackage' is required");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // left intentionally blank
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        if (this.processPropertyPlaceHolders) {
            processPropertyPlaceHolders();
        }

        ClassPathRetrofitServiceScanner scanner = new ClassPathRetrofitServiceScanner(registry);
        scanner.setRetrofitBeanName(this.retrofitBeanName);
        scanner.setResourceLoader(this.applicationContext);
        if (StringUtils.hasText(lazyInitialization)) {
            scanner.setLazyInitialization(Boolean.parseBoolean(lazyInitialization));
        }
        scanner.registerFilters();
        scanner.scan(StringUtils.tokenizeToStringArray(
                this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    /*
     * BeanDefinitionRegistries are called early in application startup, before BeanFactoryPostProcessors. This means that
     * PropertyResourceConfigurers will not have been loaded and any property substitution of this class' properties will
     * fail. To avoid this, find any PropertyResourceConfigurers defined in the context and run them on this class' bean
     * definition. Then update the values.
     */
    private void processPropertyPlaceHolders() {
        Map<String, PropertyResourceConfigurer> prcMap = applicationContext.getBeansOfType(
                PropertyResourceConfigurer.class, false, false);

        if (!prcMap.isEmpty() && applicationContext instanceof ConfigurableApplicationContext) {
            BeanDefinition scannerBean = ((ConfigurableApplicationContext) applicationContext).getBeanFactory()
                    .getBeanDefinition(beanName);

            // PropertyResourceConfigurer does not expose any methods to explicitly perform
            // property placeholder substitution. Instead, create a BeanFactory that just
            // contains this mapper scanner and post process the factory.
            DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
            factory.registerBeanDefinition(beanName, scannerBean);

            for (PropertyResourceConfigurer prc : prcMap.values()) {
                prc.postProcessBeanFactory(factory);
            }

            PropertyValues values = scannerBean.getPropertyValues();

            this.basePackage = getPropertyValue("basePackage", values);
            this.retrofitBeanName = getPropertyValue("retrofitBeanName", values);
            this.lazyInitialization = getPropertyValue("lazyInitialization", values);
        }
        this.basePackage = Optional.ofNullable(this.basePackage)
                .map(getEnvironment()::resolvePlaceholders).orElse(null);
        this.retrofitBeanName = Optional.ofNullable(this.retrofitBeanName)
                .map(getEnvironment()::resolvePlaceholders).orElse(null);
        this.lazyInitialization = Optional.ofNullable(this.lazyInitialization)
                .map(getEnvironment()::resolvePlaceholders).orElse(null);
    }

    private Environment getEnvironment() {
        return this.applicationContext.getEnvironment();
    }

    private String getPropertyValue(String propertyName, PropertyValues values) {
        PropertyValue property = values.getPropertyValue(propertyName);

        if (property == null) {
            return null;
        }

        Object value = property.getValue();

        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return value.toString();
        } else if (value instanceof TypedStringValue) {
            return ((TypedStringValue) value).getValue();
        } else {
            return null;
        }
    }

}
