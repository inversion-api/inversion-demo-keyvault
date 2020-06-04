/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.sandbox.keyvault;

import java.util.LinkedHashSet;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;

import com.azure.core.http.rest.PagedIterable;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;

import io.inversion.spring.InversionApp;
import io.inversion.utils.Config;
import io.inversion.utils.Utils;

public class KeyVaultDemoMain
{
   public static void main(String[] args)
   {
      String configPath = Utils.findProperty("configPath");
      String configProfile = Utils.findProperty("configProfile", "profile");

      Config.loadConfiguration(configPath, configProfile);//this loads the default configuration
      CompositeConfiguration config = Config.getConfiguration();

      PropertiesConfiguration secretsConf = new PropertiesConfiguration();
      config.addConfigurationFirst(secretsConf);

      InversionApp.run();
   }

   public static PropertiesConfiguration getSecrets()
   {
      PropertiesConfiguration secretsConf = new PropertiesConfiguration();

      String keyVaultName = System.getenv("KEY_VAULT_NAME");
      String kvUri = "https://" + keyVaultName + ".vault.azure.net";

      SecretClient secretClient = new SecretClientBuilder().//
                                                           vaultUrl(kvUri).//
                                                           credential(new DefaultAzureCredentialBuilder().build()).buildClient();

      PagedIterable<SecretProperties> secretIt = secretClient.listPropertiesOfSecrets();

      LinkedHashSet<String> names = new LinkedHashSet();
      secretIt.streamByPage().forEach(resp -> resp.getElements().stream().map(props -> names.add(props.getName())));

      for (String name : names)
      {
         KeyVaultSecret sec = secretClient.getSecret(name);
         secretsConf.setProperty(sec.getName(), sec.getValue());
      }

      return secretsConf;
   }
}
