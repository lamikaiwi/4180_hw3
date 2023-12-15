package source;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class IndexMap extends HashMap<String, ChunkInfo>{
    public IndexMap(){
        super();
        this.load();
    }
    private void load() {
        try {
            File indexFile = new File(Utils.INDEX_FILE_PATH);
            indexFile.getParentFile().mkdirs();
            if (indexFile.createNewFile()) { // if index file do not exist, just continue without the map
                return;
            } else {
                Scanner reader = new Scanner(indexFile);
                while (reader.hasNextLine()) {
                    // each line is in the format of '"chunk_id" "usage count"', separated by a space
                    String[] line = reader.nextLine().split(" ");
                    String chunkID = line[0];
                    int containerID = Integer.parseInt(line[1]);
                    int offset = Integer.parseInt(line[2]);
                    int size = Integer.parseInt(line[3]);
                    int usage_count = Integer.parseInt(line[4]);
                    this.put(chunkID, new ChunkInfo(chunkID, containerID, offset, size, usage_count));
                }
                reader.close();
            }
        } catch (IOException e) {
            System.out.print("getIndexFileMap error\n");
        }
    }
    public void dump(){
        try {
            FileWriter fw = new FileWriter(Utils.INDEX_FILE_PATH);
            for (Map.Entry<String, ChunkInfo> entry : this.entrySet()) {
                ChunkInfo info = entry.getValue();
                fw.write(entry.getKey() + " " + info.containerID + " " + info.offset + " " + info.size + " " + info.usage_count + "\n");
            }
            fw.close();
        } catch (IOException e) {
            System.out.print("update index file error\n");
        }
    }
}
