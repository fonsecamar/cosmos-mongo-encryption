package com.function.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.model.vault.EncryptOptions;
import com.mongodb.client.model.vault.RewrapManyDataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;

public class MongoCrypt {

    private String _vaultNamespace = null;
    private Map<String, Map<String, Object>> _kmsProviders = null;
    private ClientEncryption _clientEncryption = null;
    private String _vaultEndpoint = null;
    private String _keyName = null;

    private String _clientConnectionString = null;
    private MongoClient _autoDecryptionClient = null;
    private MongoClient _defaultClient = null;

    public MongoCrypt(String connectionString, String keyName)
    {
        _clientConnectionString = connectionString;
        _keyName = keyName;

        var vaultConnectionString = System.getenv("mongoVaultConnection");
        _vaultNamespace = System.getenv("mongoVaultNamespace"); //database.collection used to store key references
        _vaultEndpoint = System.getenv("vaultEndpoint");

        _kmsProviders = new HashMap<String, Map<String, Object>>();

        //Get service principal details from config
        var azureTenantId = System.getenv("encryptionPrincipalTenantId");
        var azureClientId = System.getenv("encryptionPrincipalClientId");
        var azureClientSecret = System.getenv("encryptionPrincipalClientSecret");

        //Build hashmap options required to access Key Vault from the driver
        var azureKmsOptions = new HashMap<String, Object>() {{
            put("tenantId", azureTenantId);
            put("clientId", azureClientId);
            put("clientSecret", azureClientSecret);
        }};

        //Map Azure provider and options
        _kmsProviders.put( "azure", azureKmsOptions);

        //Build Mongo connection string for encryption collection
        var kvmcs = MongoClientSettings.builder()
                                        .applyConnectionString(new ConnectionString(vaultConnectionString))
                                        .build();

        //Build client encryption options
        var clientEncryptionOptions = ClientEncryptionSettings.builder()
                                                               .keyVaultMongoClientSettings(kvmcs)
                                                               .keyVaultNamespace(_vaultNamespace)
                                                               .kmsProviders(_kmsProviders)
                                                               .build();

        //Create client encryption instance
        _clientEncryption = ClientEncryptions.create(clientEncryptionOptions);
    }

    public MongoClient AutoDecryptionClient()
    {
        if (_autoDecryptionClient == null)
        {
            //Build auto encryption settings, providing vault details
            var autoEncryptionOptions = AutoEncryptionSettings.builder()
                                            .keyVaultNamespace(_vaultNamespace)
                                            .kmsProviders(_kmsProviders)
                                            .bypassAutoEncryption(true) //Must be true to skip auto encryption (not supported on Mongo Community or Cosmos DB)
                                            .build();

            //Build mongo client settings
            var kvmcs = MongoClientSettings.builder()
                                            .applyConnectionString(new ConnectionString(_clientConnectionString))
                                            .autoEncryptionSettings(autoEncryptionOptions)
                                            .build();

            //Create mongo client instance with encryption settings
            _autoDecryptionClient = MongoClients.create(kvmcs);
        }

        return _autoDecryptionClient;
    }

    public MongoClient DefaultClient()
    {
        //Create mongo client instance without encryption settings
        if (_defaultClient == null)
            _defaultClient = MongoClients.create(MongoClientSettings.builder()
                                                                        .applyConnectionString(new ConnectionString(_clientConnectionString))
                                                                        .build());
        
        return _defaultClient;
    }

    public String CreateKey()
    {
        //Calls Key Vault key creation
        var keyVersion = KeyVaultOperations.CreateKey(_vaultEndpoint, _keyName);

        //Build BSON document with key details to store in mongo collection
        var masterKey = new BsonDocument();
        masterKey.append("keyName", new BsonString(_keyName))
                .append("keyVaultEndpoint", new BsonString(_vaultEndpoint))
                .append("keyVersion", new BsonString(keyVersion));

        var altKey = new ArrayList<String>();
        altKey.add(_keyName); //AltKey: friendly name to retrieve the key

        //Create data key options
        var dataKeyOptions = new DataKeyOptions().masterKey(masterKey).keyAltNames(altKey);

        //Stores key details in mongo collection
        var dataKeyId = _clientEncryption.createDataKey("azure", dataKeyOptions);

        return dataKeyId.asUuid().toString();
    }

    public Boolean RotateKey()
    {
        //Calls Key Vault key rotation
        var keyVersion = KeyVaultOperations.RotateKey(_vaultEndpoint, _keyName);

        //Build BSON document with key details to store in mongo collection (new key version)
        var rewrapDataKey = new BsonDocument();
        rewrapDataKey.append("keyName", new BsonString(_keyName))
                .append("keyVaultEndpoint", new BsonString(_vaultEndpoint))
                .append("keyVersion", new BsonString(keyVersion));

        //Create rewrap data key options
        var rewrapDataKeyOptions = new RewrapManyDataKeyOptions().provider("azure").masterKey(rewrapDataKey);

        //Rewraps DEK and stores new version in mongo collection
        var dataKey = _clientEncryption.rewrapManyDataKey(Filters.eq("masterKey.keyName", _keyName), rewrapDataKeyOptions);

        return dataKey.getBulkWriteResult().getModifiedCount() != 0;
    }

    public BsonBinary EncryptField(BsonValue field, Boolean isDeterministic)
    {
        //Explicit Client Encryption used for storing encrypted data or encrypting deterministic search fields
        var encryptedField = _clientEncryption.encrypt(
            field,
            new EncryptOptions((isDeterministic ? "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic" : "AEAD_AES_256_CBC_HMAC_SHA_512-Random")).keyAltName(_keyName));

        return encryptedField;
    }
}
