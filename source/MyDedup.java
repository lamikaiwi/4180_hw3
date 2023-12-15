package source;

import java.lang.Math;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.ArrayList;

public class MyDedup {

    private static long debug_time_count=0; // for time testing only

    private static void upload(int min_chunk, int avg_chunk, int max_chunk, int base, String filePath) throws Exception {
        IndexMap indices = Utils.getIndexMap();
        Statistic stat = Utils.getStatistic();

        ArrayList<String> chunkIDs = new ArrayList<String>();

        File originFile = new File(filePath);
        if (!originFile.exists()){
            System.out.println("file not exist!");
            System.exit(-1);
        }
        byte[] fileBytes = Utils.getFileBytes(originFile);
        byte[] buffer = new byte[max_chunk];
        int current_buffer_size = 0;
        int rfp = 0; // Rabin finger print value
        int c=0;
        int mask = avg_chunk - 1;
        final int base_max_pow = (int)Math.pow(base, min_chunk-1);

        for (int i=0; i<fileBytes.length; i++){
            c = fileBytes[i];
            buffer[current_buffer_size] = (byte)c;
            current_buffer_size++;
            if (i<=min_chunk){
                rfp = (rfp * base + c) & mask;
            }else{
                rfp = (base*(rfp - (mask & base_max_pow*fileBytes[i-min_chunk])) + c) & mask;
                rfp = (rfp>=0)?rfp:(rfp+avg_chunk); // make sure rfp is positive
            }

            if ((current_buffer_size >= max_chunk) || (rfp == 0 && current_buffer_size>=min_chunk) || (i == fileBytes.length - 1)) {
                byte[] realBuffer = Arrays.copyOfRange(buffer, 0, current_buffer_size);
                ChunkInfo chunkInfo = Container.AddChunk(realBuffer);
                chunkIDs.add(chunkInfo.hash);
                stat.pre_dedup_chunk_count++;
                stat.pre_dedup_chunk_size += chunkInfo.size;
                stat.unique_chunk_count += chunkInfo.usage_count == 1 ? 1 : 0;
                stat.unique_chunk_size += chunkInfo.usage_count == 1 ? chunkInfo.size : 0;
                current_buffer_size = 0;
                buffer = new byte[max_chunk];
            }
        }
        Container.TryDumpLastContainer();

        indices.dump();
        Utils.createRecipe(filePath, chunkIDs, fileBytes.length);

        stat.file_count++;
        stat.dump();
        stat.print();
    }
    
    private static void download(String down_filename, String save_filename) throws Exception {
        String file_recipe_name = Utils.praseFilePathForSaving(down_filename);
        File file_recipe = new File(Utils.RECIPE_DIR + "/" + file_recipe_name);
        if (!file_recipe.exists()){
            System.out.println("file: " + down_filename + " not exist!");
            System.exit(-1);
        }
        Tuple<ArrayList<String>, Integer> recipe = Utils.loadRecipe(file_recipe_name);
        byte[] fileBytes = Utils.loadFileFromRecipe(recipe.first, recipe.second);
        
        FileOutputStream fw = new FileOutputStream(save_filename);
        fw.write(fileBytes);
        fw.close();
    }

    public static void main(String[] args) throws Exception {
        if (args[args.length - 1].equals("debug")) {
            debug_time_count = System.nanoTime();
        }
        final String c_state = args[0];
        if (c_state.equals("upload")) {
            
            int min_chunk = Integer.valueOf(args[1]);
            if (Utils.isChunkSizeNotValid(min_chunk)){
                System.out.println("min chunk size must >= 2 and power of 2!");
                System.exit(-2);
            }
            
            int avg_chunk = Integer.valueOf(args[2]);
            if (Utils.isChunkSizeNotValid(avg_chunk)){
                System.out.println("avg chunk size must >= 2 and power of 2");
                System.exit(-2);
            }
            
            int max_chunk = Integer.valueOf(args[3]);
            if (Utils.isChunkSizeNotValid(max_chunk)){
                System.out.println("max chunk size must >= 2 and power of 2");
                System.exit(-2);
            }

            int multiplier = Integer.valueOf(args[4]);
            final String upload_filePath = args[5];

            upload(min_chunk, avg_chunk, max_chunk, multiplier, upload_filePath);
        }
        else if (c_state.equals("download")) {
            final String down_filename = args[1];
            final String save_filename = args[2];
            
            download(down_filename, save_filename);

        } else {
            System.out.println("Not upload nor download");
            System.exit(-1);
        }
        if (args[args.length - 1].equals("debug")) {
            System.out.println("time used: " + (System.nanoTime() - debug_time_count) / 1000000 + "ms");
        }

        Utils.exit();   
    }
}