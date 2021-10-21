import java.io.File;
import java.nio.file.*;
import java.util.Scanner;

public class CacheSimulator {
  // Variables used to handle cache parameters,
  public String trace_file;
  Cache l1_cache = null;
  Cache l2_cache = null;
  private int trace_file_lines;

  // Initiates variable when class is instantiated,
  public CacheSimulator(String[] args) {
    // Check if correct number of arguments are passed,
    if (args.length != 8) {
      System.out.println("You must enter all input");
      System.out.println(
          "<BLOCKSIZE> <L1_SIZE> <L1_ASSOC> <L2_SIZE> <L2_ASSOC> <REPLACEMENT_POLICY> <INCLUSION_PROPERTY> <trace_file>");
      System.exit(0);
    }
    // Assigns value to cache
    int block_size = Integer.parseInt(args[0]);
    int l1_size = Integer.parseInt(args[1]);
    int l1_assoc = Integer.parseInt(args[2]);
    int l2_size = Integer.parseInt(args[3]);
    int l2_assoc = Integer.parseInt(args[4]);
    int replacement_policy = Integer.parseInt(args[5]);
    int inclusion_property = Integer.parseInt(args[6]);

    if ((block_size & block_size - 1) != 0) {
      System.out.println("Blocksize must be power of 2");
      System.exit(0);
    }

    String trace_file_name = args[7];
    TraceObject trace_list[] = loadFile(trace_file_name);

    // Create simulated cache
    this.l1_cache = new Cache(block_size, l1_size, l1_assoc, replacement_policy, inclusion_property, trace_list);
    if (l2_size > 0) {
      this.l2_cache = new Cache(block_size, l2_size, l2_assoc, replacement_policy, inclusion_property, trace_list);
    }

    // Sends index of operation for first cache to handle
    for (int i = 0; i < this.trace_file_lines; i++) {
      this.l1_cache.checkCache(i, l2_cache);
    }
  }

  private TraceObject[] loadFile(String trace_file_name) {
    // Loads file.
    File trace_file = new File(trace_file_name);
    TraceObject trace_list[];
    try {
      // Reads number of lines in file.
      this.trace_file_lines = (int) Files.lines(Paths.get(trace_file_name)).count();

    } catch (Exception e) {
      System.out.println("Unable to open file");
      System.out.println("Error: " + e);
      System.exit(0);
    }
    // Initializes array of objects to keep trace data.
    trace_list = new TraceObject[this.trace_file_lines];
    // Sets index to keep track of tracelist.
    int trace_index = 0;

    try {
      // Begin reading file.
      Scanner trace_file_read = new Scanner(trace_file);

      // Goes through each line of file, extracts opcode and address.
      while (trace_file_read.hasNextLine()) {
        String[] trace_file_line = trace_file_read.nextLine().split(" ");
        String op_code = trace_file_line[0];
        String hex_address = trace_file_line[1];
        trace_list[trace_index] = new TraceObject(op_code, hexToBinary(hex_address));
        trace_index++;
      }
      trace_file_read.close();

    } catch (Exception e) {
      System.out.println("Unable to open file");
      System.out.println("Error: " + e);
      System.exit(0);
    }

    return trace_list;
  }

  public String hexToBinary(String hexAddress) {
    // Converts hex to decimal, then converts decimal to a binary string.
    long decimalRepresentation = (Long.parseLong(hexAddress.toUpperCase(), 16));
    String binaryString = Long.toBinaryString(decimalRepresentation);
    // Some adds leading zeroes because some input hex values omit them
    while (binaryString.length() < 31) {
      binaryString = "0" + binaryString;
    }
    return binaryString;
  }

}

class TraceObject {
  String opCode;
  String binaryAddress;

  TraceObject(String opCode, String binaryAddress) {
    this.opCode = opCode;
    this.binaryAddress = binaryAddress;
  }
}