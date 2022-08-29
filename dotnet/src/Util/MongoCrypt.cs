using MongoDB.Bson;
using MongoDB.Driver;
using MongoDB.Driver.Encryption;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace MongoEncryption.Util
{
    internal class MongoCrypt
    {
        private CollectionNamespace _vaultNamespace = null;
        private Dictionary<string, IReadOnlyDictionary<string, object>> _kmsProviders = null;
        private ClientEncryption _clientEncryption = null;
        private string _vaultEndpoint = String.Empty;
        private string _keyName = String.Empty;

        private string _clientConnectionString = String.Empty;
        private MongoClient _autoDecryptionClient = null;
        private MongoClient _defaultClient = null;

        public MongoCrypt(string connectionString, string keyName)
        {
            _clientConnectionString = connectionString;
            _keyName = keyName;

            var vaultConnectionString = Environment.GetEnvironmentVariable("mongoVaultConnection");
            _vaultNamespace = CollectionNamespace.FromFullName(Environment.GetEnvironmentVariable("mongoVaultNamespace"));
            _vaultEndpoint = Environment.GetEnvironmentVariable("vaultEndpoint");

            _kmsProviders = new Dictionary<string, IReadOnlyDictionary<string, object>>();

            //Get service principal details from config
            var azureTenantId = Environment.GetEnvironmentVariable("encryptionPrincipalTenantId");
            var azureClientId = Environment.GetEnvironmentVariable("encryptionPrincipalClientId");
            var azureClientSecret = Environment.GetEnvironmentVariable("encryptionPrincipalClientSecret");

            //Build Dictionary options required to access Key Vault from the driver
            var azureKmsOptions = new Dictionary<string, object>
            {
                { "tenantId", azureTenantId },
                { "clientId", azureClientId },
                { "clientSecret", azureClientSecret },
            };

            //Map Azure provider and options
            _kmsProviders.Add("azure", azureKmsOptions);

            //Build Mongo connection string for encryption collection
            var vaultClient = new MongoClient(vaultConnectionString);

            //Build client encryption options
            var clientEncryptionOptions = new ClientEncryptionOptions(
                keyVaultClient: vaultClient,
                keyVaultNamespace: _vaultNamespace,
                kmsProviders: _kmsProviders);
            
            //Create client encryption instance
            _clientEncryption = new ClientEncryption(clientEncryptionOptions);
        }

        public MongoClient AutoDecryptionClient
        {
            get
            {
                if (_autoDecryptionClient == null)
                {
                    //Build auto encryption settings, providing vault details
                    var clientSettingsAutoDecrypt = MongoClientSettings.FromConnectionString(_clientConnectionString);
                    var autoEncryptionOptions = new AutoEncryptionOptions(
                        keyVaultNamespace: _vaultNamespace,
                        kmsProviders: _kmsProviders,
                        bypassAutoEncryption: true); //Must be true to skip auto encryption (not supported on Mongo Community or Cosmos DB)
                    clientSettingsAutoDecrypt.AutoEncryptionOptions = autoEncryptionOptions;

                    //Create mongo client instance with encryption settings
                    _autoDecryptionClient = new MongoClient(clientSettingsAutoDecrypt);
                }

                return _autoDecryptionClient;
            }
        }

        public MongoClient DefaultClient
        {
            get
            {
                //Create mongo client instance without encryption settings
                if (_defaultClient == null)
                    _defaultClient = new MongoClient(_clientConnectionString);

                return _defaultClient;
            }
        }

        public async Task<Guid> CreateKeyAsync()
        {
            //Calls Key Vault key creation
            var keyVersion = await KeyVaultOperations.CreateKeyAsync(_vaultEndpoint, _keyName);

            //Create data key options with key details to store in mongo collection
            var dataKeyOptions = new DataKeyOptions(
                            masterKey: new BsonDocument
                            {
                                { "keyName", _keyName },
                                { "keyVaultEndpoint", _vaultEndpoint },
                                { "keyVersion", keyVersion }
                            },
                            alternateKeyNames: new List<string>()
                            {
                                { _keyName } //AltKey: friendly name to retrieve the key
                            }
                            );

            //Stores key details in mongo collection
            var dataKeyId = await _clientEncryption.CreateDataKeyAsync("azure", dataKeyOptions);

            return dataKeyId;
        }


        public async Task<bool> RotateKeyAsync()
        {
            //Calls Key Vault key rotation
            var keyVersion = await KeyVaultOperations.RotateKeyAsync(_vaultEndpoint, _keyName);

            //Create rewrap data key options with key details to store in mongo collection (new key version)
            var rewrapDataKey = new RewrapManyDataKeyOptions("azure",
                masterKey: new BsonDocument
                {
                    { "keyName", _keyName },
                    { "keyVaultEndpoint", _vaultEndpoint },
                    { "keyVersion", keyVersion }
                }
                );

            //Rewraps DEK and stores new version in mongo collection
            var dataKey = await _clientEncryption.RewrapManyDataKeyAsync(new BsonDocument { { "masterKey.keyName", _keyName } }, rewrapDataKey);

            return dataKey.BulkWriteResult.IsAcknowledged;
        }

        public async Task<BsonValue> EncryptFieldAsync(BsonValue field, bool isDeterministic = false)
        {
            //Explicit Client Encryption used for storing encrypted data or encrypting deterministic search fields
            var encryptedField = await _clientEncryption.EncryptAsync(
                field,
                new EncryptOptions(algorithm: (isDeterministic ? "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic" : "AEAD_AES_256_CBC_HMAC_SHA_512-Random"),
                    alternateKeyName: _keyName),
                CancellationToken.None);

            return encryptedField;
        }
    }
}
