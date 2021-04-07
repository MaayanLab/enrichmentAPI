package serialization;

import java.io.File;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;

public class TestVS {
    public static void main(String[] args) {
        System.out.println("Hello World");

        File file = new File("data/test.h5");

        try{
            long time = System.currentTimeMillis();
            HdfFile hdfFile = new HdfFile(file);
            short[][] tt = (short[][]) hdfFile.getDatasetByPath("data/expression").getData();
            System.out.println(System.currentTimeMillis() - time);
            System.out.println(tt.length+" - "+tt[0].length);
            
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
