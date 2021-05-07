package retrofit2.service;

import org.springframework.beans.factory.FactoryBean;
import retrofit2.Retrofit;

/**
 * BeanFactory that enables injection of service interfaces. It can be set up with a Retrofit.
 * <p>
 * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
 *
 * @author Chen Hui
 */
public class RetrofitServiceFactoryBean<T> implements FactoryBean<T> {

    private Class<T> retrofitServiceInterface;

    private Retrofit retrofit;

    public RetrofitServiceFactoryBean() {
        // intentionally empty
    }

    public RetrofitServiceFactoryBean(Class<T> retrofitServiceInterface) {
        this.retrofitServiceInterface = retrofitServiceInterface;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getObject() throws Exception {
        return retrofit.create(this.retrofitServiceInterface);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getObjectType() {
        return retrofitServiceInterface;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton() {
        return true;
    }


    // ------------- mutators --------------

    public void setRetrofitServiceInterface(Class<T> retrofitServiceInterface) {
        this.retrofitServiceInterface = retrofitServiceInterface;
    }

    public Class<T> getRetrofitServiceInterface() {
        return retrofitServiceInterface;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public void setRetrofit(Retrofit retrofit) {
        this.retrofit = retrofit;
    }
}
