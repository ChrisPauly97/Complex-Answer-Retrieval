package car;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lemurproject.galago.core.index.IndexPartReader;
import org.lemurproject.galago.core.index.KeyIterator;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class getDocTest {

    private static String indexPath ="/run/media/leftclick/Games Volume/indices/paragraphcorpus2";
    public static Retrieval index;
    @BeforeEach
    void setup(){
        try {
            index = RetrievalFactory.instance(indexPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetVocab() {
        try{
            IndexPartReader reader = DiskIndex.openIndexPart(indexPath + "/postings");
            KeyIterator iterator = reader.getIterator();
            while (!iterator.isDone()) {
                String term = iterator.getKeyString();
                System.out.println(term);
                iterator.nextKey();
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

}
