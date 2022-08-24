package com.function;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.util.MongoCrypt;
import com.microsoft.azure.functions.*;

public class RotateKey {

    @FunctionName("RotateKey")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", 
                methods = {HttpMethod.PUT}, 
                authLevel = AuthorizationLevel.FUNCTION,
                route = "rotatekey/{keyName}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("keyName") String keyName,
            final ExecutionContext context) {
        
        try
        {
            var mongoCrypt = new MongoCrypt(null, keyName);

            Boolean success = mongoCrypt.RotateKey();

            context.getLogger().info("Key "+ keyName + " rotated. Succeeded': " + success);

            return request.createResponseBuilder(HttpStatus.ACCEPTED).build();
        }
        catch (Exception ex)
        {
            context.getLogger().warning("Key rotation error. Error: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).build();
        }


    }
}
