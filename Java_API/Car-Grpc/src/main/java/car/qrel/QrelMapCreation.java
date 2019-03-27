package car.qrel;

import car.GalagoFunctions.getDoc;

import java.io.*;
import java.util.*;

public class QrelMapCreation {
    private static Map<String, List<String>> qrelIdToText = new HashMap<>();

    public static Map<String, List<String>> qrelMatch(boolean test) {

        String qrelData;
        BufferedReader reader;
        BufferedWriter testWriter = null;
        BufferedWriter trainWriter = null;
        String line;

        try {
            if(test){
                qrelData = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/test.pages.cbor.tree.qrels";
                testWriter = new BufferedWriter(new FileWriter(new File("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/qrelMap-test.txt")));
            }else{
                qrelData = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/train.pages.cbor.tree.qrels";
                trainWriter = new BufferedWriter(new FileWriter(new File("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/qrelMap-train.txt")));
            }

            reader = new BufferedReader(new FileReader(new File(qrelData)));

            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split(" ");

                if (getDoc.idToText(splitLine[2]) != null) {

                    if(test) testWriter.write(splitLine[0] + ":" + getDoc.idToText(splitLine[2]) + "\n");
                    else trainWriter.write(splitLine[0] + ":" + getDoc.idToText(splitLine[2]) + "\n");

                    if (!qrelIdToText.containsKey(splitLine[0])) qrelIdToText.put(splitLine[0], new ArrayList<>(Arrays.asList(getDoc.idToText(splitLine[2]))));
                    else qrelIdToText.get(splitLine[0]).add(getDoc.idToText(splitLine[2]));

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if(trainWriter != null) trainWriter.close();
                if(testWriter != null) testWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return qrelIdToText;
    }
}
