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
package io.inversion.demo.keyvault;

import java.util.LinkedHashSet;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;

import com.azure.core.http.rest.PagedIterable;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;

import io.inversion.Api;
import io.inversion.action.misc.MockAction;
import io.inversion.spring.InversionApp;
import io.inversion.utils.Config;
import io.inversion.utils.Utils;

/**
 * Example of how to add secrets from an Azure KeyVault into the Inversion configuration at runtime. 
 */
public class KeyVaultDemoMain
{
   public static void main(String[] args)
   {
      //-- pull all the secrets from the KeyVault
      //-- for demo purposes, assume there are secrets for 'myAction.statusCode' and 'myAction.jsonUrl' 
      PropertiesConfiguration secretsConf = new PropertiesConfiguration();

      //-- TODO: this KeyVault integration has not been tested.  The main point of this example is 
      //-- to show you how to customize the configuration, not how to use KeyVault.
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
      //-- end secrets lookup

      //-- now we are going to cause the default configration to be  
      //-- loaded and then augment it with the keyvault properties
      String configPath = Utils.findProperty("configPath");
      String configProfile = Utils.findProperty("configProfile", "profile");

      Config.loadConfiguration(configPath, configProfile);//this loads the default configuration
      CompositeConfiguration config = Config.getConfiguration();

      //-- add the secrets to the start of the composite list so that keyvaut values are pulled first
      config.addConfigurationFirst(secretsConf);

      //-- now wire up your api. bean properties for all named objects will be reflectively set 
      MockAction mockAction = new MockAction().withName("myAction");
      Api api = new Api().withEndpoint("*", "*", mockAction);

      //-- Running the app causes Engine.startup to be called which is where
      //-- the reflective bean property setting happens.
      InversionApp.run(api);

      System.out.println("Azure KeyVault Secret 'myAction.statusCode' has been used to configure the action with property 'statusCode' value: " + mockAction.getStatusCode());
      System.out.println("Azure KeyVault Secret 'myAction.jsonUrl' has been used to configure the action with 'jsonUrl' value: " + mockAction.getJsonUrl());

   }

}
