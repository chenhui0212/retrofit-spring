package retrofit2.service;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;
import retrofit2.annotation.RetrofitService;

import java.util.Arrays;
import java.util.Set;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers Retrofit Service by {@code basePackage}.
 * <p>
 *
 * @author Chen Hui
 */
public class ClassPathRetrofitServiceScanner extends ClassPathBeanDefinitionScanner {

    // Copy of FactoryBean#OBJECT_TYPE_ATTRIBUTE which was added in Spring 5.2
    static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

    private boolean lazyInitialization;

    private String retrofitBeanName;

    public ClassPathRetrofitServiceScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    public void setLazyInitialization(boolean lazyInitialization) {
        this.lazyInitialization = lazyInitialization;
    }

    public void setRetrofitBeanName(String retrofitBeanName) {
        this.retrofitBeanName = retrofitBeanName;
    }

    /**
     * Configures parent scanner to search for the right interfaces.
     */
    public void registerFilters() {
        addIncludeFilter(new AnnotationTypeFilter(RetrofitService.class));

        // exclude package-info.java
        addExcludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            return className.endsWith("package-info");
        });
    }

    /**
     * Calls the parent search that will search and register all the candidates. Then the registered objects are post
     * processed to set them as MapperFactoryBeans
     */
    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        if (beanDefinitions.isEmpty()) {
            logger.warn("No Retrofit service was found in '" + Arrays.toString(basePackages)
                    + "' package. Please check your configuration.");
        } else {
            processBeanDefinitions(beanDefinitions);
        }

        return beanDefinitions;
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        AbstractBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (AbstractBeanDefinition) holder.getBeanDefinition();
            String beanClassName = definition.getBeanClassName();
            if (beanClassName == null) continue;
            logger.debug("Creating RetrofitServiceFactoryBean with name '" + holder.getBeanName() + "' and '"
                    + beanClassName + "' retrofit service interface");

            // the retrofit service interface is the original class of the bean
            // but, the actual class of the bean is RetrofitServiceFactoryBean
            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
            definition.setBeanClass(RetrofitServiceFactoryBean.class);

            // Attribute for MockitoPostProcessor
            // https://github.com/mybatis/spring-boot-starter/issues/475
            definition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, beanClassName);

            if (StringUtils.hasText(this.retrofitBeanName)) {
                definition.getPropertyValues().add("retrofit", new RuntimeBeanReference(this.retrofitBeanName));
            } else {
                logger.debug("Enabling autowire by type for RetrofitServiceFactoryBean with name '"
                        + holder.getBeanName() + "'.");
                definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            }

            definition.setLazyInit(lazyInitialization);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        if (super.checkCandidate(beanName, beanDefinition)) {
            return true;
        } else {
            logger.warn("Skipping RetrofitServiceFactoryBean with name '" + beanName + "' and '"
                    + beanDefinition.getBeanClassName() + "' retrofit service interface"
                    + ". Bean already defined with the same name!");
            return false;
        }
    }

}
