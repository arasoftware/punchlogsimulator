package com.example.util;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public MongoDBConnection() {
        // Connection string to connect to MongoDB, replace with your connection string
        String connectionString = "mongodb://admin:nexu@192.168.1.2:27017";
        mongoClient = MongoClients.create(new ConnectionString(connectionString));
        database = mongoClient.getDatabase("simulator"); // Replace "mydatabase" with your database name
        collection = database.getCollection("logdata");
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void closeConnection() {
        mongoClient.close();
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }

    
}
