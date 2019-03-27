package car;


import car.GalagoFunctions.OutlinePipeline;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;


public class GenerateQueryFileTest {
    public static String outlinePath = "/home/leftclick/Desktop/CAR/complex-answer-retrieval/Java_API/Car-Grpc/src/main/resources/data/benchmarkY1/fold-1-train.pages.cbor-outlines.cbor";
    public static ArrayList<String> allQueries;
    public static Map<String,String> arrayValsSdm;
    public static Map<String,String> arrayValsBow;

    @BeforeEach
    void setUp(){
        allQueries = new ArrayList<>();
        try {
            arrayValsBow = OutlinePipeline.queries(outlinePath,"combine",3);
            arrayValsSdm = OutlinePipeline.queries(outlinePath, "sdm",3);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSdmRIHFormat() {
        String[] rihQuery = arrayValsSdm.get("rih").split(",");

        for(int x = 0; x < rihQuery.length; x += 2){
            allQueries.add(String.join(",",rihQuery[x],rihQuery[x+1]));
        }

        Assert.assertEquals("{\"number\":\"enwiki:Carbohydrate/Structure\",\"text\":\"#sdm:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (carbohydrate structure)\"}",allQueries.get(0));
    }

    @Test
    void testSdmRFormat() {
        String[] rQuery = arrayValsSdm.get("r").split(",");
        for(int x = 0; x < rQuery.length; x += 2){
            allQueries.add(String.join(",",rQuery[x],rQuery[x+1]));
        }

        Assert.assertEquals("{\"number\":\"enwiki:Carbohydrate/Structure\",\"text\":\"#sdm:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (carbohydrate)\"}",allQueries.get(0));
    }

    @Test
    void testSdmRIFormat() {
        String[] riQuery = arrayValsSdm.get("ri").split(",");
        for(int x = 0; x < riQuery.length; x += 2){
            allQueries.add(String.join(",",riQuery[x],riQuery[x+1]));
        }

        Assert.assertEquals("{\"number\":\"enwiki:Carbohydrate/Structure\",\"text\":\"#sdm:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (carbohydrate)\"}",allQueries.get(0));
    }

    @Test
    void testBowFormat() {
        String[] riQuery = arrayValsBow.get("rih").split(",");
        for(int x = 0; x < riQuery.length; x += 2){
            allQueries.add(String.join(",",riQuery[x],riQuery[x+1]));
        }

        Assert.assertEquals("{\"number\":\"enwiki:Carbohydrate/Structure\",\"text\":\"#combine:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (carbohydrate structure)\"}s",allQueries.get(0));
    }


}
