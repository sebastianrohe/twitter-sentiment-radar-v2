package com.project.sebastianrohe.twitter.database;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.project.sebastianrohe.twitter.data.Tweet;
import org.apache.uima.UIMAException;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

// This class will extent the solution of task 1. Was not part of the original requirements.
public class MongoDBConnectionHandler {

    private final String fileName;

    private MongoDatabase connectedDatabase;
    private MongoClient mongoClient;
    private static MongoCollection<Document> myCollection;

    /**
     * Constructor to handle connection and communication with mongodb.
     *
     * @param fileName File name of the properties file with login information to mongodb
     * @throws IOException If something goes wrong
     */
    public MongoDBConnectionHandler(String fileName) throws IOException {
        this.fileName = fileName;
        init(fileName);
    }

    public String getFileName() {
        return this.fileName;
    }

    public MongoDatabase getConnectedDatabase() {
        return this.connectedDatabase;
    }

    public MongoClient getMongoClient() {
        return this.mongoClient;
    }

    /**
     * Method to initialize com.project.sebastianrohe.twitter.database connection.
     *
     * @param fileName Name of the properties for com.project.sebastianrohe.twitter.database connection.
     * @throws IOException If something goes wrong.
     */
    @SuppressWarnings("deprecation")
    public void init(String fileName) throws IOException {
        // Create new properties instance.
        Properties propMongo = new Properties();
        propMongo.load(new FileInputStream(fileName));
        MongoCredential credential = MongoCredential.createCredential(propMongo.getProperty("username"),
                propMongo.getProperty("com/project/sebastianrohe/twitter/database"), propMongo.getProperty("password").toCharArray());

        MongoClientOptions options = MongoClientOptions.builder().sslEnabled(false).build();
        mongoClient = new MongoClient(new ServerAddress(propMongo.getProperty("host"),
                Integer.parseInt(propMongo.getProperty("port"))), Collections.singletonList(credential), options);
        connectedDatabase = mongoClient.getDatabase(propMongo.getProperty("com/project/sebastianrohe/twitter/database"));

        // Select collection 'Uebung2'.
        myCollection = connectedDatabase.getCollection("Uebung2");
        // Inform that Connection to mongodb was successful.
        System.out.println("Connection to MongoDB successful.");
    }

    /**
     * Method to convert tweet object to tweet document.
     *
     * @param tweet Accepts a tweet object.
     * @return Tweet converted to document.
     * @throws UIMAException If something goes wrong.
     */
    public static Document convertToMongoDocument(Tweet tweet) throws UIMAException {
        // Creating a empty document for MongoDB.
        Document tweetDocument = new Document();

        // Add attributes to document.
        tweetDocument.put("_id", tweet.getId());
        tweetDocument.put("user", tweet.getUser());
        tweetDocument.put("text", tweet.getText());
        tweetDocument.put("hashtags", new ArrayList<>(tweet.getHashtags()));
        tweetDocument.put("createAt", new Date(tweet.getDate().getTime()));
        tweetDocument.put("language", tweet.getLanguage());
        tweetDocument.put("retweet", tweet.getRetweet());
        tweetDocument.put("retweetId", tweet.getRetweetId());

        return tweetDocument;
    }

    /**
     * Method to check if tweet is already in mongo collection by checking ids.
     *
     * @param tweet Tweet from the set of all tweets from the read in csv file which should be checked.
     * @return Boolean to check if tweet is already in mongo collection or not.
     */
    public boolean checkIfDocumentIsAlreadyInDatabase(Tweet tweet) {
        // Creating query object to check by id.
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", tweet.getId());

        // Iterator to check in collection.
        FindIterable<Document> iterator = myCollection.find(whereQuery);

        // Variable tweetAlreadyInDatabase is 'false' by default.
        // This means the tweet is not in the collection.
        boolean tweetAlreadyInDatabase = false;

        // If one or more documents with given id are found by iterator, tweetAlreadyInDatabase is set to 'true'.
        // This means the tweet is already inside the collection.
        for (Document ignored : iterator)
            tweetAlreadyInDatabase = true;

        return tweetAlreadyInDatabase;
    }

    /**
     * This Method checks if a single tweet is already in the mongo collection, converts it to a document if not and returns
     * an empty document if the tweet is already in the collection or the resulting tweet document from the conversion.
     *
     * @param tweet Tweet object to insert in collection.
     * @return Empty document or tweet document if tweet was not already in mongo collection.
     */
    public Document insertTweetDocument(Tweet tweet) throws UIMAException {
        // Variable to check if tweet is already inside the collection.
        boolean isTweetInMongoDB = checkIfDocumentIsAlreadyInDatabase(tweet);

        // Document to insert is set to null by default.
        // This means we have an empty document by default.
        Document tweetDocumentToInsert = null;

        // If isTweetInMongoDB is 'false' tweet will get inserted.
        // This means if a tweet is not already in mongodb it will get added.
        if (!isTweetInMongoDB) {
            // Tweet object is converted to tweet document.
            tweetDocumentToInsert = convertToMongoDocument(tweet);
            // Insert resulting tweet document in collection.
            myCollection.insertOne(tweetDocumentToInsert);
        }

        return tweetDocumentToInsert;
    }

    /**
     * Method to insert many tweets in the mongodb.
     *
     * @param tweetObjectsSet Set of tweet objects which where created with the com.project.sebastianrohe.twitter.data from the read in csv file.
     * @throws UIMAException If something goes wrong.
     */
    public void insertManyTweetDocuments(Set<Tweet> tweetObjectsSet) throws UIMAException {
        // Insert tweet.by tweet for every tweet in given tweet set.
        for (Tweet tweet : tweetObjectsSet) {
            // Tweet will get converted or check document will get returned.
            Document insertDocument = insertTweetDocument(tweet);

            // If document is empty.
            if (insertDocument == null)
                // Give information when tweet is already in the com.project.sebastianrohe.twitter.database.
                System.err.println("Error: Already in com.project.sebastianrohe.twitter.database... " + tweet);

            else
                // Give information when tweet is not already and com.project.sebastianrohe.twitter.database and is added to it.
                System.out.println("Success: Added to com.project.sebastianrohe.twitter.database... " + tweet);
        }

        System.out.println("Process completed.");
    }

    /**
     * Get collection from mongodb.
     *
     * @return Collection.
     */
    public static MongoCollection<Document> getMyCollection() {
        return myCollection;
    }

    /**
     * Update (replace) tweet document in mongodb with nlp analysed document by using query for id.
     *
     * @param analysedTweetDocument Takes a nlp analysed tweet document.
     */
    public void updateWithNLPAnalysedDocument(Document analysedTweetDocument) {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", analysedTweetDocument.get("_id"));

        // Update (replace) document which was found with where query with the results of nlp com.project.sebastianrohe.twitter.analysis.
        MongoDBConnectionHandler.getMyCollection().replaceOne(whereQuery, analysedTweetDocument);
    }

}


