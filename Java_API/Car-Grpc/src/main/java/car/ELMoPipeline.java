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

public class ELMoPipeline {

    public static Map<String, String> queries = new HashMap<>();
    private static Path avgAndIndy;
    private static Path global;
    private static Path noContext;
    private static Path queryPath;
    private static List<String> stopWords = new ArrayList<>();
    public static String queryType = "combine";

    static {

    }

    public static void main(String[] args) throws Exception {
        String type = args[0];
        int numExpansionWords = Integer.valueOf(args[1]);
        String dataset = args[2];

        Path baselineEval;
        Path qrelPath;
        Path batchPath;
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

        if(dataset.equals("test")){
            System.out.println("Hi");
            baselineEval = Paths.get("Queries/ELMo/ELMo_subset/batch-0-0-testset.txt");
            qrelPath = Paths.get(basePath,"test.pages.cbor.tree.qrels");
            avgAndIndy = Paths.get(basePath, "data/Local_ELMo/embeddingsAverageWeightsUnlimitedTest.txt");
            noContext = Paths.get(basePath,"data/Local_ELMo/noContextWeightsTest.txt");
            global = Paths.get(basePath,"data/Global_ELMo/global_avg_similarity_test.txt");
            queryPath = Paths.get("Queries/ELMo/ELMo_subset/test-"+type+"-"+numExpansionWords+"-"+dataset+"set.json");
            batchPath = Paths.get("Queries/ELMo/ELMo_subset/batch-"+type+ "-"+numExpansionWords+ "-"+dataset+ "set.txt");
        } else{

            baselineEval = Paths.get("Queries/ELMo/ELMo_subset/batch-0-0-trainset.txt");
            qrelPath = Paths.get(basePath,"train.pages.cbor.tree.qrels");
            avgAndIndy = Paths.get(basePath,"data/Local_ELMo/embeddingsAverageWeightsUnlimited.txt");
            noContext = Paths.get(basePath,"data/Local_ELMo/noContextWeightsTrain.txt");
            global = Paths.get(basePath,"data/Global_ELMo/global_avg_similarity.txt");
            queryPath = Paths.get("Queries/ELMo/ELMo_subset/train-"+type+"-"+numExpansionWords+"-"+dataset+"set.json");
            batchPath = Paths.get("Queries/ELMo/ELMo_subset/batch-"+type+ "-"+numExpansionWords+ "-"+dataset+ "set.txt");
        }
        getELMoQueries();

        generateELMoQueries(numExpansionWords,type,dataset);

        PrintStream batch_stream = new PrintStream(batchPath.toFile());
        PrintStream eval_stream = new PrintStream(Paths.get("Queries/ELMo/ELMo_subset/eval-"+type+"-"+ numExpansionWords+ "-"+dataset+"set.txt").toFile());

        String indexPath = args[3];
        App.run(new String[]{"batch-search",
                "--index=" + indexPath,
                "--requested=" + "1000",
                "--threadCount=" + 8,
                "--mu=" + 1200,
                "--scorer=bm25",
                queryPath.toString()}, batch_stream
        );

        App.run(new String[]{"eval",
                        "--baseline=" + batchPath,
                        "--judgments=" + qrelPath,
//                        "--treatment=" + baselineEval,
                        "--details=true",
                        "--metrics+map", "--metrics+ndcg", "--metrics+r-prec",
//                        "--comparisons=ttest"
                },
                eval_stream);
    }
    public static void generateELMoQueries(int numExpansionWords, String type, String test){
        List<LinkedHashMap<String, List<Map<String,String>>>> ELMoMap = getELMoQueries();

        HashMap<String,List<Map<String,String>>> Map;
//        System.out.println(type);
        if(type.equals("Average")) Map = ELMoMap.get(0);
        else if(type.equals("Independent")) Map = ELMoMap.get(1);
        else if(type.equals("Global") || type.equals("NoContext")) Map = getDataset(type);
        else {
            System.out.println("Please enter an expansion type");
            return;
        }
//        System.out.println(test);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Map<String, String> queryArr = new HashMap();
        BufferedWriter writer = null;
        LinkedHashSet<String> queries = null;
        int[] kVals = {3,5,10,20,50,150,10000};

        if (Map != null) {
            queries = new LinkedHashSet<>(Map.keySet());
        }

        System.out.println(Map.keySet());


        List<String> queriesWithParagraphs = new ArrayList<>();

        try {
            writer = new BufferedWriter(new FileWriter(queryPath.toFile()));
        }catch (Exception e) {
            e.printStackTrace();
        }

        try{
            Map<String,List<String>> qrelMap;

            if(test.equals("test")) qrelMap = QrelMapCreation.qrelMatch(true);
            else qrelMap = QrelMapCreation.qrelMatch(false);

            if (queries != null) {
                for(String key:queries){

                    ObjectNode text = mapper.createObjectNode();

                    String QueryId = key.split("\\+")[0].trim();
                    String queryWords = key.split("\\+")[1];

                    List<Map<String,String>> expansionTerms = Map.get(key);
                    Map<String,List<Map.Entry>> outList = new HashMap<>();

                    if(type.equals("Global") || type.equals("NoContext")){
                        List<Map.Entry> noContextEntryList = new ArrayList<>();
                        for(Map<String,String> entry: expansionTerms){
                            noContextEntryList.add(entry.entrySet().iterator().next());
                        }
                        outList.put(QueryId,noContextEntryList);
                    } else {
                        for(Map<String,String> termMap : expansionTerms){
                            List<Map.Entry> t = termMap.entrySet().stream().sorted(java.util.Map.Entry.comparingByValue()).collect(Collectors.toList());
                            outList.put(QueryId,t);
                        }
                    }
                    List<Map.Entry> expansionTermsQuery;

                    expansionTermsQuery = outList.get(QueryId).subList(0,numExpansionWords);
                    List<String> queryTerms = new ArrayList<>();

                    for(Map.Entry<String,String> e : expansionTermsQuery){
                        queryTerms.add(e.getKey());
                    }

                    JsonNode queryText;
                    String qTerms =  queryWords.replaceAll("-"," ").replaceAll("[^A-Za-z0-9 ]","").replace("entireQuery","").toLowerCase();

                    String outputQTerms = "";
                    for(String qTerm : qTerms.split(" ")){
                        if (!outputQTerms.contains(qTerm)) {
                            if (!stopWords.contains(qTerm)) {
                                outputQTerms += qTerm + " ";
                            }
                        }
                    }

//                    System.out.println(outputQTerms);

                    queryText = numExpansionWords == 0 ? mapper.readTree("\"#combine(" + outputQTerms.trim() + ")\"") : mapper.readTree("\"#combine:0=0.80:1=0.20(#combine(" + outputQTerms.trim() + ") #combine(" + String.join(" ",queryTerms) + "))\"");

                    JsonNode numberText = mapper.readTree("\"" + QueryId + "\"");

                    List<String> paragraphs = qrelMap != null ? qrelMap.get(QueryId) : null;

                    if(paragraphs != null) {

                        queriesWithParagraphs.add(QueryId);
                        String combinedParagraphs = String.join(" ", paragraphs);
                        Set<String> paragraphTerms = new HashSet<>(Arrays.asList(combinedParagraphs.split(" ")));
                        paragraphTerms.removeAll(stopWords);

                        LinkedHashSet<String> expansionTermsK;

                        List<String> terms = new ArrayList<>();

                        for(Map.Entry<String,String> e : outList.get(QueryId)){
                            terms.add(e.getKey());
                        }

                        expansionTermsK = new LinkedHashSet<>(terms);

//                        MetricsAndHelpers.createMetricMaps(kVals,expansionTermsK,paragraphTerms);

                        text.put("number", numberText);
                        text.put("text", queryText);


//                        System.out.println(text);

                        if (queryArr.get("queries") == null) {
                            queryArr.put("queries", text.toString());
                        } else {
                            queryArr.put("queries", queryArr.get("queries") + "," + text.toString());
                        }

                    }
                }
            }

//            MetricsAndHelpers.printMeasures(queriesWithParagraphs,kVals,MetricsAndHelpers.kToPrecision,MetricsAndHelpers.kToRecall,MetricsAndHelpers.kToF1);

            String nodes = queryArr.get("queries");

            if (nodes != null) {
                writer.write("{\"queries\":[" + nodes + "]}");
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(writer != null) writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<LinkedHashMap<String,List<Map<String,String>>>> getELMoQueries(){
//        avgAndIndy = Paths.get("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/Local_ELMo/embeddingsAverageWeightsUnlimitedTest.txt");
        BufferedReader reader;
        String line;

        LinkedHashMap<String,List<Map<String,String>>> averageELMoMap = new LinkedHashMap<>();
        LinkedHashMap<String,List<Map<String,String>>> indyELMoMap = new LinkedHashMap<>();

        List<LinkedHashMap<String,List<Map<String,String>>>> outList = new ArrayList();

        try {
            reader = new BufferedReader(new FileReader(avgAndIndy.toFile()));
        } catch(FileNotFoundException e){
            System.out.println("FILE NOT FOUND");
            return null;
        }

        try {
            while ((line = reader.readLine()) != null) {

                String queryId = line.split("@")[0];
                String expansionWords = line.split("@")[1];
                String [] splitLine = expansionWords.replaceAll("[\\[\\]']","").split(",");

                String query = "";

                List<Map<String,String>> entireExpansionWordSet = new ArrayList<>();
                List<Map<String,String>> indySet = new ArrayList<>();

                for (String s : splitLine) {
                    query += s.split(":")[0];

                    Map<String,String> entireWordWeight = new HashMap<>();
                    if(s.split(":")[0].trim().equals("entireQuery")){

                        for(String w: s.split(":")[1].split(" ")){
                            String[] entireWordWeightList = w.split("\\+");
                            if(!queryId.toLowerCase().contains(entireWordWeightList[0].toLowerCase())){
                                entireWordWeight.put(entireWordWeightList[0],entireWordWeightList[1]);
                                entireExpansionWordSet.add(entireWordWeight);
                            }
                        }
                    }else{

                        Map<String,String> indy = new HashMap<>();
                        for(String w: s.split(":")[1].split(" ")) {

                            String[] indyWeightList = w.split("\\+");
                            if (!queryId.toLowerCase().contains(indyWeightList[0].toLowerCase())) {
                                indy.put(indyWeightList[0], indyWeightList[1]);
                                indySet.add(indy);
                            }
                        }
                    }
                }

                averageELMoMap.put(queryId + "+" + query,entireExpansionWordSet);
                indyELMoMap.put(queryId + "+" + query, indySet);
            }

            outList.add(averageELMoMap);
            outList.add(indyELMoMap);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return outList;
    }

    private static LinkedHashMap<String,List<Map<String,String>>> getDataset(String dataset){

        BufferedReader reader;
        String line;
        LinkedHashMap<String,List<Map<String,String>>> ELMoMap = new LinkedHashMap<>();

        try {
            if(dataset.equals("Global")){
                reader = new BufferedReader(new FileReader(global.toFile()));
            }else{
                reader = new BufferedReader(new FileReader(noContext.toFile()));
            }
        } catch(FileNotFoundException e){
            return null;
        }

        try {
            while ((line = reader.readLine()) != null) {
                String queryId = line.split("@")[0];

                String expansionWords = line.split("@")[1];
                String [] splitLine = expansionWords.replaceAll("[\\[\\]']","").split(",");
                List<Map<String,String>> expansionWordSet = new ArrayList<>();
                String queryText = queryId.replace("enwiki:","").replace("%20"," ").replace("/"," ");
                for (String s : splitLine) {
                    Map<String,String> wordWeight = new HashMap<>();
                    String[] entireWordWeightList = s.split("\\+");

                    if(!queryText.toLowerCase().contains(entireWordWeightList[0].trim().toLowerCase())){
                        wordWeight.put(entireWordWeightList[0].trim(),entireWordWeightList[1]);
                        expansionWordSet.add(wordWeight);
                    }
                }
                ELMoMap.put(queryId + "+" + queryText,expansionWordSet);
            }
            return ELMoMap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}