package com.function;

import java.util.Optional;

import com.function.util.Order;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class CreateOrder {

    @FunctionName("CreateOrder")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", 
                methods = {HttpMethod.POST}, 
                authLevel = AuthorizationLevel.FUNCTION,
                route = "orders/create") 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        try
        {
            String requestBody = request.getBody().get();

            Order.InsertOrder(requestBody);

            return request.createResponseBuilder(HttpStatus.ACCEPTED).build();
        }
        catch (Exception ex)
        {
            context.getLogger().warning("Order creation error. Error: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).build();
        }
    }
}