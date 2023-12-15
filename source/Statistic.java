package source;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Statistic {
    
    public int file_count = 0;
    public int pre_dedup_chunk_count = 0;
    public int unique_chunk_count = 0;
    public int pre_dedup_chunk_size = 0;
    public int unique_chunk_size = 0;
    public int container_count = 0;

    public Statistic(){
        loadStat();
    }

    private void loadStat(){
        File filePath = new File(Utils.STATISTIC_PATH);
        filePath.getParentFile().mkdirs();
        try{
            if (!filePath.createNewFile()){
                Scanner reader = new Scanner(filePath);
                String[] data = reader.nextLine().split(" "); // data is separated by spaces, in 1 line only
                file_count = Integer.parseInt(data[0]);
                pre_dedup_chunk_count = Integer.parseInt(data[1]);
                unique_chunk_count = Integer.parseInt(data[2]);
                pre_dedup_chunk_size = Integer.parseInt(data[3]);
                unique_chunk_size = Integer.parseInt(data[4]);
                container_count = Integer.parseInt(data[5]);
                reader.close();
            }
        }catch (IOException e){
            System.out.println("stats reading error\n");
        }
    }
    public void dump(){
        try (FileWriter fw = new FileWriter(Utils.STATISTIC_PATH)){
            fw.write(file_count + " " + pre_dedup_chunk_count + " " + unique_chunk_count + " " + pre_dedup_chunk_size + " " + unique_chunk_size + " " + container_count);
        } catch (IOException e) {
            System.out.println("stats writing error\n");
        }
    }
    public void print(){
        System.out.print("Total number of files that have been stored: " + file_count + "\n");
        System.out.print("Total number of pre-deduplicated chunks in storage: " + pre_dedup_chunk_count + "\n");
        System.out.print("Total number of unique chunks in storage: " + unique_chunk_count + "\n");
        System.out.print("Total number of bytes of pre-deduplicated chunks in storage: " + pre_dedup_chunk_size + "\n");
        System.out.print("Total number of bytes of unique chunks in storage: " + unique_chunk_size + "\n");
        System.out.print("Total number of containers in storage: " + container_count + "\n");
        System.out.printf("Deduplication ratio: %.2f\n", (float)pre_dedup_chunk_size / (float)unique_chunk_size);
    }
}