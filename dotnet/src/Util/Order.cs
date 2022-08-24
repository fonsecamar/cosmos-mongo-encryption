using MongoDB.Bson;
using MongoDB.Driver;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace MongoEncryption.Util
{
    internal static class Order
    {
        private static MongoCrypt _mongoCrypt = new MongoCrypt(Environment.GetEnvironmentVariable("mongoOrderConnectionString"), Environment.GetEnvironmentVariable("orderKeyName"));

        private static IMongoCollection<BsonDocument> OrderCollection(MongoClient client)
        {
            return client.GetDatabase("db1").GetCollection<BsonDocument>("orders");
        }

        public static async Task InsertOrderAsync(string input, bool encryptPII = true)
        {
            var order = BsonDocument.Parse(input);
            order.Add("createDate", DateTime.UtcNow);

            if (encryptPII)
            {
                order["customerName"] = await _mongoCrypt.EncryptFieldAsync(order["customerName"], true);
                order["shippingAddress"] = await _mongoCrypt.EncryptFieldAsync(order["shippingAddress"]);
            }

            await OrderCollection(_mongoCrypt.AutoDecryptionClient).InsertOneAsync(order.ToBsonDocument());
        }

        public static async Task<List<dynamic>> FindOrderAsync(string field, BsonValue filterValue, bool encryptFilter = false, bool autodecrypt = true)
        {
            var filter = Builders<BsonDocument>.Filter.Eq(field, (encryptFilter ? await _mongoCrypt.EncryptFieldAsync(filterValue, true) : filterValue));

            if (autodecrypt)
                return (await OrderCollection(_mongoCrypt.AutoDecryptionClient).FindAsync<dynamic>(filter)).ToList<dynamic>();
            else
            {
                var list = (await OrderCollection(_mongoCrypt.DefaultClient).FindAsync<BsonDocument>(filter)).ToList<BsonDocument>();

                var result = list.ConvertAll(new Converter<BsonDocument, dynamic>(ConvertBinary));

                return result;
            }
        }

        private static dynamic ConvertBinary(BsonDocument input)
        {
            Dictionary<string, dynamic> dict = new Dictionary<string, dynamic>();

            foreach (var item in input.Elements)
            {
                if (item.Value.BsonType == BsonType.Binary)
                    dict.Add(item.Name, Convert.ToHexString(item.Value.AsByteArray));
                else
                    dict.Add(item.Name, item.Value);
            }

            return dict;
        }
    }
}
