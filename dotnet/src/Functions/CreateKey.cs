using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using MongoEncryption.Util;
using System;
using System.Threading.Tasks;

namespace MongoEncryption.Functions
{
    public static class CreateKey
    {
        [FunctionName("CreateKey")]
        public static async Task<IActionResult> RunAsync(
            [HttpTrigger(AuthorizationLevel.Function, 
            "put", 
            Route = "createkey/{keyName}")] HttpRequest req,
            string keyName,
            ILogger log)
        {
            try
            {
                var mongoCrypt = new MongoCrypt(null, keyName);

                var keyId = await mongoCrypt.CreateKeyAsync();

                log.LogInformation($"Key {keyName} created. Id: {keyId}");

                return new AcceptedResult();
            }
            catch (Exception ex)
            {
                log.LogError(ex, "Key creation error");
                return new BadRequestResult();
            }
        }
    }
}
