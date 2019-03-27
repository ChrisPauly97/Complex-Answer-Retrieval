package car.GalagoFunctions;

import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import org.lemurproject.galago.core.tools.App;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;

public class RunEval {

    public static String qrelPath = "src/main/resources/train.pages.cbor.tree.qrels";
//    public static String qrelPath = "src/main/resources/all.tree.qrels";
    public static String qrelPathTest = "src/main/resources/test.pages.cbor.tree.qrels";
    public static final String searchPath = "../../EvalOutput/glove-expansion-";
    public static String searchOutPath = "../../BatchSearchOutput/ranking-combine.txt";
    public static String evalOut = "../../EvalOutput/eval-ranking-test.txt";
    public static String evalFolder = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Queries/ELMo/Ttest/Train";
    public static String evalFolderTest = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Queries/ELMo/Ttest/Test";
    public static String baselineEvalTest = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Queries/ELMo/Ttest/Test/batch-Average-0-testset.txt";
    public static String baselineEval = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Queries/ELMo/Ttest/Train/batch-Average-0-trainset.txt";

    public static void main(String args[]) throws Exception {
        runEval();
    }

    public static void runEval() {
        PrintStream stream = null;
        File[] testEval = new File(evalFolderTest).listFiles();
        File[] trainEval = new File(evalFolder).listFiles();

        try {
            for(File fileEntry : testEval){
                stream = new PrintStream(new File("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Queries/ELMo/Ttest/Test_T/ttest-" + fileEntry.getName() + ".txt"));

                App.run(new String[]{"eval",
                                "--baseline=" + baselineEvalTest,
                                "--judgments=" + qrelPathTest,
                                "--treatment=" + fileEntry.getAbsolutePath(),
                                "--details=true",
//                            "--metrics+" + "map", "--metrics+" + "ndcg", "--metrics+" + "r-prec",
                                "--comparisons=ttest"
                        },
                        stream);
            }

//            for(File fileEntry : trainEval){
//                System.out.println(fileEntry);
//                stream = new PrintStream(new File("/home/leftclick/Desktop/CAR/complex-answer-retrieval/Queries/ELMo/Ttest/Train_T/ttest-" + fileEntry.getName() + ".txt"));
//
//                App.run(new String[]{"eval",
//                                "--baseline=" + baselineEval,
//                                "--judgments=" + qrelPath,
//                                "--treatment=" + fileEntry.getAbsolutePath(),
//                                "--details=true",
////                            "--metrics+" + "map", "--metrics+" + "ndcg", "--metrics+" + "r-prec",
//                                "--comparisons=ttest"
//                        },
//                        stream);
//            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            e.printStackTrace();
        }
        stream.close();

    }
}
