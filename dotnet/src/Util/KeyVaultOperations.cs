using Azure.Identity;
using Azure.Security.KeyVault.Keys;
using System;
using System.Threading.Tasks;

namespace MongoEncryption.Util
{
    internal static class KeyVaultOperations
    {
        //Create RSA Key on Key Vault
        public static async Task<string> CreateKeyAsync(string vaultEndpoint, string keyName)
        {
            var kvClient = new KeyClient(vaultUri: new Uri($"https://{vaultEndpoint}"), credential: new DefaultAzureCredential());

            var keyOptions = new CreateKeyOptions();
            keyOptions.Enabled = true;
            keyOptions.KeyOperations.Add(KeyOperation.UnwrapKey);
            keyOptions.KeyOperations.Add(KeyOperation.WrapKey);

            var newKey = await kvClient.CreateKeyAsync(keyName, KeyType.Rsa, keyOptions);

            return newKey.Value.Properties.Version;
        }

        //Rotate RSA Key on Key Vault.
        public static async Task<string> RotateKeyAsync(string vaultEndpoint, string keyName)
        {
            var kvClient = new KeyClient(vaultUri: new Uri($"https://{vaultEndpoint}"), credential: new DefaultAzureCredential());

            var newKey = await kvClient.RotateKeyAsync(keyName);

            return newKey.Value.Properties.Version;
        }
    }
}