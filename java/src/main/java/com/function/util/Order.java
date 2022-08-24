package com.function.util;

import java.time.Instant;
import java.util.*;
import java.util.List;

import org.bson.*;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;

public class Order {

    final private static MongoCrypt _mongoCrypt = new MongoCrypt(System.getenv("mongoOrderConnectionString"), System.getenv("orderKeyName"));

    private static MongoCollection<Document> OrderCollection(MongoClient client)
    {
        return client.getDatabase("db1").getCollection("orders");
    }

    public static List<Object> FindOrder(String field, Object inputFilter, Boolean encryptFilter, Boolean autodecrypt)
    {
        BsonValue filterValue = null;
        
        if (inputFilter instanceof Integer)
            filterValue = new BsonInt32((Integer)inputFilter);
        else if (inputFilter instanceof String)
            filterValue = new BsonString((String)inputFilter);
        
        var filter = Filters.eq(field, (encryptFilter ? _mongoCrypt.EncryptField(filterValue, true) : filterValue));

        if (autodecrypt)
            return OrderCollection(_mongoCrypt.AutoDecryptionClient()).find(filter).into(new ArrayList<>()); //map(doc -> BsonDocument.parse(doc.toJson()))
        else
            return OrderCollection(_mongoCrypt.DefaultClient()).find(filter).into(new ArrayList<>());
    }

    public static void InsertOrder(String input)
    {
        final Boolean encryptPII = true;

        var order = BsonDocument.parse(input);
        order.append("createDate", new BsonDateTime(Instant.now().toEpochMilli()));

        if (encryptPII)
        {
            order.replace("customerName", _mongoCrypt.EncryptField(order.get("customerName"), true));
            order.replace("shippingAddress", _mongoCrypt.EncryptField(order.get("shippingAddress"), false));
        }

        OrderCollection(_mongoCrypt.AutoDecryptionClient()).insertOne(Document.parse(order.toJson()));
    }
}
