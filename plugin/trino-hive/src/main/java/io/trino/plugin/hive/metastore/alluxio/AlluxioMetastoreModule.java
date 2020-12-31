/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.hive.metastore.alluxio;

import alluxio.ClientContext;
import alluxio.client.table.RetryHandlingTableMasterClient;
import alluxio.client.table.TableMasterClient;
import alluxio.conf.InstancedConfiguration;
import alluxio.conf.PropertyKey;
import alluxio.master.MasterClientContext;
import alluxio.util.ConfigurationUtils;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.airlift.configuration.AbstractConfigurationAwareModule;
import io.prestosql.plugin.hive.metastore.HiveMetastore;

import static io.airlift.configuration.ConfigBinder.configBinder;
import static org.weakref.jmx.guice.ExportBinder.newExporter;

/**
 * Module for an Alluxio metastore implementation of the {@link HiveMetastore} interface.
 */
public class AlluxioMetastoreModule
        extends AbstractConfigurationAwareModule
{
    @Override
    protected void setup(Binder binder)
    {
        configBinder(binder).bindConfig(AlluxioHiveMetastoreConfig.class);

        binder.bind(HiveMetastore.class).to(AlluxioHiveMetastore.class).in(Scopes.SINGLETON);
        newExporter(binder).export(HiveMetastore.class).as(generator -> generator.generatedNameOf(AlluxioHiveMetastore.class));
    }

    @Provides
    public TableMasterClient provideCatalogMasterClient(AlluxioHiveMetastoreConfig config)
    {
        return createCatalogMasterClient(config);
    }

    public static TableMasterClient createCatalogMasterClient(AlluxioHiveMetastoreConfig config)
    {
        InstancedConfiguration conf = new InstancedConfiguration(ConfigurationUtils.defaults());
        String addr = config.getMasterAddress();
        String[] parts = addr.split(":", 2);
        conf.set(PropertyKey.MASTER_HOSTNAME, parts[0]);
        if (parts.length > 1) {
            conf.set(PropertyKey.MASTER_RPC_PORT, parts[1]);
        }
        MasterClientContext context = MasterClientContext
                .newBuilder(ClientContext.create(conf)).build();
        return new RetryHandlingTableMasterClient(context);
    }
}