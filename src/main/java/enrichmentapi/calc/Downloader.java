package enrichmentapi.calc;

import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Downloader {

    private static final String path = "C:/data6/";

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        loadRankData();
    }

    public static void loadRankData() throws IOException, ClassNotFoundException {
        FileInputStream file = new FileInputStream("C:/Users/Nikita_Stepochkin/Desktop/sigcom/lincsfwd_uid.so");
        ObjectInputStream in = new ObjectInputStream(file);
        System.out.println("start reading");
        FileOutputStream fileOutputStream1 = new FileOutputStream(new File(path + "dbe.csv"));
        BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(fileOutputStream1));
        FileOutputStream fileOutputStream2 = new FileOutputStream(new File(path + "dbs.csv"));
        BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(fileOutputStream2));
        FileOutputStream fileOutputStream3 = new FileOutputStream(new File(path + "dbes.csv"));
        BufferedWriter bw3 = new BufferedWriter(new OutputStreamWriter(fileOutputStream3));

        Object ob = in.readObject();
        System.out.println("end reading");
        String[] entity_id = (String[]) ((HashMap) ob).get("entity_id");
        String[] signature_id = (String[]) ((HashMap) ob).get("signature_id");
        short[][] ranks = (short[][]) ((HashMap) ob).get("rank");

        System.out.println(Arrays.asList(ArrayUtils.toObject(ranks[0])).subList(0, 1000));

        int datasetId = 2;

        AtomicLong i = new AtomicLong(3000000);
        for (String entityName : entity_id) {
            long id = i.getAndIncrement();
            bw1.write(id + "," + entityName + "," + datasetId);
            bw1.newLine();
            System.out.println(id);
        }
        bw1.flush();
        bw1.close();

        AtomicLong i2 = new AtomicLong(3000000);
        for (String signatureName : signature_id) {
            long id = i2.getAndIncrement();
            bw2.write(id + "," + signatureName + "," + datasetId);
            bw2.newLine();
            System.out.println(id);
        }
        bw2.flush();
        bw2.close();

        bw3.write("entity,signature,rank");
        bw3.newLine();
        int num = 0;
        AtomicLong i3 = new AtomicLong(3000000);
        for (int n1 = 0; n1 < ranks.length; n1++) {
            long id = i3.getAndIncrement();
            short[] rank = ranks[n1];
            for (int n2 = 0; n2 < rank.length; n2++) {
                long eId = n2 + 3000000;
                long sId = n1 + 3000000;
                bw3.write(eId + "," + sId + "," + rank[n2]);
                bw3.newLine();
            }
            System.out.println(id);
            if (id % 100 == 0) {
                bw3.flush();
                bw3.close();
                FileOutputStream fileOutputStreamm = new FileOutputStream(new File(path + "dbes" + num++ + ".csv"));
                bw3 = new BufferedWriter(new OutputStreamWriter(fileOutputStreamm));
                bw3.write("entity,signature,rank");
                bw3.newLine();
            }
        }
        bw3.flush();
        bw3.close();
    }

    public static void loadOverlapData() throws IOException, ClassNotFoundException {
        FileInputStream file = new FileInputStream("C:/Users/Nikita_Stepochkin/Desktop/sigcom/enrichr_uid.so");
        ObjectInputStream in = new ObjectInputStream(file);
        System.out.println("start reading");
        FileOutputStream fileOutputStream1 = new FileOutputStream(new File(path + "dbe.csv"));
        BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(fileOutputStream1));
        FileOutputStream fileOutputStream2 = new FileOutputStream(new File(path + "dbs.csv"));
        BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(fileOutputStream2));
        FileOutputStream fileOutputStream3 = new FileOutputStream(new File(path + "dbes.csv"));
        final BufferedWriter bw3 = new BufferedWriter(new OutputStreamWriter(fileOutputStream3));

        Object ob = in.readObject();
        System.out.println("end reading");
        HashMap<String, short[]> genelist = (HashMap<String, short[]>) ((HashMap) ob).get("geneset");
        HashMap<Short, String> revDictionary = (HashMap<Short, String>) ((HashMap) ob).get("revDictionary");

        final int datasetId = 3;

        final int idIncr = 400000;

        bw1.write("id,name,dataset");
        bw1.newLine();
        revDictionary.forEach((id, name) -> {
            try {
                int newId = id + idIncr;
                bw1.write(newId + "," + name + "," + datasetId);
                bw1.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        bw1.flush();
        bw1.close();

        bw2.write("id,name,dataset");
        bw2.newLine();
        bw3.write("entity,signature,dataset");
        bw3.newLine();

        AtomicLong i = new AtomicLong(400000);
        genelist.forEach(
                (sigName, entityIds) -> {
                    try {
                        long sId = i.getAndIncrement();
                        bw2.write(sId + "," + sigName + "," + datasetId);
                        bw2.newLine();
                        for (short eId : entityIds) {
                            int newId = eId + idIncr;
                            bw3.write(newId + "," + sId + "," + datasetId);
                            bw3.newLine();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );

        bw2.flush();
        bw2.close();
        bw3.flush();
        bw3.close();
    }

}
