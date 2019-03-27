package com.carapi.queryEngine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.tools.App;


import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class QueryEngineServer {
    private static final Logger logger = java.util.logging.Logger.getLogger("server-logger");

    private Server server;
    private Map<String, List<String>> qrelIdToText = new HashMap<>();
    private Retrieval index;
    private String IPath;
    private String qrelPath;

    private void start(String indexPath, String qPath) throws Exception {
        int port = 50051;
        IPath = indexPath;

        server = ServerBuilder.forPort(port)
                .addService(com.carapi.queryEngine.QueryEngineGrpc.bindService(new QueryEngineImpl()))
                .build()
                .start();

        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                QueryEngineServer.this.stop();
                System.err.println("*** server shut down");
            }
        });

        QueryEngineImpl impl = new QueryEngineImpl();
        index = RetrievalFactory.instance(IPath);
        qrelPath = qPath;
        qrelIdToText = impl.qrelMatch();

    }

    private void stop() {
        if (server != null) {
            try {
                index.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        final QueryEngineServer server = new QueryEngineServer();
        server.start(args[0],args[1]);
        server.blockUntilShutdown();
    }

    private class QueryEngineImpl implements com.carapi.queryEngine.QueryEngineGrpc.QueryEngine {

        /* Handle logic of receiving a request and returning a response
           Processes the input queries, expanded and normal,
           Runs a batch search for each and returns the response containing both queries
         */
        @Override
        public void sendQuery(QueryRequest req, StreamObserver<QueryResponse> responseObserver) {

            logger.info("Input Query: " + req.getOriginalQuery() + "\n");

            String expandedWord = "";
            if(req.getOriginalQuery().split(" ").length > 4) {
                expandedWord = req.getOriginalQuery().split(" ")[req.getOriginalQuery().split(" ").length - 2];
            }
            logger.info(expandedWord);

            processQuery(req.getOriginalQuery(), req.getRawQueryText(), expandedWord);

            String[] outQuery = batchSearch(req.getNumberOfDocs(), req.getRawQueryText(), "", IPath, expandedWord).split("\n");

            QueryResponse.Builder response = QueryResponse.newBuilder();

            List<result> resultsQuery = createResults(outQuery);

            List<String> qrelParagraphs = new ArrayList<>();


            if (qrelIdToText.containsKey(req.getRawQueryText()) && qrelIdToText.get(req.getRawQueryText()) != null)
                qrelParagraphs = qrelIdToText.get(req.getRawQueryText());


            logger.info("Found " + qrelParagraphs.size() + " qrel paragraphs");

            String evalQuery = evalQuery("", expandedWord, req.getRawQueryText().replaceAll("[/:]", "-") );

            String map, rP, ndcg;
            map = rP = ndcg = "";

            if (!evalQuery.equals("")) {
                logger.info(evalQuery);
                map = evalQuery.split(" +")[5];
                rP = evalQuery.split(" +")[6];
                ndcg = evalQuery.split(" +")[7];
            }

            if (!resultsQuery.equals(new ArrayList<result>())) response.addAllResults(resultsQuery);

            response.addAllQrelParagraph(qrelParagraphs);

            if(!map.equals("") && !rP.equals("") && !ndcg.equals("")){

                logger.info("Original query eval results: MAP: " + map + " R-Precision: " + rP + " NDCG: " + ndcg);

                response.setQueryEval(eval.newBuilder().setMap(Float.parseFloat(map))
                        .setNdcg(Float.parseFloat(ndcg))
                        .setRprec(Float.parseFloat(rP)).build()).build();

            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        /* Function which gets the text from the index given an Id,
           Returns the document text or null
        */
        String idToText(String id) {
            Document.DocumentComponents dc = new Document.DocumentComponents(false, false, true);
            Document doc = null;

            try {
                doc = index.getDocument(id, dc);
            } catch (IOException e) {
                logger.info(e.getMessage());
            }

            if (doc != null) {
                return doc.text;
            }

            return null;
        }

        // Function which creates a Map of query texts' to paragraphs
        Map<String, List<String>> qrelMatch() {
            String qrelData = qrelPath;
            BufferedReader reader = null;
            String line;

            try {
                reader = new BufferedReader(new FileReader(new File(qrelData)));
            } catch (FileNotFoundException e) {
                logger.info(e.getMessage());
                logger.info("Qrel map creation failed");
                return null;
            }

            try {
                while ((line = reader.readLine()) != null) {
                    String[] splitLine = line.split(" ");
                    if (idToText(splitLine[2]) != null) {
                        if (!qrelIdToText.containsKey(splitLine[0])) {
                            qrelIdToText.put(splitLine[0], new ArrayList<>(Arrays.asList(idToText(splitLine[2]))));
                        } else {
                            qrelIdToText.get(splitLine[0]).add(idToText(splitLine[2]));
                        }
                    }
                }

            } catch (IOException e) {
                logger.info("Qrel map creation failed");
                return null;
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return qrelIdToText;
        }


        /* Function to create query file based on incoming query file.
           Takes a query, the expanded text, and whether or not it is Expanded
           Writes the query in the correct format to a temporary file
         */
        private void processQuery(String query, String numberText, String expandedWord) {
            logger.info(expandedWord);
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            ObjectNode text = mapper.createObjectNode();

            JsonNode numText = null;
            JsonNode queryText = null;

            try {
                query = query.replaceAll(":", "\\:");
                queryText = mapper.readTree("\"" + query + "\"");

                numText = mapper.readTree("\"" + numberText + "\"");
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.err.println("Json Node Creation Failed");
            }

            text.put("number", numText);
            text.put("text", queryText);

            BufferedWriter writer = null;
            final String QueryPath = "/tmp/";
            try {
                writer =  new BufferedWriter(new FileWriter(new File(QueryPath + expandedWord + "-query.json")));
                writer.write("{\"queries\":[" + text + "]}");
            } catch (IOException e1) {
                System.err.println("IO Exception occured");
                e1.printStackTrace();
            } finally{
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        private List<result> createResults(String[] outArr) {
            List<result> results = new ArrayList();

            if (outArr.length > 5) {
                for (String id : Arrays.copyOfRange(outArr, 0, 5)) {
                    String paragraph;
                    if ((paragraph = idToText(id.split(" ")[2])) != null) {
                        result newRes = result.newBuilder().setParagraph(paragraph)
                                .setSimilarity(id.split(" ")[4]).build();
                        results.add(newRes);
                    }
                }
            } else{
                for (String id : outArr) {
                    String paragraph;
                    if(id.split(" ").length > 1){
                        if ((paragraph = idToText(id.split(" ")[2])) != null) {
                            result newRes = result.newBuilder().setParagraph(paragraph)
                                    .setSimilarity(id.split(" ")[4]).build();
                            results.add(newRes);
                        }
                    }
                }
            }
            return results;
        }

        /* Function to run batch search given a number of paragraphs per query,
           The query text to find the correct query file and the expanded flag,
           Writes the output of the batch search to a file for eval.
         */
        public String batchSearch(Integer paragraphNum, String numberText, String type,String indexPath, String expandedWord) {
            String queryPath = queryPath = "/tmp/" + expandedWord + "-query.json";


            ByteArrayOutputStream os = new ByteArrayOutputStream(); ;
            FileWriter writer = null;
            PrintStream stream = new PrintStream(os);
            try{
                writer = new FileWriter(new File("/tmp/batchout" + type + "-" + numberText.replaceAll("[/:]", "-") + expandedWord + ".txt"));
                App.run(new String[] {"threaded-batch-search",
                        "--index=" + indexPath,
                        "--requested=" + paragraphNum,
                        "--threadCount=" + 8,
                        "--mu=" + 1200,
                        "--scorer=bm25",
                        queryPath},stream
                );
                writer.write(os.toString());

            } catch(Exception e){
                e.printStackTrace();
            } finally{
                try {
                    if (writer != null) writer.close();
                    os.close();
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return os.toString();
        }

        /* Function to evaluate a batch search for a given query
           Takes the expanded flag to find the correct batch out path
           Writes the evaluation to an output stream which is returned.
         */
        public String evalQuery(String type, String expandedWord, String numberText) {
            String qrelData = qrelPath;
            String batchPath;

            batchPath = "/tmp/batchout" + type + "-" + numberText.replaceAll("[/:]", "-") + expandedWord + ".txt";

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(os);
            try{
                App.run(new String[]{"eval",
                                "--runs+" + batchPath,
                                "--judgments=" + qrelData,
                                "--metrics+map",
                                "--metrics+ndcg",
                                "--metrics+r-prec"
                        },
                        stream);
            } catch(Exception e){
                System.err.println("Query Eval Failed");
            } finally {
                try {
                    os.close();
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return os.toString();
        }
    }
}