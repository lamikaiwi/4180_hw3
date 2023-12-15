package source;

public class ChunkInfo {

    public String hash;
    public int containerID;
    public int offset; // offset in the container
    public int size;
    public int usage_count;

    public ChunkInfo(String hash, int containerID, int offset, int size, int usage_count){
        this.hash = hash;
        this.containerID = containerID;
        this.offset = offset;
        this.size = size;
        this.usage_count = usage_count;
    }
}