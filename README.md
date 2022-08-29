# Cosmos Mongo API Client Encryption

## Introduction

This repository provides a code sample in .NET and Java on how to use MongoDB Client-Side Field Level Encryption with Azure Cosmos DB Mongo API 4.2 (<a href="https://docs.microsoft.com/en-us/azure/cosmos-db/mongodb/feature-support-42#client-side-field-level-encryption" target="_blank">Feature support version 4.2</a>).

### 1. Requirements

> It's recommended to create all the resources within the same region.

* <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal#register-an-application-with-azure-ad-and-create-a-service-principal" target="_blank">Create a service principal and secret.</a> Store TenantId, ClientId and the secret temporarely for next step reuse.

* <a href="https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-function-app-portal#create-a-function-app" target="_blank">Create a Function App.</a> Choose the Runtime stack accordingly (sample code provided in **.NET 6** or **Java 11**).

* <a href="https://docs.microsoft.com/en-us/azure/key-vault/general/quick-create-portal" target="_blank">Create a Key Vault.</a>

* <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/mongodb/quickstart-dotnet?tabs=azure-portal%2Cwindows#create-an-azure-cosmos-db-account" target="_blank">Create a Cosmos DB Mongo API account.</a>

* <a href="https://github.com/fonsecamar/cosmos-mongo-encryption.git">Clone this repository.</a>

### 2. Configuration

* <a href="https://docs.microsoft.com/en-us/azure/app-service/overview-managed-identity?tabs=portal%2Chttp#add-a-system-assigned-identity" target="_blank">Enable system-assigned idenity on your Function App.</a>

* Configure Key Vault policies and secrets

* <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/mongodb/how-to-create-container-mongodb#portal-mongodb" target="_blank">Create a database and a collection.</a> Choose `Unlimited` **Storage capacity** and provide `customerId` as the **Shard key**.

* Configure application settings

* Deploy Function application

### 3. Running the sample

1. Call CreateKey function
1. Check Key Vault Key and keys collection on Cosmos
1. Call CreateOrder function
1. Call GetOrders function with different autoDecrypt values
1. Call RotateKey function
1. Check new version of Key Vault Key and keys collection on Cosmos
1. Call GetOrders function with different autoDecrypt values