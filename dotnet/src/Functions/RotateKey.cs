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
    public static class RotateKey
    {
        [FunctionName("RotateKey")]
        public static async Task<IActionResult> RunAsync(
            [HttpTrigger(AuthorizationLevel.Function, "put", Route = "rotatekey/{keyName}")] HttpRequest req,
            string keyName,
            ILogger log)
        {
            try
            {
                var mongoCrypt = new MongoCrypt(null, keyName);

                var success = await mongoCrypt.RotateKeyAsync();

                log.LogInformation($"Key {keyName} rotated. Succeeded: {success}");

                return new AcceptedResult();
            }
            catch(Exception ex)
            {
                log.LogError(ex, "Key rotation error");
                return new BadRequestResult();
            }
        }
    }
}
