package enrichmentapi.calc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;

public class SoReader {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        readOverlap();
    }

    public static void readOverlap() throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream("C:/Users/Nikita_Stepochkin/Desktop/sigcom/enrichr_uid.so");
        ObjectInputStream in = new ObjectInputStream(fis);
        Object file = in.readObject();
        HashMap<String, short[]> genelist = (HashMap<String, short[]>) ((HashMap) file).get("geneset");
        System.out.println(genelist.size());
        HashMap<Short, String> revDictionary = (HashMap<Short, String>) ((HashMap) file).get("revDictionary");
        HashMap<String, Short> dictionary = (HashMap<String, Short>) ((HashMap) file).get("dictionary");
    }

    public static void readRank() throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream("C:/Users/Nikita_Stepochkin/Desktop/sigcom/enrichr_uid.so");
        ObjectInputStream in = new ObjectInputStream(fis);
        Object file = in.readObject();
        String[] entity_id = (String[]) ((HashMap) file).get("entity_id");
        String[] signature_id = (String[]) ((HashMap) file).get("signature_id");
        short[][] ranks = (short[][]) ((HashMap) file).get("rank");
        System.out.println(ranks.length);
    }

}
