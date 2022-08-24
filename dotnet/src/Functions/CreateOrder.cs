using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using MongoEncryption.Util;
using System;
using System.IO;
using System.Threading.Tasks;

namespace MongoEncryption.Functions
{
    public static class CreateOrder
    {
        [FunctionName("CreateOrder")]
        public static async Task<IActionResult> Run(
            [HttpTrigger(AuthorizationLevel.Function, "post", Route = "orders/create")] HttpRequest req,
            ILogger log)
        {
            try
            {
                string requestBody = await new StreamReader(req.Body).ReadToEndAsync();

                await Order.InsertOrderAsync(requestBody);

                return new AcceptedResult();
            }
            catch (Exception ex)
            {
                log.LogError(ex, "Order creation error");
                return new BadRequestResult();
            }
        }
    }
}
