package car;

import car.qrel.QrelMapCreation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.lemurproject.galago.core.tools.App;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GlovePipeline {

    public static Map<String, String> queries = new HashMap<>();
    private static Path randomSubset;

    private static Path gloveIndependentPath;
    private static List<String> stopWords = new ArrayList<>();
    public static String queryType = "combine";
    private static Path queryFile;
    private static Path queryPath;
    private static String indexPath;

    public static void main(String[] args) throws Exception {


        Path qrelPath;
        String basePath = "Java_API/Car-Grpc/src/main/resources/";

        Path STOP_WORDS = Paths.get(basePath,"data/General/StopWords");
        BufferedReader stop_word_reader;
        try {
            stop_word_reader = new BufferedReader(new FileReader(STOP_WORDS.toFile()));

            String line;
            while ((line = stop_word_reader.readLine()) != null) {
                stopWords.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(args[0].equals("test")){
            gloveIndependentPath = Paths.get(basePath,"data/topIN/independent-Glove-test.txt");
            qrelPath = Paths.get(basePath,"test.pages.cbor.tree.qrels");
            randomSubset = Paths.get(basePath,"data/General/test-random-subset.txt");
            queryFile = Paths.get("Queries/Glove/test-expansion.json");
        } else{
            gloveIndependentPath = Paths.get(basePath,"data/topIN/independent-Glove-train.txt");
            qrelPath = Paths.get(basePath,"train.pages.cbor.tree.qrels");
            randomSubset = Paths.get(basePath,"data/General/train-random-subset.txt");
            queryFile = Paths.get("Queries/Glove/train-expansion.json");
        }

        queryPath = Paths.get("Queries/Glove/", args[0]+"-expansion.json");
        Path baselinePath = Paths.get("Queries/Glove/batch-glove.txt");

        generateGloveMetrics(Integer.parseInt(args[1]), args[0]);

        PrintStream batch_stream = new PrintStream(Paths.get("Queries/Glove/batch-glove.txt").toFile());
        PrintStream eval_stream = new PrintStream(Paths.get("Queries/Glove/eval-glove.txt").toFile());

        indexPath = args[2];
        App.run(new String[] {"threaded-batch-search",
                "--index=" + indexPath,
                "--requested=" + "1000",
                "--threadCount=" + 8,
                "--scorer=bm25",
                "--mu=" + 1200,
                queryPath.toString()},batch_stream
        );

        App.run(new String[]{"eval",
                        "--baseline=" + baselinePath,
                        "--judgments=" + qrelPath,
                        "--details=true",
                        "--metrics+" + "map", "--metrics+" + "ndcg", "--metrics+" + "r-prec"
                },
                eval_stream);
    }

    private static void generateGloveMetrics(int numQueryExpansionTerms, String test){
        BufferedReader queryReader = null, reader = null;
        Map<String, List<String>> qrelMap;
        if(test.equals("test")) qrelMap = QrelMapCreation.qrelMatch(true);
        else qrelMap = QrelMapCreation.qrelMatch(false);

        Map<String,Map<String,String>> wordToScore = new HashMap<>();
        Map<String, String> queryArr = new HashMap();
        List<String> querySubsetList = new ArrayList<>();

        try{
            reader = new BufferedReader(new FileReader(gloveIndependentPath.toFile()));
            queryReader = new BufferedReader(new FileReader(randomSubset.toFile()));
            String queryLine;
            while((queryLine = queryReader.readLine()) != null){
                querySubsetList.add(queryLine);
            }

            String line = reader.readLine();
            String[] words = line.split(";");
            for(String word : words){
                String queryWord = word.split(":")[0].toLowerCase();
                Map<String,String> expansionWordScore = new HashMap<>();

                for(String pair: word.split(":")[1].split(" ")) {
                    String eWord = "", score = "";

                    if (pair.split("\\+").length == 2) {
                        eWord = pair.split("\\+")[0];
                        score = pair.split("\\+")[1];
                    }

                    if(!stopWords.contains(eWord)) expansionWordScore.put(eWord, score);
                }

                wordToScore.put(queryWord, expansionWordScore);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (reader != null) reader.close();
                if (queryReader != null) queryReader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int[] kVals = {3,5,10,20,50,1000};

        for(String key: querySubsetList){

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            ObjectNode text = mapper.createObjectNode();

            String[] keyWords = key.replace("enwiki:","").replace("%20"," ").replaceAll("[/]"," ").replaceAll("-"," ").split(" ");

            LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList(keyWords));

            List<Map.Entry<String,String>> gloveTermList = new ArrayList<>();

            for(String w : set){
                if(!stopWords.contains(w.toLowerCase()))
                    if (wordToScore.containsKey(w.toLowerCase()))
                        gloveTermList.addAll(wordToScore.get(w.toLowerCase()).entrySet());
            }

            gloveTermList.sort((m1, m2) -> Float.compare(Float.parseFloat(m1.getValue()), Float.parseFloat(m2.getValue())));

            LinkedHashSet<String> gloveTermWords = new LinkedHashSet<>();
            for (Map.Entry<String,String> e: gloveTermList) gloveTermWords.add(e.getKey());

            LinkedHashSet<String> gloveSet = new LinkedHashSet<>(gloveTermWords);

            String combinedParagraphs = "";
            if (qrelMap != null && qrelMap.containsKey(key)) combinedParagraphs = String.join(" ", qrelMap.get(key));

            Set<String> paragraphTerms = new HashSet<>(Arrays.asList(combinedParagraphs.split(" ")));
            paragraphTerms.removeAll(stopWords);

            JsonNode numberText = null;
            JsonNode queryText = null;

            try {
                if(gloveTermWords.size() > 5){
                    queryText = mapper.readTree("\"#combine:0=0.8:1=0.2(#combine(" + String.join(" ",set).replaceAll("[^A-Za-z0-9 ]","").toLowerCase() + ") #combine(" + gloveTermWords.stream().limit(numQueryExpansionTerms).collect(Collectors.joining(" ")) + "))\"");
                } else{
                    queryText = mapper.readTree("\"#combine:0=0.8:1=0.2(#combine(" + String.join(" ",set).replaceAll("[^A-Za-z0-9 ]","").toLowerCase() + ") #combine(" + String.join(" ",gloveTermWords) + "))\"");
                }
                numberText = mapper.readTree("\"" + key + "\"");

            } catch (IOException e) {
                e.printStackTrace();
            }

            text.put("number", numberText);
            text.put("text", queryText);

            if (queryArr.get("queries") == null) queryArr.put("queries", text.toString());
            else queryArr.put("queries", queryArr.get("queries") + "," + text.toString());

            MetricsAndHelpers.createMetricMaps(kVals, gloveSet, paragraphTerms);
        }

        BufferedWriter queryArrWriter = null;
        String nodes = queryArr.get("queries");

        try {
            queryArrWriter = new BufferedWriter(new FileWriter(queryFile.toFile()));

            if (nodes != null) queryArrWriter.write("{\"queries\":[" + nodes + "]}");
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (queryArrWriter != null) queryArrWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MetricsAndHelpers.printMeasures(querySubsetList,kVals, MetricsAndHelpers.kToPrecision, MetricsAndHelpers.kToRecall, MetricsAndHelpers.kToF1);
    }
}
