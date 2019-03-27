package car.qrel;

import car.qrel.QrelMapCreation;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PseudoQrelRanking {
    private static List<String> stopWords = new ArrayList<>();
    public static String qrelTest = "../../Rankings/qrel-ranking-test.txt";

    static {
        String STOP_WORDS = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/General/Stopwords";
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

    public static void main(String[] args) {
        generateQrelJudgements();
    }

    public static void generateQrelJudgements() {
        Map<String, List<String>> qrelMap = QrelMapCreation.qrelMatch(true);
        BufferedWriter queryWriter = null;
        try {
            queryWriter = new BufferedWriter(new FileWriter(new File(qrelTest)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String q : qrelMap.keySet()) {
            if (qrelMap.containsKey(q)) {
                List<String> paragraphs = qrelMap.get(q);
                String completeSet = String.join(" ", paragraphs);
                completeSet.replaceAll("[^a-zA-Z0-9 ]", "").split("[ ]");
                HashSet<String> splitSet = new HashSet<>(Arrays.asList(completeSet.split("[\\s+]")));
                splitSet.removeAll(stopWords);
                splitSet = splitSet.stream().filter(x -> x.length() > 2).collect(Collectors.toCollection(HashSet::new));

                try {
                    for (String qrelWord : splitSet) {
                        queryWriter.write(q + " " + qrelWord + " " + 1 + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            queryWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

