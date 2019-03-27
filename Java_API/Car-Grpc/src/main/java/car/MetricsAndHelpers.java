package car;

import car.qrel.QrelMapCreation;
import edu.stanford.nlp.util.Sets;
import org.lemurproject.galago.core.tools.apps.GetRMTermsFn;
import org.lemurproject.galago.utility.Parameters;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public class MetricsAndHelpers {

    public static Map<String, String> queries = new HashMap<>();
    private static List<String> stopWords = new ArrayList<>();
    public static String queryType = "combine";

    static LinkedHashMap<Integer,List<Float>> kToPrecision = new LinkedHashMap<>();
    static LinkedHashMap<Integer,List<Float>> kToRecall = new LinkedHashMap<>();
    static LinkedHashMap<Integer,List<Float>> kToF1 = new LinkedHashMap<>();

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

    private static BufferedWriter csvWriter;

    static {
        try {
            csvWriter = new BufferedWriter(new FileWriter(new File("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/metrics.csv")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void createMetricMaps(int[] kVals, LinkedHashSet<String> expansionTermsK, Set<String> paragraphTerms){

        float FN, FP,TP;

        for(int x : kVals) {

            expansionTermsK = expansionTermsK.stream().limit(x).collect(Collectors.toCollection(LinkedHashSet::new));
            TP = Sets.intersection(paragraphTerms, expansionTermsK).size();
            FN = paragraphTerms.size() - TP;
            FP = expansionTermsK.size() - TP;

            float Recall = (TP != 0 && FN != 0) ? (TP / (TP + FN)) : 0;
            float Precision = TP != 0 && FP != 0 ? (TP / (TP + FP)) : 0;
            float F1 = (Recall == 0 || Precision == 0) ? 0 : (2 * (Recall * Precision)) / (Recall + Precision);

            if (!kToPrecision.containsKey(x)) kToPrecision.put(x, new ArrayList<>(Arrays.asList(Precision)));
            else kToPrecision.get(x).add(Precision);

            if (!kToRecall.containsKey(x)) kToRecall.put(x, new ArrayList<>(Arrays.asList(Recall)));
            else kToRecall.get(x).add(Recall);

            if (!kToF1.containsKey(x)) kToF1.put(x, new ArrayList<>(Arrays.asList(F1)));
            else kToF1.get(x).add(F1);
        }
    }

    static void printMeasures(List<String> queriesWithParagraphs, int[] kVals, LinkedHashMap<Integer, List<Float>> kToPrecision, LinkedHashMap<Integer, List<Float>> kToRecall, LinkedHashMap<Integer, List<Float>> kToF1) {

        String precisionList = "Precision\t Recall \t F1\n";
        for (int y = 0; y < queriesWithParagraphs.size(); y++) {
            precisionList += queriesWithParagraphs.get(y) + "\t";
            for (int x : kVals) {
                precisionList += kToPrecision.get(x).get(y) + "\t" + kToRecall.get(x).get(y) + "\t" + kToF1.get(x).get(y) + "\t";
            }
            precisionList += "\n";

        }

        try {
            csvWriter.write(precisionList);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int x : kVals) {
            System.out.printf("Average Precision at %d: %f\t \n", x, kToPrecision.get(x).stream().mapToDouble(a -> a).average().orElse(0.0));
            System.out.printf("Average Recall at %d: %f\t\t \n", x, kToRecall.get(x).stream().mapToDouble(a -> a).average().orElse(0.0));
            System.out.printf("Average F1 at %d: %f\t\t\t \n", x, kToF1.get(x).stream().mapToDouble(a -> a).average().orElse(0.0));
        }

    }

    public static void getRMTerms(String queryId, String query, String path) {
        String indexPath = path;
        PrintStream rmTermStream = null;
        Map<String, List<String>> qrelIdToText = QrelMapCreation.qrelMatch(false);

        try {
            String testPath = "src/main/resources/data/rm-terms.txt";
            rmTermStream = new PrintStream(new FileOutputStream(testPath, true));

            Parameters params = Parameters.create();
            params.put("query", query);
            params.put("numTerms", 1000);
            params.put("index", indexPath);
            params.put("requested", 1000);

            String list = GetRMTermsFn.run(params, rmTermStream);
            String qrelText = "";
            String finalRmTermsSet = "";

            if (qrelIdToText != null && qrelIdToText.containsKey(queryId))
                qrelText = String.join(" ", qrelIdToText.get(queryId));

            Set<String> qrelVocabSet = new HashSet<>(Arrays.asList(qrelText.trim().split(" ")));

            for(String word: Arrays.asList(list.split(" ")).subList(0,500)){
                String[] wList = word.split("%20");
                for(String w: wList){
                    if(!qrelVocabSet.isEmpty()
                            && qrelVocabSet.contains(w)
                            && !stopWords.contains(w)
                            && w.length() > 2
                            && !w.replaceAll("[0-9]","").equals(""))

                        finalRmTermsSet += w.replace("\n","") + " ";

                }

            }

            rmTermStream.append(query).append("@").append(finalRmTermsSet).append("\n");
            rmTermStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            if (rmTermStream != null) {
                rmTermStream.close();
            }
        }
    }
}