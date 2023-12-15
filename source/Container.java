package source;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.LinkedList;

public class Container {
    
    byte[] buffer = new byte[Utils.CONTAINER_SIZE];
    int size = 0;
    int containerID = 0;
    private static LinkedList<Container> containerBufferList;
    private static Container current_uploading_container;
    
    public byte[] getData(int from, int size){
        // get the data from the container
        if (from < 0 || from + size > this.size) {
            System.out.println("get data from container error");
            System.exit(-1);
        }
        byte[] data = new byte[size];
        System.arraycopy(this.buffer, from, data, 0, size);
        return data;
    }

    public static int ContainerCount(){
        Statistic stat = Utils.getStatistic();
        return stat.container_count;
    }
    public static ChunkInfo AddChunk(byte[] chunk){
        //return the latest container ID after add.
        if (current_uploading_container == null) {
            current_uploading_container = new Container();
            int count = ContainerCount();
            current_uploading_container.containerID = count - (count==0?0:1);
        }
        if (current_uploading_container.size + chunk.length > Utils.CONTAINER_SIZE) {
            Container old_container = current_uploading_container;
            current_uploading_container = new Container();
            current_uploading_container.containerID = old_container.containerID + 1;
            Utils.addTask(() -> {
                Container.Dump(old_container.buffer, old_container.containerID);
            });
        }
        String hash = Utils.getHash(chunk);
        IndexMap indices = Utils.getIndexMap();
        int usage_count = 1;
        if (indices.containsKey(hash)) {
            ChunkInfo info = indices.get(hash);
            info.usage_count++;
            return info;
        }
        else{
            System.arraycopy(chunk, 0, current_uploading_container.buffer, current_uploading_container.size, chunk.length);
            int offset = (current_uploading_container.size > 0)? current_uploading_container.size-1: 0;
            int chunkSize = chunk.length;
            int containerID = current_uploading_container.containerID;
            current_uploading_container.size += chunk.length;

            ChunkInfo chunkInfo = new ChunkInfo(hash, containerID, offset, chunkSize, usage_count);
            indices.put(hash, chunkInfo);
            return chunkInfo;
        }
    }
    public static void Dump(byte[] _buffer, int id){
        // dump the container to disk
        if (_buffer.length == 0) {
            return;
        }
        try{
            File containerPath = new File(Utils.CONTAINER_DIR + "/" + id);
            containerPath.getParentFile().mkdirs();
            containerPath.createNewFile();
            FileOutputStream containerStream = new FileOutputStream(containerPath);
            containerStream.write(_buffer, 0, _buffer.length);
            containerStream.close();

            Statistic stat = Utils.getStatistic();
            stat.container_count++;

        } catch (IOException e) {
            System.out.print("create container file error\n");
        }
    }
    public static Container GetOrLoad(int containerID){
        // load the container from disk
        if (containerBufferList == null) {
            containerBufferList = new LinkedList<Container>();
        }
        for (Container container : containerBufferList) {
            if (container.containerID == containerID) {
                // move to the head of the list
                containerBufferList.remove(container);
                containerBufferList.push(container);
                return container;
            }
        }
        // if not found in the buffer, load from disk
        File containerPath = new File(Utils.CONTAINER_DIR + "/" + containerID);
        if (!containerPath.exists()){
            System.out.println("container file '" + containerPath + "' not exist!");
            System.exit(-1);
        }
        byte[] buffer = Utils.getFileBytes(containerPath);
        Container container = new Container();
        container.buffer = buffer;
        container.size = buffer.length;
        container.containerID = containerID;

        containerBufferList.push(container);
        if (containerBufferList.size() > Utils.CONTAINER_LOAD_BUFFER_COUNT) {
            containerBufferList.removeLast();
        }

        return container;
    }
    public static void TryDumpLastContainer(){
        if (current_uploading_container != null) {
            Utils.addTask(() -> {
                Container.Dump(current_uploading_container.buffer, current_uploading_container.containerID);
            });
        }
    }
}
