package car.GalagoFunctions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.stanford.nlp.util.Sets;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public class getDoc {
    private static List<String> stopWords = new ArrayList<>();
    private static String indexPath ="/run/media/leftclick/Games Volume/indices/paragraphcorpus2";
    private static String testIndexpath = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/indices/paragraphcorpus2.0/postings";
    private static String paragraphCorpusPath = "data/paragraphCorpus.txt";
    public static Retrieval index;
    static {
        try {
            index = RetrievalFactory.instance(indexPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Document.DocumentComponents dc = new Document.DocumentComponents(false,false,true);

    public static void main(String args[]) throws Exception {
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
//        match();
//        getJsonParagraphs();
//         getReducedVocab();
    }


    public static void getReducedVocab(){
//        PrintStream stream = new PrintStream(new File("src/main/resources/vocabComplete.txt"));
//        App.run(new String[]{
//                "dump-term-stats",
//                indexPath + "/postings"
//        }, stream);
        BufferedReader reader = null;
        BufferedWriter writer = null;
        BufferedReader ranking_reader = null;
        try {

            reader = new BufferedReader(new FileReader(new File("src/main/resources/vocabComplete.txt")));
            writer = new BufferedWriter(new FileWriter(new File("src/main/resources/400kvocab.txt")));
            ranking_reader = new BufferedReader(new FileReader(new File("../../Rankings/qrel-ranking-test.txt")));

            List<String> trainWords = new ArrayList<>();
            String ranking_line;

            while ((ranking_line = ranking_reader.readLine()) != null) {
                String word = ranking_line.split(" ")[1].toLowerCase();
                trainWords.add(word);
            }

            String line;
            List<String> completeVocab = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                List<String> splitLine = Arrays.asList(line.split("[\\s+]"));
                if (splitLine.size() == 3) {

                    if (Integer.parseInt(splitLine.get(1)) < 50
//                            || stopWords.contains(splitLine.get(0))
                            || splitLine.get(0).startsWith("enwiki:")
                            || splitLine.get(0).length() < 3) {

                        continue;
                    }
                    completeVocab.add(splitLine.get(0).toLowerCase());
                }
            }

            HashSet<String> trainSet = new HashSet(trainWords);
            HashSet<String> globalSet = new HashSet<>(completeVocab);

            Set overlap = Sets.intersection(trainSet, globalSet);

            for (String x : completeVocab) {
                writer.write(x + "\n");
            }

        } catch(IOException e){
            e.printStackTrace();
        } finally{
            try {
                writer.close();
                reader.close();
                ranking_reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void getJsonParagraphs() throws IOException {
        System.out.println("Started writing out paragraphs");

        BufferedReader reader = new BufferedReader(new FileReader(new File("src/main/resources/data/General/train-heading-rih-bow.run")));
        BufferedReader test_reader = new BufferedReader(new FileReader(new File("src/main/resources/data/General/test-heading-rih-bow.run")));

        Map<String,List<List<String>>> batchMap = new HashMap();
        Map<String,List<Set<String>>> batchMapIndependent = new HashMap();

        String line;
        String[] splitLine;

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        int x = 0;
        while((line = test_reader.readLine()) != null) {

            splitLine = line.split(" ");
            String paragraph = idToText(splitLine[2].trim());
            List<String> vocab = null;
            HashSet<String> vocab_set = null;
            if (paragraph != null) {
                vocab = Arrays.asList(paragraph.split(" "));
                vocab = vocab.stream().filter(w -> w.length() > 2).filter(w -> !stopWords.contains(w)).collect(Collectors.toList());
                vocab_set = new HashSet<>(vocab);

                if (!batchMap.containsKey(splitLine[0])) {
//                    List<List<String>> containingList = new ArrayList<>();
                    List<Set<String>> containingListIndy = new ArrayList<>();
                    containingListIndy.add(vocab_set);
//                    containingList.add(vocab);
//                    batchMap.put(splitLine[0], containingList);
                    batchMapIndependent.put(splitLine[0],containingListIndy);
                } else {
//                    batchMap.get(splitLine[0]).add(vocab);
                    batchMapIndependent.get(splitLine[0]).add(vocab_set);
                }
            }
            x++;
        }

//        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("src/main/resources/data/Local_ELMo/test-subset-localELMo.json")));
        BufferedWriter independent_writer = new BufferedWriter(new FileWriter(new File("src/main/resources/data/Local_ELMo/test-subset-indy-localELMo.json")));
        BufferedReader subset_reader = new BufferedReader(new FileReader(new File("src/main/resources/data/General/test-random-subset.txt")));

        List<String> subset_keyset = new ArrayList<>();
        String subset_line;
        while((subset_line= subset_reader.readLine()) != null){
            subset_keyset.add(subset_line.replace("\n",""));
        }

        independent_writer.write("{\"queries\":[");
        x = 0;
//        List<String> keys = new ArrayList(batchMapIndependent.keySet());
//        Collections.shuffle(keys);
        for(String queryId: subset_keyset) {
            ObjectNode text = mapper.createObjectNode();

            text.put("QueryId", queryId);
            ArrayNode arrayNode = text.putArray("Paragraphs");
            for (Set<String> val : batchMapIndependent.get(queryId)) {
                arrayNode.add(val.toString());
            }
            if(x != 49){
                independent_writer.write(text.toString() + ",");
            }else{
                independent_writer.write(text.toString());
            }
            x++;
        }

        independent_writer.write("]}");
        independent_writer.close();
    }

    public static String idToText(String id) {
        Document doc = null;
        try {
            // Get a document
            doc = index.getDocument(id, dc);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        if(doc != null){
            String cleanedText = doc.text.replaceAll("<page>(.+)page> ","")
                    .replaceAll("<link(.+)/link> ","")
                    .replaceAll("[{}()\\[\\]\']","")
                    .replaceAll("[.]"," FullStop ")
                    .replaceAll("%20"," ")
                    .replaceAll("[^A-Za-z0-9]"," ")
                    .toLowerCase();
//                    .replaceAll("[0-9]"," __NUMBER__ ")
            return cleanedText;
        }

        return null;
    }

    public static List<String> idToTerms(String id){
        Document.DocumentComponents dc = new Document.DocumentComponents(false, false, true);
        try {
            Retrieval index = RetrievalFactory.instance(indexPath);
            List<String> terms = index.getDocument(id, dc).terms;
            index.close();

            return terms;
        } catch(Exception e){
            System.out.println("Document not found");
            return null;
        }
    }

    private static void match() throws Exception {
        String qrelData = "src/main/resources/test.pages.cbor.tree.qrels";
        String qrelMap = "src/main/resources/data/qrelMap-test.txt";
        BufferedReader reader;
        String line;
        BufferedWriter writer;

        try{
            reader = new BufferedReader(new FileReader(new File(qrelData)));
            writer = new BufferedWriter(new FileWriter(new File(qrelMap)));
        } catch(FileNotFoundException e){
            System.out.println("File not found");
            return;
        }

        try{
            while((line = reader.readLine()) != null){
                System.out.println(line);
                String [] splitLine = line.split(" ");
                System.out.println("Query " + splitLine[0] + " paragraph: " + splitLine[2]);
                String terms;
                if((terms = getDoc.idToText(splitLine[2])) != null) {
                    System.out.println(terms);
                    writer.write(splitLine[0] + " : " + terms + "\n");
                }
            }
        } catch(IOException e){
            System.out.println("No content found");
        }

        reader.close();
    }

}
