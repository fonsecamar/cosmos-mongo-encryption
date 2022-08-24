package com.function;

import com.function.util.MongoCrypt;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

public class CreateKey {
    @FunctionName("CreateKey")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.PUT},
                authLevel = AuthorizationLevel.FUNCTION, 
                route = "createkey/{keyName}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("keyName") String keyName,
            final ExecutionContext context) {

        try
        {
            var mongoCrypt = new MongoCrypt(null, keyName);

            String keyId = mongoCrypt.CreateKey();

            context.getLogger().info("Key "+ keyName + " created. Id': " + keyId);

            return request.createResponseBuilder(HttpStatus.ACCEPTED).build();
        }
        catch (Exception ex)
        {
            context.getLogger().warning("Key creation error. Error: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).build();
        }
    }
}
