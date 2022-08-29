package com.function;

import java.util.List;
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

public class GetOrders {

    @FunctionName("GetOrders")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", 
                methods = {HttpMethod.GET}, 
                authLevel = AuthorizationLevel.FUNCTION,
                route = "orders") 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        try
        {
            List<Object> result = null;
            final Boolean autoDecrypt = Boolean.parseBoolean(request.getQueryParameters().get("autoDecrypt"));

            //If filtered by customerName (encrypted) requires client encryption
            if (request.getQueryParameters().containsKey("customerName"))
                result = Order.FindOrder("customerName", request.getQueryParameters().get("customerName"), true, autoDecrypt);
            else if (request.getQueryParameters().containsKey("customerId"))
                result = Order.FindOrder("customerId", Integer.parseInt(request.getQueryParameters().get("customerId")), false, autoDecrypt);
            else
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("customerName or customerId query parameters required!").build();

            if (result == null)
                return request.createResponseBuilder(HttpStatus.NOT_FOUND).build();
            else
                return request.createResponseBuilder(HttpStatus.OK).body(result).build();
        }
        catch (Exception ex)
        {
            context.getLogger().warning(ex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ex.getMessage()).build();
        }
    }
}
