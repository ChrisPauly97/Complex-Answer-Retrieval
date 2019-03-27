package car.GalagoFunctions;

import org.lemurproject.galago.core.tools.App;

import java.io.File;
import java.io.PrintStream;

public class runBatch {

    private static String indexPath ="/run/media/leftclick/Games Volume/indices/paragraphcorpus2";
    public static final String searchOutPath = "../../BatchSearchOutput/ranking-";
    public static String test = "src/main/resources/data/testFile.json";
    public static void main(String args[]){
        runBatch("test","combine");
    }

    public static void runBatch(String headerType, String queryType) {
        try{
            PrintStream stream = new PrintStream(new File(searchOutPath + "-" + queryType + ".txt"));

            String queryPath;
            if(queryType.equals("combine")){
                queryPath = test;
            }else{
                queryPath = test;
            }
            System.out.println(queryPath);


            App.run(new String[] {"batch-search",
                    "--index=" + indexPath,
                    "--requested=" + "1000",
                    "--scorer=bm25",
                    queryPath},stream
            );
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Usage: runBatch rih combine");
        }

    }

}
