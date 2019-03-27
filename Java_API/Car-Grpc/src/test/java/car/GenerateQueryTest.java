package car;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenerateQueryTest {
    public static Map<String,String> arrayValsSdm;
    public static Map<String,String> arrayValsBow;
    public static String testQuerystring = "enwiki:Agriprocessors/Controversies/Anti-competitive%20practices";

    @BeforeEach
    void setUp(){
//        try {
////            arrayValsBow = GenerateQuery.queries(testQuerystring,"combine");
////            arrayValsSdm = GenerateQuery.queries(testQuerystring,"sdm");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Assertions.fail();
//        }
    }

    @Test
    void testSdmRIHFormat(){
        String rihQuery = arrayValsSdm.get("rih");
        Assert.assertEquals("{\"number\":\"enwiki:Agriprocessors/Controversies/Anti-competitive%20practices\",\"text\":\"#sdm:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (agriprocessors controversies anti-competitive practices)\"}", rihQuery);
    }
    @Test
    void testSdmRFormat(){
        String rihQuery = arrayValsSdm.get("r");
        Assert.assertEquals("{\"number\":\"enwiki:Agriprocessors/Controversies/Anti-competitive%20practices\",\"text\":\"#sdm:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (agriprocessors)\"}",rihQuery);
    }
    @Test
    void testSdmRIFormat(){
        String rihQuery = arrayValsSdm.get("ri");
        Assert.assertEquals("{\"number\":\"enwiki:Agriprocessors/Controversies/Anti-competitive%20practices\",\"text\":\"#sdm:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (agriprocessors controversies)\"}",rihQuery);
    }
    @Test
    void testBowRIHFormat(){
        String rihQuery = arrayValsBow.get("rih");
        Assert.assertEquals("{\"number\":\"enwiki:Agriprocessors/Controversies/Anti-competitive%20practices\",\"text\":\"#combine:uniw=0.82:odw=0.10:uww=0.02:mu=1200 (agriprocessors controversies anti-competitive practices)\"}",rihQuery);
    }

}

