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

            var azureTenantId = Environment.GetEnvironmentVariable("encryptionPrincipalTenantId");
            var azureClientId = Environment.GetEnvironmentVariable("encryptionPrincipalClientId");
            var azureClientSecret = Environment.GetEnvironmentVariable("encryptionPrincipalClientSecret");

            var azureKmsOptions = new Dictionary<string, object>
            {
                { "tenantId", azureTenantId },
                { "clientId", azureClientId },
                { "clientSecret", azureClientSecret },
            };

            _kmsProviders.Add("azure", azureKmsOptions);

            // New instance of CosmosClient class
            var vaultClient = new MongoClient(vaultConnectionString);

            var clientEncryptionOptions = new ClientEncryptionOptions(
                keyVaultClient: vaultClient,
                keyVaultNamespace: _vaultNamespace,
                kmsProviders: _kmsProviders);
            _clientEncryption = new ClientEncryption(clientEncryptionOptions);
        }

        public MongoClient AutoDecryptionClient
        {
            get
            {
                if (_autoDecryptionClient == null)
                {
                    var clientSettingsAutoDecrypt = MongoClientSettings.FromConnectionString(_clientConnectionString);
                    var autoEncryptionOptions = new AutoEncryptionOptions(
                        keyVaultNamespace: _vaultNamespace,
                        kmsProviders: _kmsProviders,
                        bypassAutoEncryption: true);
                    clientSettingsAutoDecrypt.AutoEncryptionOptions = autoEncryptionOptions;

                    _autoDecryptionClient = new MongoClient(clientSettingsAutoDecrypt);
                }

                return _autoDecryptionClient;
            }
        }

        public MongoClient DefaultClient
        {
            get
            {
                if (_defaultClient == null)
                    _defaultClient = new MongoClient(_clientConnectionString);

                return _defaultClient;
            }
        }

        public async Task<Guid> CreateKeyAsync()
        {
            var keyVersion = await KeyVaultOperations.CreateKeyAsync(_vaultEndpoint, _keyName);

            var dataKeyOptions = new DataKeyOptions(
                            masterKey: new BsonDocument
                            {
                                { "keyName", _keyName },
                                { "keyVaultEndpoint", _vaultEndpoint },
                                { "keyVersion", keyVersion }
                            },
                            alternateKeyNames: new List<string>()
                            {
                                { _keyName }
                            }
                            );

            var dataKeyId = await _clientEncryption.CreateDataKeyAsync("azure", dataKeyOptions);

            return dataKeyId;
        }


        public async Task<bool> RotateKeyAsync()
        {
            var keyVersion = await KeyVaultOperations.RotateKeyAsync(_vaultEndpoint, _keyName);

            var rewrapDataKey = new RewrapManyDataKeyOptions("azure",
                masterKey: new BsonDocument
                {
                    { "keyName", _keyName },
                    { "keyVaultEndpoint", _vaultEndpoint },
                    { "keyVersion", keyVersion }
                }
                );

            var dataKey = await _clientEncryption.RewrapManyDataKeyAsync(new BsonDocument { { "masterKey.keyName", _keyName } }, rewrapDataKey);

            return dataKey.BulkWriteResult.IsAcknowledged;
        }

        public async Task<BsonValue> EncryptFieldAsync(BsonValue field, bool isDeterministic = false)
        {
            var encryptedField = await _clientEncryption.EncryptAsync(
                field,
                new EncryptOptions(algorithm: (isDeterministic ? "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic" : "AEAD_AES_256_CBC_HMAC_SHA_512-Random"),
                    alternateKeyName: _keyName),
                CancellationToken.None);

            return encryptedField;
        }
    }
}
