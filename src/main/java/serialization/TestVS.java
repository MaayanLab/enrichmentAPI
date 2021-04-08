package serialization;

import java.io.File;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import io.jhdf.api.Node;

public class TestVS {
    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        
        File file = new File("data/lincs_rank.h5");
        HdfFile hdfFile = new HdfFile(file);

        String[] colid = (String[]) hdfFile.getDatasetByPath("meta/colid").getData();
        String[] rowid = (String[]) hdfFile.getDatasetByPath("meta/rowid").getData();

        short[][] ranks = new short[colid.length][rowid.length];

        try{
            
            Group g = (Group) hdfFile.getChild("data");
            Group g2 = (Group) g.getChild("expression");

            int count = 0;
            for (Node node : g2) {
                count++;
            }
            
            int colcount = 0;
            for(int i=0; i<count; i++){
                short[][] tt = (short[][]) hdfFile.getDatasetByPath("data/expression/"+i).getData();
                for(int v=0; v<tt[0].length; v++){
                    for(int k=0; k<tt.length; k++){
                        ranks[colcount][k] = tt[k][v];
                    }
                    colcount++;
                }
            }
            
            System.out.println(System.currentTimeMillis() - time);
            System.out.println(ranks.length+" - "+ranks[0].length);
            
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
