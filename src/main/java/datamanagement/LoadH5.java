package datamanagement;

import java.io.File;
import java.util.HashMap;

import io.jhdf.HdfFile;
import io.jhdf.api.Group;
import io.jhdf.api.Node;

public class LoadH5 {

    public HashMap<String, Object> loadh5(String _file){
        File file = new File(_file);
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
        }
        catch(Exception e){
            e.printStackTrace();
        }

        hdfFile.close();

        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("signature_id", colid);
        result.put("entity_id", rowid);
        result.put("ranks", ranks);

        return result;
    }

}
