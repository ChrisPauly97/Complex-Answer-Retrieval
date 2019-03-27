package car.GalagoFunctions;


import car.MetricsAndHelpers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.lemurproject.galago.core.tools.App;

import java.io.*;
import java.util.*;

public class OutlinePipeline {
    private static List<String> stopWords = new ArrayList<>();
    private static final String trainOutlinePath = "src/main/resources/data/benchmarkY1/train.pages.cbor-outlines.cbor";
    private static final String testOutlinePath = "src/main/resources/data/benchmarkY1/test.pages.cbor-outlines.cbor";
    public static String queryType = "combine";

    static {
        String STOP_WORDS = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/General/StopWords";
        BufferedReader stop_word_reader;
        try {
            stop_word_reader = new BufferedReader(new FileReader(new File(STOP_WORDS)));

            String line;
            while ((line = stop_word_reader.readLine()) != null) {
                stopWords.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {

        try {

            for (int x = 0; x < 1; x++) {
                queries(testOutlinePath, queryType, x);
                // Which headers to include in the query and which query type to use.
                String headerType = "rih";
                runBatch(headerType, queryType, "1000", x);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String,String> queries(String path, String qType, Integer expansionNumber) throws IOException {

        String STOP_WORDS = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/General/Stopwords";
        FileInputStream stream;
        BufferedReader glove_expansion_reader = null;

        List<String> stopWords = new ArrayList<>();
        List<String> headers;
        List<List<Data.Section>> outline;
        List<String> queryIds = new ArrayList<>();

        Iterator dataIterator;

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Map<String, String> queryArr = new HashMap();

        stream = new FileInputStream(new File(path));
        Map<Integer, Map<String,String>> topNMapToMap = new HashMap();
        BufferedReader stop_word_reader = null;

        try {
            stop_word_reader = new BufferedReader(new FileReader(new File(STOP_WORDS)));

            String line;
            while ((line = stop_word_reader.readLine()) != null) {
                stopWords.add(line);
            }
//            for(int x = 1; x < 10; x++){
//                Map<String,String> topNMap = new HashMap();
//                String topIN = "src/main/resources/data/topIN/topIN" + x + ".txt";
//                glove_expansion_reader = new BufferedReader(new FileReader(new File(topIN)));
//                String topNLines;
//
//                while((topNLines=glove_expansion_reader.readLine())!= null){
//
//                    String[] topNLinesKeyVal = topNLines.split(";");
//
//                    for(int y = 0; y < topNLinesKeyVal.length-1; y++){
//                        topNMap.put(topNLinesKeyVal[y].split(":")[0].replaceAll("[\' ,]",""),topNLinesKeyVal[y].split(":")[1]);
//                    }
//
//                }
//                topNMapToMap.put(x,topNMap);
//            }
//            glove_expansion_reader.close();
            stop_word_reader.close();
        } catch (IOException io) {
            System.out.println(io.getMessage());
            if(stop_word_reader != null) {
                stop_word_reader.close();
            }
        }

        dataIterator = DeserializeData.iterAnnotations(stream);
        while (dataIterator.hasNext()) {

            // Get page data and flatten
            Data.Page page = (Data.Page) dataIterator.next();
            outline = page.flatSectionPaths();

            Data.Section sec = new Data.Section(page.getPageName(),page.getPageId(),page.getSkeleton());
            List<Data.Section> titleSection = Arrays.asList(sec);
            outline.add(0,titleSection);

            List<String> queryTypes = Arrays.asList("rih");//, "rh", "ri");

            // For each query type, rih, rh, etc... create the query text
            for (String queryType : queryTypes) {

                // Clean each section heading
                for (List<Data.Section> s : outline) {
                    ObjectNode text = mapper.createObjectNode();

                    headers = Data.sectionPathHeadings(s);
                    ArrayList filteredHeaders = new ArrayList();
                    StringBuilder headerId = new StringBuilder();

                    if (!String.join(" ", headers).equals(page.getPageName())) {
                        headers.add(0, page.getPageName());
                    }

                    // For each header, format it so that it can be evaluated
                    // Match format of qrel file, remove stopwords.
                    for (String h : headers) {
                        headerId.append("/").append(h.replaceAll(" ", "%20"));
                        queryIds.add(headerId.toString());
                        String[] filtered = Arrays.stream(h.replaceAll("[-,]", " ").split(" ")).filter(word -> !stopWords.contains(word.toLowerCase())).toArray(String[]::new);
                        h = String.join(" ", filtered);

                        filteredHeaders.add(h);
                    }

                    // Based on the query type, build the correct query
                    String qText = "";
                    if (queryType.equals("rih")) {
                        qText = String.join(" ", filteredHeaders);
                    }

                    // Expand every word in the query using glove
//
//                    String expansionCombine = "";
//                    if (expansionNumber != 0) {
//                        expansionCombine = "#" + qType + "(";
//                        for (String word : qText.split(" ")) {
//                            // :uniw=0.82:odw=0.10:uww=0.02:mu=1200
//                            if (word != null) {
//                                if (topNMapToMap.get(expansionNumber).get(word.toLowerCase()) != null && !topNMapToMap.get(expansionNumber).get(word.toLowerCase()).equals("[]")) {
////                                qText.append(") #").append(qType).append(" (").append(String.join(" ", topNMapToMap.get(expansionNumber).get(word.toLowerCase()).replaceAll("[\\[\\],]", "")));
//                                    expansionCombine += "#" + qType + "(" + String.join(" ", topNMapToMap.get(expansionNumber).get(word.toLowerCase()).replaceAll("[\\[\\],]", "") + ")");
//                                }
//                            }
////                            System.out.println(expansionCombine);
//                        }
//                        expansionCombine += ")";
//                    }

//                    if (queryType.equals("rih")) {
//                        MetricsAndHelpers.getRMTerms(headerId.replace(0, 1, "enwiki:").toString(), "#" + qType + " (" + qText.replaceAll("[']", "").toLowerCase() + ")");
//                    }

                    // Text included in the actual query - #rm (chocolate  research)
                    JsonNode queryText = mapper.readTree("\"#" + qType + " (" + qText.replaceAll("[']", "").toLowerCase() + ")\"");// + expansionCombine + "\"");
                    JsonNode numberText;

                    numberText = mapper.readTree("\"" + headerId.replace(0, 1, "enwiki:") + "\"");

                    // Store the query text and number text in a single node per query
                    text.put("number", numberText);
                    text.put("text", queryText);

                    // Create a list of all query nodes to store in a file
                    if (queryArr.get(queryType) == null) {
                        queryArr.put(queryType, text.toString());
                    } else {
                        queryArr.put(queryType, queryArr.get(queryType) + "," + text.toString());
                    }
                }
            }
        }

        stream.close();

        // For each query type, write out to a file for each query type
        BufferedWriter writer;

        for(String key : queryArr.keySet()) {
            writer = new BufferedWriter(new FileWriter(new File("../../Queries/Glove/rih" + "-" + expansionNumber + "-" + queryType + "-query.json")));

            String nodes = queryArr.get(key);

            if (nodes != null) {
                writer.write("{\"queries\":[" + nodes + "]}");
            }

            writer.close();
        }

        return queryArr;
    }

    public static void runBatch(String headerType, String queryType, String paragraphNum, Integer queryNum) {
        try{

            PrintStream stream = new PrintStream(new File("../../Queries/batchout-" + queryType +"-" + queryNum + ".txt"));

            String queryPath = "../../Queries/Glove/"+ headerType + "-" + queryNum + "-" + queryType + "-query.json";

            String indexPath = "/run/media/leftclick/Games Volume/indices/paragraphcorpus2";
            App.run(new String[] {"threaded-batch-search",
                    "--index=" + indexPath,
                    "--requested=" + paragraphNum,
                    "--threadCount=" + 8,
                    "--scorer=bm25",
                    "--mu=" + 1200,
                    queryPath},stream
            );

//            BufferedReader br = new BufferedReader(new FileReader(new File("src/main/resources/BatchSearchOutput/batchout" + queryType + "-" + queryNum + ".txt")));
//            String line;
//            StringBuilder paras = new StringBuilder();
//            while((line = br.readLine()) != null){
//                String paraId = line.split(" ")[2];
//                String score = line.split(" ")[4];
//
//                paras.append(getDoc.idToText(paraId)).append(";").append(score).append(":");
//            }

            stream.close();
//            br.close();

        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
            System.out.println("Usage: runBatchQuery rih combine");
        }
    }
}
