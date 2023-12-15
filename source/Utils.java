package source;

import java.util.Scanner;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;

public class Utils {
    public static final String DATA_DIR = "data";
    public static final String CONTAINER_DIR = DATA_DIR + "/containers";
    public static final String INDEX_FILE_PATH = DATA_DIR + "/mydedup.index";
    public static final String RECIPE_DIR = DATA_DIR + "/recipe";
    public static final String STATISTIC_PATH = DATA_DIR + "/stats";
    public static final String HASH_METHOD = "SHA-1";
    public static final int CONTAINER_SIZE = 1048576; // 1MB
    public static final int CONTAINER_LOAD_BUFFER_COUNT = 16; // means it can have 16 containers in memory at the same time

    private static MessageDigest _md;
    private static ExecutorService threadPool;
    private static Statistic stat;
    private static IndexMap indices;
    private static Container container;

    public static MessageDigest getMD(){
        if (_md == null) {
            try {
                _md = MessageDigest.getInstance(HASH_METHOD);
            } catch (NoSuchAlgorithmException e) {
                System.out.print("get MessageDigest error\n");
            }
        }
        return _md;
    }
    public static ExecutorService getThreadPool(){
        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        return threadPool;
    }
    public static Statistic getStatistic(){
        if (stat == null) {
            stat = new Statistic();
        }
        return stat;
    }
    public static IndexMap getIndexMap(){
        if (indices == null) {
            indices = new IndexMap();
        }
        return indices;
    }
    public static Container getContainer(){
        if (container == null) {
            container = new Container();
        }
        return container;
    }
    public static void addTask(Runnable task){
        getThreadPool().submit(task);
    }
    public static void exit(){
        // release resources & dump data
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    public static byte[] getFileBytes(File file) {
        ByteArrayOutputStream out_str = null;
        try {
            byte[] buffer = new byte[4096];
            out_str = new ByteArrayOutputStream();
            try(FileInputStream in_st = new FileInputStream(file)) {
                int read = 0;
                while ((read = in_st.read(buffer)) != -1)
                    out_str.write(buffer, 0, read);
            } catch (IOException e) {
                System.out.print("getFileBytes in stream error" + e.getMessage() + "\n");
            }
        } finally {
            try {
                if (out_str != null)
                    out_str.close();
            } catch (IOException e) {
                System.out.print("getFileBytes out stream error" + e.getMessage() + "\n");
            }
        }
        return out_str.toByteArray();
    }
    public static String getHash(byte[] chunk) {
        MessageDigest md = getMD();
        md.reset();
        md.update(chunk, 0, chunk.length);
        byte[] checksumBytes = md.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : checksumBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    public static boolean isChunkSizeNotValid(int n) {
        return !((n >= 2) && ((n & (n - 1)) == 0)); // n>=2 or power of 2
    }
    public static String praseFilePathForSaving(String filePath){
        String absPath = new File(filePath).getAbsolutePath();
        return getHash(absPath.getBytes());
    }

    public static void createRecipe(String filePath, ArrayList<String> chunkIDs, int fileSize){
        try {
            String absPath = new File(filePath).getAbsolutePath();
            File receiptPath = new File(RECIPE_DIR + "/" + Utils.getHash(absPath.getBytes()));
            receiptPath.getParentFile().mkdirs();
            receiptPath.createNewFile();
            FileOutputStream receiptStream = new FileOutputStream(receiptPath);
            
            receiptStream.write(("original file path: "+ absPath).getBytes()); // for easy debug
            receiptStream.write("\n".getBytes());

            receiptStream.write(("fileSize: "+ fileSize).getBytes()); // for easy debug
            receiptStream.write("\n".getBytes());

            for (int i = 0; i < chunkIDs.size(); i++) {
                receiptStream.write(chunkIDs.get(i).getBytes());
                if (i != chunkIDs.size() - 1) {
                    receiptStream.write("\n".getBytes());
                }
            }
            receiptStream.close();
        } catch (IOException e) {
            System.out.print("create receipt file error\n");
        }
    }

    public static Tuple<ArrayList<String>, Integer> loadRecipe(String recipe_filename){
        File recipe_file = new File(RECIPE_DIR + "/" + recipe_filename);
        if (!recipe_file.exists()){
            System.out.println("recipe file '" + recipe_filename + "' not exist!");
            System.exit(-1);
        }
        ArrayList<String> chunkIDs = new ArrayList<String>();
        int fileSize = 0;
        try {
            Scanner reader = new Scanner(recipe_file);
            reader.nextLine(); // skip the first line
            fileSize = Integer.parseInt(reader.nextLine().split(" ")[1]);
            while (reader.hasNextLine()) {
                chunkIDs.add(reader.nextLine());
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.print("load recipe file not found!\n");
        }
        return new Tuple<ArrayList<String>, Integer>(chunkIDs, fileSize);
    }
    public static byte[] loadFileFromRecipe(ArrayList<String> chunkIDs, int fileSize){
        byte[] fileBytes = new byte[fileSize];
        IndexMap indices = getIndexMap();
        int current_size = 0;
        for (int i = 0; i < chunkIDs.size(); i++) {
            ChunkInfo info = indices.get(chunkIDs.get(i));
            if (info == null) {
                System.out.print("chunkID: " + chunkIDs.get(i) + " not found in index file!\n");
                System.exit(-1);
            }
            byte[] chunkBytes = Container.GetOrLoad(info.containerID).getData(info.offset, info.size);
            System.arraycopy(chunkBytes, 0, fileBytes, current_size, info.size);
            current_size += info.size;
        }
        return fileBytes;
    }
    
}
