package com.function;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.util.*;
import com.microsoft.azure.functions.*;

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