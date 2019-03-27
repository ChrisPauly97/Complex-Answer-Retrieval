package com.carapi.queryEngine;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueryEngineClient {
    private static final Logger logger = Logger.getLogger(QueryEngineClient.class.getName());

    private final ManagedChannel channel;
    private final QueryEngineGrpc.QueryEngineBlockingStub blockingStub;

    private QueryEngineClient(String host, int port) {
      channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build();
        blockingStub = QueryEngineGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void query(String query, Integer numberOfDocs) {
      try {
        logger.info("Attempting query " + query + " ...");

        QueryRequest request = QueryRequest.newBuilder()
                                  .setOriginalQuery(query)
                                  .setNumberOfDocs(numberOfDocs)
                                  .build();
        QueryResponse response = blockingStub.sendQuery(request);

        logger.info("Query:\t" + query);

        for(result para: response.getResultsList().subList(0,5)){
            logger.info("\nRetrieved paragraphs:\t" + para.getParagraph() + "\n");
            logger.info("\nRetrieved Similarity:\t" + para.getSimilarity() + "\n");
        }

        if(response.getQrelParagraphCount() > 0){
            for(String qrelPara : response.getQrelParagraphList().subList(1,response.getQrelParagraphCount())){
                logger.info("\nQrel Parargraphs:\t" + qrelPara + "\n");
            }
        }

        logger.info("Map:\t" + response.getQueryEval().getMap());
        logger.info("NDCG:\t" + response.getQueryEval().getNdcg());
        logger.info("R-Precision:\t" + response.getQueryEval().getRprec());

      } catch (RuntimeException e){
        logger.log(Level.WARNING, "RPC failed", e);
        return;
      }
    }

    public static void main(String[] args) throws Exception {
      QueryEngineClient client = new QueryEngineClient("localhost",50051);
      try {
        String query = "enwiki:Agriprocessors/Controversies";
        String expanded = "#combine(agriprocessors controversies) " +
                "#combine(cargill sillamäe cashways postville cavel rubashkin " +
                "slaughterhouse ifco wampler meatpacking) #combine(disagreements " +
                "embroiled scandals embarrassments criticisms furor arisen controversy disputes arose)\n";

        Integer numberOfDocs = 1000;
        if(args.length == 1) {
          query = args[0];
        }else if(args.length == 2){
          numberOfDocs = Integer.parseInt(args[1]);
        }
        client.query(query,numberOfDocs);
      }finally{
        client.shutdown();
      }
    }
  }
