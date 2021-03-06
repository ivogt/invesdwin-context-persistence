package de.invesdwin.context.persistence.jpa.datanucleus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Named;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.datanucleus.api.jpa.PersistenceProviderImpl;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaVendorAdapter;

import de.invesdwin.context.beans.init.EhCacheConfigurationMerger;
import de.invesdwin.context.beans.init.MergedContext;
import de.invesdwin.context.persistence.jpa.ConnectionAutoSchema;
import de.invesdwin.context.persistence.jpa.ConnectionDialect;
import de.invesdwin.context.persistence.jpa.PersistenceUnitContext;
import de.invesdwin.context.persistence.jpa.api.index.Indexes;
import de.invesdwin.context.persistence.jpa.api.query.IConfigurableQuery;
import de.invesdwin.context.persistence.jpa.spi.delegate.IDialectSpecificDelegate;
import de.invesdwin.context.persistence.jpa.spi.impl.ConfiguredCPDataSource;
import de.invesdwin.context.persistence.jpa.spi.impl.NativeJdbcIndexCreationHandler;
import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.error.UnknownArgumentException;

@Named
@NotThreadSafe
public class DatanucleusDialectSpecificDelegate implements IDialectSpecificDelegate {

    private final NativeJdbcIndexCreationHandler nativeJdbcIndexCreationHandler = new NativeJdbcIndexCreationHandler();

    @Override
    public void createIndexes(final PersistenceUnitContext context, final Class<?> entityClass, final Indexes indexes) {
        nativeJdbcIndexCreationHandler.createIndexes(context, entityClass, indexes);
    }

    @Override
    public void dropIndexes(final PersistenceUnitContext context, final Class<?> entityClass, final Indexes indexes) {
        nativeJdbcIndexCreationHandler.dropIndexes(context, entityClass, indexes);
    }

    @Override
    public DataSource createDataSource(final PersistenceUnitContext context) {
        return new ConfiguredCPDataSource(context, false);
    }

    @Override
    public Map<String, String> getPersistenceProperties(final PersistenceUnitContext context) {
        final Map<String, String> props = new HashMap<String, String>();

        if (context.getConnectionDialect().isRdbms()) {
            props.put("datanucleus.ConnectionDriverName", context.getConnectionDriver());
            props.put("datanucleus.ConnectionURL", context.getConnectionUrl());
            props.put("datanucleus.ConnectionUserName", context.getPersistenceUnitName());
            props.put("datanucleus.ConnectionPassword", "NEED_TO_LOOKUP_VIA_PERSISTENCE_UNIT_CONTEXT");
        } else {
            //        <prop key="datanucleus.ConnectionDriverName">${javax.persistence.jdbc.driver}</prop>
            props.put("datanucleus.ConnectionDriverName", context.getConnectionDriver());
            //        <prop key="datanucleus.ConnectionURL">${javax.persistence.jdbc.url}</prop>
            props.put("datanucleus.ConnectionURL", context.getConnectionUrl());
            //        <prop key="datanucleus.ConnectionUserName">${javax.persistence.jdbc.user}</prop>
            props.put("datanucleus.ConnectionUserName", context.getConnectionUser());
            //        <prop key="datanucleus.ConnectionPassword">${javax.persistence.jdbc.password}</prop>
            props.put("datanucleus.ConnectionPassword", context.getConnectionPassword());
            //        <prop key="datanucleus.cloud.storage.bucket">${datanucleus.cloud.storage.bucket}</prop>
            //TODO: extract this from connection url syntax? or introduce a new parameter for this
        }

        final boolean datanucleusAutoCreateSchema;
        final boolean datanucleusValidateSchema;
        switch (context.getConnectionAutoSchema()) {
        case CREATE:
        case CREATE_DROP:
        case UPDATE:
            datanucleusAutoCreateSchema = true;
            datanucleusValidateSchema = true;
            break;
        case VALIDATE:
            datanucleusAutoCreateSchema = false;
            datanucleusValidateSchema = true;
            break;
        default:
            throw UnknownArgumentException.newInstance(ConnectionAutoSchema.class, context.getConnectionAutoSchema());
        }
        //        <prop key="">${datanucleus.autoCreateSchema}</prop>
        props.put("datanucleus.autoCreateSchema", String.valueOf(datanucleusAutoCreateSchema));
        //        <prop key="datanucleus.autoCreateTables">${datanucleus.autoCreateSchema}</prop>
        props.put("datanucleus.autoCreateTables", String.valueOf(datanucleusAutoCreateSchema));
        //        <prop key="datanucleus.autoCreateColumns">${datanucleus.autoCreateSchema}</prop>
        props.put("datanucleus.autoCreateColumns", String.valueOf(datanucleusAutoCreateSchema));
        //        <prop key="datanucleus.autoCreateConstraints">${datanucleus.autoCreateSchema}</prop>
        props.put("datanucleus.autoCreateConstraints", String.valueOf(datanucleusAutoCreateSchema));
        //        <prop key="datanucleus.validateTables">${datanucleus.validateSchema}</prop>
        props.put("datanucleus.validateTables", String.valueOf(datanucleusValidateSchema));
        //        <prop key="datanucleus.validateColumns">${datanucleus.validateSchema}</prop>
        props.put("datanucleus.validateColumns", String.valueOf(datanucleusValidateSchema));
        //        <prop key="datanucleus.validateConstraints">${datanucleus.validateSchema}</prop>
        props.put("datanucleus.validateConstraints", String.valueOf(datanucleusValidateSchema));

        //        <prop key="datanucleus.rdbms.statementBatchLimit">${de.invesdwin.common.persistence.PersistenceProperties.CONNECTION_BATCH_SIZE}</prop>
        props.put("datanucleus.rdbms.statementBatchLimit", String.valueOf(context.getConnectionBatchSize()));
        //        <prop key="datanucleus.jpa.addClassTransformer">false</prop>
        props.put("datanucleus.jpa.addClassTransformer", String.valueOf(false));
        //        <prop key="datanucleus.connectionPoolingType">ConfiguredDataSource</prop>
        props.put("datanucleus.connectionPoolingType",
                PersistenceUnitContextConnectionPoolFactory.class.getSimpleName());
        //        <prop key="datanucleus.identifier.case">PreserveCase</prop>
        props.put("datanucleus.identifier.case", "MixedCase");
        //        <prop key="datanucleus.plugin.pluginRegistryBundleCheck">NONE</prop>
        props.put("datanucleus.plugin.pluginRegistryBundleCheck", "NONE");
        //        <prop key="datanucleus.plugin.allowUserBundles">true</prop>
        props.put("datanucleus.plugin.allowUserBundles", String.valueOf(true));
        //        <prop key="datanucleus.managedRuntime">true</prop>
        props.put("datanucleus.managedRuntime", String.valueOf(true));
        //        <prop key="datanucleus.cache.level2.type">Soft</prop>
        //        <!-- <prop key="datanucleus.cache.level2.type">EHCache</prop> cannot be found interestingly...-->
        props.put("datanucleus.cache.level2.type", "EHCache");
        //        <prop key="datanucleus.cache.level2.cacheName">org.datanucleus.L2Cache</prop>
        props.put("datanucleus.cache.level2.cacheName", "org.datanucleus.L2Cache");
        //        <prop key="datanucleus.cache.level2.configurationFile">#{ehCacheConfigurationMerger.newEhCacheXml().toString()}</prop>
        final EhCacheConfigurationMerger ehCacheConfigurationMerger = MergedContext.getInstance()
                .getBean(EhCacheConfigurationMerger.class);
        Assertions.assertThat(ehCacheConfigurationMerger.newEhCacheXml()).isNotNull();
        props.put("datanucleus.cache.level2.configurationFile",
                "/" + EhCacheConfigurationMerger.DEFAULT_EHCACHE_CLASSPATH_LOCATION);
        //        <!-- interestingly doesn't work <prop key="datanucleus.cache.queryResults.type">EHCache</prop>-->
        //        <prop key="datanucleus.findObject.validateWhenCached">false</prop>
        props.put("datanucleus.findObject.validateWhenCached", String.valueOf(false));
        return props;
    }

    @Override
    public PersistenceProvider getPersistenceProvider(final PersistenceUnitContext context) {
        return new PersistenceProviderImpl();
    }

    @Override
    public JpaVendorAdapter getJpaVendorAdapter(final PersistenceUnitContext context) {
        return null;
    }

    @Override
    public void setCacheable(final PersistenceUnitContext context, final IConfigurableQuery query,
            final boolean cacheable) {
        query.setHint(org.datanucleus.store.query.Query.EXTENSION_RESULTS_CACHED, String.valueOf(cacheable));
    }

    @Override
    public Set<ConnectionDialect> getSupportedDialects() {
        final Set<ConnectionDialect> supportedDialects = new HashSet<ConnectionDialect>();
        for (final ConnectionDialect dialect : ConnectionDialect.values()) {
            if (dialect.isRdbms()) {
                supportedDialects.add(dialect);
            }
        }
        return supportedDialects;
    }

    @Override
    public IDialectSpecificDelegate getOverriddenParent() {
        return null;
    }

    @Override
    public JpaDialect getJpaDialect(final PersistenceUnitContext persistenceUnitContext) {
        return null;
    }

}
