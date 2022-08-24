package com.function.util;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyOperation;
import com.azure.security.keyvault.keys.models.KeyVaultKey;

public class KeyVaultOperations {

    public static String CreateKey(String vaultEndpoint, String keyName)
    {
        var kvClient = new KeyClientBuilder()
                            .vaultUrl("https://"+vaultEndpoint)
                            .credential(new DefaultAzureCredentialBuilder().build())
                            .buildClient();

        var keyOptions = new CreateRsaKeyOptions(keyName)
                            .setEnabled(true)
                            .setKeyOperations(KeyOperation.WRAP_KEY, KeyOperation.UNWRAP_KEY);

        KeyVaultKey newKey = kvClient.createRsaKey(keyOptions);

        return newKey.getProperties().getVersion();
    }

    public static String RotateKey(String vaultEndpoint, String keyName)
    {
        return CreateKey(vaultEndpoint, keyName);
    }
}
