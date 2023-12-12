import java.lang.Math;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyDedup {
    public static int min_chunk = 0, avg_chunk = 0, max_chunk = 0, d = 0, container_offset = 0;
    public static Map<String, String> indexMap = null;
    public static String containerID = ""; // set to be the first chunk id
    public static byte[] container = new byte[1048576];
    public static File fileRecipe;
    public static int[] stats = new int[6];

    // read index file
    public static void getIndexFileMap() throws IOException {
        try {
            File indexFile = new File("mydedup.index");
            if (indexFile.createNewFile()) { // if index file do not exist, just continue without the map
                return;
            } else {
                Scanner reader = new Scanner(indexFile);
                while (reader.hasNextLine()) {
                    String data = reader.nextLine();
                    String[] split_data = data.split(" ");
                    if (split_data[0] == "stats") { // Take the most recent stats
                        stats[Integer.parseInt(split_data[1])] = Math.max(stats[Integer.parseInt(split_data[1])],
                                Integer.parseInt(split_data[2]));
                    } else { // normal chunk
                        indexMap.put(split_data[0], split_data[1]);
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            System.out.print("getIndexFileMap error\n");
        }
    }

    // getFileBytes function returns the byte map of a file
    public static byte[] getFileBytes(File file) throws IOException {
        ByteArrayOutputStream out_str = null;
        InputStream in_st = null;
        try {
            byte[] buffer = new byte[4096];
            out_str = new ByteArrayOutputStream();
            in_st = new FileInputStream(file);
            int read = 0;
            while ((read = in_st.read(buffer)) != -1)
                out_str.write(buffer, 0, read);
        } finally {
            try {
                if (out_str != null)
                    out_str.close();
            } catch (IOException e) {
                System.out.print("getFileBytes out stream error\n");
            }
            try {
                if (in_st != null)
                    in_st.close();
            } catch (IOException e) {
                System.out.print("getFileBytes in stream error\n");
            }
        }
        return out_str.toByteArray();
    }

    // create new chunk
    public static void newChunk(byte[] chunk, int size, String filename, boolean end) {
        try {
            stats[1] += 1; // no. of pre-chunks
            stats[3] += size; // size of pre-chunks
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(chunk, 0, chunk.length);
            byte[] checksumBytes = md.digest();
            String chunkID = Arrays.toString(checksumBytes);

            // update container and update indexfile
            FileWriter fw = new FileWriter(filename, true);
            fw.write(chunkID + "\n");
            fw.close();
            if (!indexMap.containsKey(chunkID)) {
                stats[2] += 1; // no. of unique chunks
                stats[4] += size; // no. of unique chunks
                if ((container_offset + size) >= 1048576) {
                    // when reach max size, flush the container
                    containerID = "";
                    container_offset = 0;
                    container = new byte[1048576];
                    FileOutputStream stream = new FileOutputStream(containerID);
                    stream.write(container);
                    stream.close();
                    stats[5] += 1; // no. of containers
                }
                // make new container if empty
                if (containerID == "") {
                    containerID = chunkID;
                }
                // update index map and index file
                indexMap.put(chunkID, containerID + "-" + container_offset + "-" + size);
                fw = new FileWriter("mydedup.index", true);
                fw.write(chunkID + " " + containerID + "-" + container_offset + "-" + size);
                fw.close();
                // concat the container with the chunk
                int conLen = container.length;
                int chuLen = chunk.length;
                byte[] newContainer = new byte[conLen + chuLen];
                System.arraycopy(container, 0, newContainer, 0, conLen);
                System.arraycopy(chunk, 0, newContainer, conLen, chuLen);

                container_offset += size;
                if (end) {
                    FileOutputStream stream = new FileOutputStream(containerID);
                    stream.write(container);
                    stream.close();
                }
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.print("No SHA-1 Algorithm\n");
        } catch (IOException e) {
            System.out.print("newChunk IO error\n");
        }
    }

    public static void main(String[] args) throws Exception {
        // get metadata
        getIndexFileMap();

        final String c_state = args[0];
        if (c_state == "upload") {
            // read input and validation
            min_chunk = Integer.valueOf(args[1]);
            avg_chunk = Integer.valueOf(args[2]);
            max_chunk = Integer.valueOf(args[3]);
            int[] check = new int[] { min_chunk, avg_chunk, max_chunk };
            for (int i = 0; i < 3; i++) {
                if (check[i] < 2) {
                    System.out.println("chunk size smaller than 2!");
                    System.exit(-2);
                }
                for (int j = check[i]; j > 2; j /= 2) {
                    if (j % 2 != 0) {
                        System.out.println("chunk size not power of 2!");
                        System.exit(-2);
                    }
                }
            }
            d = Integer.valueOf(args[4]);
            final String up_filename = args[5];

            // initialization
            File file = new File(up_filename);
            fileRecipe = new File(up_filename + "_recipe");
            fileRecipe.createNewFile();
            int rfp = 0, p_pt = 0; // Rabin_fingerprint value, previous anchor point
            int base = (int) (Math.pow(Double.valueOf(d), Double.valueOf(min_chunk - 1)));
            int mask = avg_chunk - 1;
            long length = file.length();
            byte[] file_byte = getFileBytes(file); // storing all bytes

            // chunking
            int c_chunk_size = 0; // current chunk size;
            // start calculating the next windows with quicker method
            for (int i = min_chunk; i < length - min_chunk; i++) {
                if (c_chunk_size >= max_chunk) { // reach max_chunk
                    newChunk(Arrays.copyOfRange(file_byte, p_pt + 1, i + 1), i - p_pt, up_filename + "_recipe", false);
                    p_pt = i;
                    c_chunk_size = 0;
                    continue;
                } else if (c_chunk_size < min_chunk) { // starting a new chunk
                    int first_base = base;
                    for (int j = 0; j < min_chunk; j++) {
                        first_base /= d;
                        rfp += (file_byte[j] * (first_base & mask)) & mask;
                        // &mask operates the same as mod (mask+1), which is avg_chunk
                    }
                    i += min_chunk - 1; // need to minus 1 to negate the effect of the i++ in the large loop
                    c_chunk_size += min_chunk;
                } else {
                    // calculate the next window with the algorithm
                    rfp = (d & mask) * ((rfp - file_byte[i - avg_chunk] * (base & mask)) & mask)
                            + file_byte[i];
                    rfp &= mask;
                    c_chunk_size++;
                }
                // set anchor point
                if ((rfp & mask) == 0) {
                    newChunk(Arrays.copyOfRange(file_byte, p_pt + 1, i + 1), i - p_pt, up_filename + "_recipe", false);
                    p_pt = i;
                    c_chunk_size = 0;
                }
            }

            // create chunk for the last part
            newChunk(Arrays.copyOfRange(file_byte, p_pt + 1, (int) length), (int) length - p_pt,
                    up_filename + "_recipe",
                    true);

            // update stats in the index file
            // Note the recent value must >= previous values
            stats[0] += 1;
            FileWriter fw = new FileWriter("mydedup.index", true);
            try {
                for (int i = 0; i < stats.length; i++) {
                    fw.write("stats " + i + " " + stats[i]);
                }
            } catch (IOException e) {
                System.out.println("stats writing error\n");
            } finally {
                fw.close();
            }
            System.out.print("Total number of files that have been stored: " + stats[0] + "\n");
            System.out.print("Total number of pre-deduplicated chunks in storage: " + stats[1] + "\n");
            System.out.print("Total number of unique chunks in storage: " + stats[2] + "\n");
            System.out.print("Total number of bytes of pre-deduplicated chunks in storage: " + stats[3] + "\n");
            System.out.print("Total number of bytes of unique chunks in storage: " + stats[4] + "\n");
            System.out.print("Total number of containers in storage: " + stats[5] + "\n");
            System.out.printf("Deduplication ratio: %.2f\n", (double) stats[3] / (double) stats[4]);

        } else if (c_state == "download") {
            final String down_filename = args[1];
            final String save_filename = args[2];
            // read recipe

            // find index from index file

            // get containers

            // construct file from container with info from indexfile
        } else {
            System.out.println("Not upload nor download");
            System.exit(-1);
        }

    }
}