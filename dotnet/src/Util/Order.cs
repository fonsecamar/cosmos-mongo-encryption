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
        //Instance MongoCrypt class
        private static MongoCrypt _mongoCrypt = new MongoCrypt(Environment.GetEnvironmentVariable("mongoOrderConnectionString"), Environment.GetEnvironmentVariable("orderKeyName"));

        private static IMongoCollection<BsonDocument> OrderCollection(MongoClient client)
        {
            //Returns collection
            return client.GetDatabase(Environment.GetEnvironmentVariable("databaseName")).GetCollection<BsonDocument>(Environment.GetEnvironmentVariable("collectionName"));
        }

        public static async Task InsertOrderAsync(string input, bool encryptPII = true)
        {
            //Parse json provided and adds createData attribute
            var order = BsonDocument.Parse(input);
            order.Add("createDate", DateTime.UtcNow);

            if (encryptPII)
            {
                //Calls Explicity Client Encryption for PII data
                order["customerName"] = await _mongoCrypt.EncryptFieldAsync(order["customerName"], true);
                order["shippingAddress"] = await _mongoCrypt.EncryptFieldAsync(order["shippingAddress"]);
            }

            //Stores new order
            await OrderCollection(_mongoCrypt.AutoDecryptionClient).InsertOneAsync(order.ToBsonDocument());
        }

        public static async Task<List<dynamic>> FindOrderAsync(string field, BsonValue filterValue, bool encryptFilter = false, bool autodecrypt = true)
        {
            //Build query filter and encrypt search value if encryptFilter = true
            var filter = Builders<BsonDocument>.Filter.Eq(field, (encryptFilter ? await _mongoCrypt.EncryptFieldAsync(filterValue, true) : filterValue));

            //If autodecrypt == true, uses client connection with encryption settings to allow Auto Decryption. 
            //Otherwise, uses default connections and returns encrypted data as binary data.
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

            //Cast Binary to string
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
