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
        _vaultNamespace = System.getenv("mongoVaultNamespace");
        _vaultEndpoint = System.getenv("vaultEndpoint");

        _kmsProviders = new HashMap<String, Map<String, Object>>();

        var azureTenantId = System.getenv("encryptionPrincipalTenantId");
        var azureClientId = System.getenv("encryptionPrincipalClientId");
        var azureClientSecret = System.getenv("encryptionPrincipalClientSecret");

        var azureKmsOptions = new HashMap<String, Object>() {{
            put("tenantId", azureTenantId);
            put("clientId", azureClientId);
            put("clientSecret", azureClientSecret);
        }};

        _kmsProviders.put( "azure", azureKmsOptions);

        var kvmcs = MongoClientSettings.builder().applyConnectionString(new ConnectionString(vaultConnectionString)).build();

        var clientEncryptionOptions = ClientEncryptionSettings.builder()
                                                               .keyVaultMongoClientSettings(kvmcs)
                                                               .keyVaultNamespace(_vaultNamespace)
                                                               .kmsProviders(_kmsProviders)
                                                               .build();

        _clientEncryption = ClientEncryptions.create(clientEncryptionOptions);
    }

    public MongoClient AutoDecryptionClient()
    {
        if (_autoDecryptionClient == null)
        {
            var autoEncryptionOptions = AutoEncryptionSettings.builder()
                                            .keyVaultNamespace(_vaultNamespace)
                                            .kmsProviders(_kmsProviders)
                                            .bypassAutoEncryption(true)
                                            .build();

            var kvmcs = MongoClientSettings.builder()
                                            .applyConnectionString(new ConnectionString(_clientConnectionString))
                                            .autoEncryptionSettings(autoEncryptionOptions)
                                            .build();

                                            _autoDecryptionClient = MongoClients.create(kvmcs);
        }

        return _autoDecryptionClient;
    }

    public MongoClient DefaultClient()
    {
        if (_defaultClient == null)
            _defaultClient = MongoClients.create(_clientConnectionString);
        
        return _defaultClient;
    }

    public String CreateKey()
    {
        var keyVersion = KeyVaultOperations.CreateKey(_vaultEndpoint, _keyName);

        var masterKey = new BsonDocument();
        masterKey.append("keyName", new BsonString(_keyName))
                .append("keyVaultEndpoint", new BsonString(_vaultEndpoint))
                .append("keyVersion", new BsonString(keyVersion));

        var altKey = new ArrayList<String>();
        altKey.add(_keyName);

        var dataKeyOptions = new DataKeyOptions().masterKey(masterKey).keyAltNames(altKey);

        var dataKeyId = _clientEncryption.createDataKey("azure", dataKeyOptions);

        return dataKeyId.asUuid().toString();
    }

    public Boolean RotateKey()
    {
        var keyVersion = KeyVaultOperations.RotateKey(_vaultEndpoint, _keyName);

        var rewrapDataKey = new BsonDocument();
        rewrapDataKey.append("keyName", new BsonString(_keyName))
                .append("keyVaultEndpoint", new BsonString(_vaultEndpoint))
                .append("keyVersion", new BsonString(keyVersion));

        var rewrapDataKeyOptions = new RewrapManyDataKeyOptions().provider("azure").masterKey(rewrapDataKey);

        var dataKey = _clientEncryption.rewrapManyDataKey(Filters.eq("masterKey.keyName", _keyName), rewrapDataKeyOptions);

        return dataKey.getBulkWriteResult().getModifiedCount() != 0;
    }

    public BsonBinary EncryptField(BsonValue field, Boolean isDeterministic)
    {
        var encryptedField = _clientEncryption.encrypt(
            field,
            new EncryptOptions((isDeterministic ? "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic" : "AEAD_AES_256_CBC_HMAC_SHA_512-Random")).keyAltName(_keyName));

        return encryptedField;
    }
}
