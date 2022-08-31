## Execution statistics

Since `keys` collection and Key Vault are accessed based on the workload, below we have some load test statistics to help sizing correctly your collection and perhaps cost estimation.

Load testing tool: <a href="https://docs.microsoft.com/en-us/azure/load-testing/" target="_blank">Azure Load Testing</a>

```
Test Environment:

Function -> Elastic Premium Plan EP1 (1 vCore)
Cosmos DB Mongo API:
    Orders collection: Auto scale 20,000 RU/s Max
    Keys collection  : Auto scale 4,000 RU/s Max
```

### Scenario 1: 100 simultaneous requests

**Create order API**

1. Function execution

   ![Function Create 100rps](./images/function_create_100rps.jpg)

1. Load Test statistics

   ![Load Test Create 100rps](./images/loadtest_create_100rps.jpg)

1. Cosmos `keys` collections queries per minute

   ![Cosmos Queries Create 100rps](./images/cosmos_queries_create_100rps.jpg)

1. Cosmos `keys` collections Request Charge per minute

   ![Cosmos Request Charge Create 100rps](./images/cosmos_request_charge_create_100rps.jpg)

1. Key Vault hits

   ![Key Vault Create 100rps](./images/keyvault_create_100rps.jpg)

<br/>

**Get order API by Customer Id with Auto Decrypt**

1. Function execution

   ![Function Get 100rps](./images/function_get_100rps.jpg)

1. Load Test statistics

   ![Load Test Get 100rps](./images/loadtest_get_100rps.jpg)

1. Cosmos `keys` collections queries per minute

   ![Cosmos Queries Get 100rps](./images/cosmos_queries_get_100rps.jpg)

1. Cosmos `keys` collections Request Charge per minute

   ![Cosmos Request Charge Get 100rps](./images/cosmos_request_charge_get_100rps.jpg)

1. Key Vault hits

   ![Key Vault Get 100rps](./images/keyvault_get_100rps.jpg)

<br/>

### Scenario 2: 250 simultaneous requests

**Create order API**

1. Function execution

   ![Function Create 250rps](./images/function_create_250rps.jpg)

1. Load Test statistics

   ![Load Test Create 250rps](./images/loadtest_create_250rps.jpg)

1. Cosmos `keys` collections queries per minute

   ![Cosmos Queries Create 250rps](./images/cosmos_queries_create_250rps.jpg)

1. Cosmos `keys` collections Request Charge per minute

   ![Cosmos Request Charge Create 250rps](./images/cosmos_request_charge_create_250rps.jpg)

1. Key Vault hits

   ![Key Vault Create 250rps](./images/keyvault_create_250rps.jpg)

<br/>

**Get order API by Customer Id with Auto Decrypt**

1. Function execution

   ![Function Get 250rps](./images/function_get_250rps.jpg)

1. Load Test statistics

   ![Load Test Get 250rps](./images/loadtest_get_250rps.jpg)

1. Cosmos `keys` collections queries per minute

   ![Cosmos Queries Get 250rps](./images/cosmos_queries_get_250rps.jpg)

1. Cosmos `keys` collections Request Charge per minute

   ![Cosmos Request Charge Get 250rps](./images/cosmos_request_charge_get_250rps.jpg)

1. Key Vault hits

   ![Key Vault Get 250rps](./images/keyvault_get_250rps.jpg)