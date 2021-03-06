package com.project.sebastianrohe.twitter.analysis;

import com.project.sebastianrohe.twitter.data.impl.TweetMongoDBImpl;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.bson.Document;
import org.hucompute.textimager.uima.type.Sentiment;
import org.json.JSONObject;
import org.texttechnologylab.uimadb.wrapper.mongo.MongoSerialization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NLPAnalysis {

    /**
     * This method handles the NLP analysis of a given tweet document from the database.
     *
     * @param tweetDocument  Tweet document which should be analysed (from database).
     * @param analysisEngine Analysis engine with pipeline to run NLP.
     * @return Analysed tweet document.
     * @throws UIMAException If something goes wrong.
     */
    public Document runNLP(Document tweetDocument, AnalysisEngine analysisEngine) throws UIMAException {
        // Create instance of TweetMongoDBImpl class to represent tweet as document from the database.
        TweetMongoDBImpl tweetFromDatabase = new TweetMongoDBImpl(tweetDocument);

        // Create JCas from tweet document from twitter.database.
        JCas jCas = tweetFromDatabase.toJCas();

        // Run analysis engine to analyse JCas.
        SimplePipeline.runPipeline(jCas, analysisEngine);

        // Part to analyse for tokens.
        Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
        List<String> tokenList = new ArrayList<>(0);
        for (Token token : tokens) {
            tokenList.add(token.getCoveredText());
        }

        // Part to analyse for sentences.
        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
        List<String> sentenceList = new ArrayList<>(0);
        for (Sentence sentence : sentences) {
            sentenceList.add(sentence.getCoveredText());
        }

        // Part to analyse for sentiments.
        Collection<Sentiment> sentiments = JCasUtil.select(jCas, Sentiment.class);
        List<Double> sentimentList = new ArrayList<>(0);
        for (Sentiment sentiment : sentiments) {
            sentimentList.add(sentiment.getSentiment());
        }

        // Part to analyse for POS.
        Collection<POS> posCollection = JCasUtil.select(jCas, POS.class);
        List<String> posList = new ArrayList<>(0);
        for (POS pos : posCollection) {
            posList.add(pos.getPosValue());
        }

        // Part to analyse for named entities.
        Collection<NamedEntity> namedEntityCollection = JCasUtil.select(jCas, NamedEntity.class);
        List<String> orgEntityList = new ArrayList<>(0);
        List<String> personEntityList = new ArrayList<>(0);
        List<String> locationEntityList = new ArrayList<>(0);

        for (NamedEntity namedEntity : namedEntityCollection) {
            switch (namedEntity.getValue()) {
                case "ORG":
                    orgEntityList.add(namedEntity.getCoveredText());
                    break;

                case "PER":
                    personEntityList.add(namedEntity.getCoveredText());
                    break;

                case "LOC":
                    locationEntityList.add(namedEntity.getCoveredText());
                    break;
            }
        }

        // Update the tweet document from the database with results of NLP analysis.
        Document analysedTweetDocument = tweetFromDatabase.getTweetDocument();

        analysedTweetDocument.put("sentiments", sentimentList);
        analysedTweetDocument.put("tokens", tokenList);
        analysedTweetDocument.put("sentences", sentenceList);
        analysedTweetDocument.put("pos", posList);
        analysedTweetDocument.put("organizations", orgEntityList);
        analysedTweetDocument.put("persons", personEntityList);
        analysedTweetDocument.put("locations", locationEntityList);

        // Save serialized JCas in every document for database.
        analysedTweetDocument.put("uima", new JSONObject(MongoSerialization.serializeJCas(jCas)).toString());

        // Return analysed document.
        return analysedTweetDocument;
    }

}
