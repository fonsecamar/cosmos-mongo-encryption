package com.function.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class Order {

    //Instance MongoCrypt class
    final private static MongoCrypt _mongoCrypt = new MongoCrypt(System.getenv("mongoOrderConnectionString"), System.getenv("orderKeyName"));

    private static MongoCollection<Document> OrderCollection(MongoClient client)
    {
        //Returns collection
        return client.getDatabase(System.getenv("databaseName")).getCollection(System.getenv("collectionName"));
    }

    public static void InsertOrder(String input)
    {
        final Boolean encryptPII = true;

        //Parse json provided and adds createData attribute
        var order = BsonDocument.parse(input);
        order.append("createDate", new BsonDateTime(Instant.now().toEpochMilli()));

        if (encryptPII)
        {
            //Calls Explicity Client Encryption for PII data
            order.replace("customerName", _mongoCrypt.EncryptField(order.get("customerName"), true));
            order.replace("shippingAddress", _mongoCrypt.EncryptField(order.get("shippingAddress"), false));
        }

        //Stores new order
        OrderCollection(_mongoCrypt.AutoDecryptionClient()).insertOne(Document.parse(order.toJson()));
    }

    public static List<Object> FindOrder(String field, Object inputFilter, Boolean encryptFilter, Boolean autodecrypt)
    {
        BsonValue filterValue = null;
        
        //Cast filter based on filter type. This sample expects Integer or String filters.
        if (inputFilter instanceof Integer)
            filterValue = new BsonInt32((Integer)inputFilter);
        else if (inputFilter instanceof String)
            filterValue = new BsonString((String)inputFilter);

        //Build query filter and encrypt search value if encryptFilter = true
        var filter = Filters.eq(field, (encryptFilter ? _mongoCrypt.EncryptField(filterValue, true) : filterValue));

        //If autodecrypt == true, uses client connection with encryption settings to allow Auto Decryption. 
        //Otherwise, uses default connections and returns encrypted data as binary data.
        if (autodecrypt)
            return OrderCollection(_mongoCrypt.AutoDecryptionClient()).find(filter).into(new ArrayList<>()); //map(doc -> BsonDocument.parse(doc.toJson()))
        else
            return OrderCollection(_mongoCrypt.DefaultClient()).find(filter).into(new ArrayList<>());
    }
}