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
    public static class GetOrders
    {
        [FunctionName("GetOrders")]
        public static async Task<IActionResult> RunAsync(
            [HttpTrigger(AuthorizationLevel.Function, 
            "get", 
            Route = "orders")] HttpRequest req,
            ILogger log)
        {
            try
            {
                object result = null;
                bool autoDecrypt = true;

                if (req.Query.ContainsKey("autoDecrypt"))
                    autoDecrypt = bool.Parse(req.Query["autoDecrypt"]);

                //If filtered by customerName (encrypted) requires client encryption
                if (req.Query.ContainsKey("customerName"))
                    result = await Order.FindOrderAsync("customerName", req.Query["customerName"].ToString(), true, autoDecrypt);
                else if (req.Query.ContainsKey("customerId"))
                    result = await Order.FindOrderAsync("customerId", int.Parse(req.Query["customerId"]), false, autoDecrypt);
                else
                    return new BadRequestObjectResult("customerName or customerId query parameters required!");

                if (result == null)
                    return new NotFoundResult();
                else
                    return new OkObjectResult(result);
            }
            catch (Exception ex)
            {
                log.LogError(ex, ex.Message);
                return new BadRequestObjectResult(ex.Message);
            }
        }
    }
}
