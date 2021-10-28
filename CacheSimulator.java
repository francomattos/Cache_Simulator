import java.io.File;
import java.nio.file.*;
import java.util.Scanner;

public class CacheSimulator {
  // Variables used to handle cache parameters,
  // public String trace_file;
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
      this.l2_cache.prevCache = this.l1_cache;
      this.l1_cache.nextCache = this.l2_cache;
    }

    // Sends index of operation for first cache to handle
    for (int i = 0; i < trace_list.length; i++) {
      this.l1_cache.checkCache(i, null, null);
    }
    // Translates replacement policy to text for output.
    String replacement_policy_string = "";
    switch (replacement_policy) {
    case 0:
      replacement_policy_string = "LRU";
      break;
    case 1:
      replacement_policy_string = "Pseudo-LRU";
      break;
    case 2:
      replacement_policy_string = "Optimal";
      break;
    default:
      System.out.println("Unsupported Replacement Policy, 0 for LRU, 1 for PLRU, 2 for Optimal.");
      System.exit(0);
    }

    // Translates inclusion property to text for output.
    String inclusion_property_string = "";
    switch (inclusion_property) {
    case 0:
      inclusion_property_string = "non-inclusive";
      break;
    case 1:
      inclusion_property_string = "inclusive";
      break;

    default:
      System.out.println("Unsupported inclusion property, 0 for non-inclusive, 1 for inclusive.");
      System.exit(0);
    }

    String outputResult = String.format("""
        ===== Simulator configuration =====
        BLOCKSIZE:             %s
        L1_SIZE:               %s
        L1_ASSOC:              %s
        L2_SIZE:               %s
        L2_ASSOC:              %s
        REPLACEMENT POLICY:    %s
        INCLUSION PROPERTY:    %s
        trace_file:            %s""", block_size, l1_size, l1_assoc, l2_size, l2_assoc, replacement_policy_string,
        inclusion_property_string, trace_file_name);
    outputResult += "\n";

    printOutput(outputResult, this.l1_cache, this.l2_cache);

  }

  private void printOutput(String outputResult, Cache l1_cache, Cache l2_cache) {

    if (l1_cache != null) {
      outputResult += "===== L1 contents =====";
      outputResult += "\n";
      String set_line = "";
      String dirty_bit = "";
      String hex_address = "";
      String address = "";
      char first_bit = 0;
      for (int set = 0; set < l1_cache.sets; set++) {
        for (int assoc_index = 0; assoc_index < l1_cache.cache_assoc; assoc_index++) {
          address = l1_cache.cache_memory[assoc_index][set];
          first_bit = address.charAt(0);
          if (first_bit == '1') {
            dirty_bit = " D 	";
          } else {
            dirty_bit = "  	";
          }
          hex_address = binaryToHex(address.substring(1));
          set_line += hex_address.toLowerCase() + dirty_bit;
        }
        outputResult += "Set     " + set + ":	" + set_line;
        outputResult += "\n";
        set_line = "";
      }
    } else {
      System.out.println("Error loading cache 1");
      System.exit(0);
    }

    if (l2_cache != null) {
      outputResult += "===== L2 contents =====";
      outputResult += "\n";
      String set_line = "";
      String dirty_bit = "";
      String hex_address = "";
      String address = "";
      char first_bit = 0;
      for (int set = 0; set < l2_cache.sets; set++) {
        for (int assoc_index = 0; assoc_index < l2_cache.cache_assoc; assoc_index++) {
          address = l2_cache.cache_memory[assoc_index][set];
          first_bit = address.charAt(0);
          if (first_bit == '1') {
            dirty_bit = " D 	";
          } else {
            dirty_bit = "  	";
          }
          hex_address = binaryToHex(address.substring(1));
          set_line += hex_address.toLowerCase() + dirty_bit;
        }
        outputResult += "Set     " + set + ":	" + set_line;
        outputResult += "\n";
        set_line = "";
      }
    }

    double l1_missrate = ((double) l1_cache.read_misses + l1_cache.write_misses)
        / (l1_cache.read_hits + l1_cache.write_hits);
    outputResult += String.format("""
        ===== Simulation results (raw) =====
        a. number of L1 reads:        %s
        b. number of L1 read misses:  %s
        c. number of L1 writes:       %s
        d. number of L1 write misses: %s
        e. L1 miss rate:              %.6f
        f. number of L1 writebacks:   %s
         """, l1_cache.read_hits, l1_cache.read_misses, l1_cache.write_hits, l1_cache.write_misses, l1_missrate,
        l1_cache.write_back);

    if (l2_cache == null) {
      outputResult += String.format("""
          g. number of L2 reads:        0
          h. number of L2 read misses:  0
          i. number of L2 writes:       0
          j. number of L2 write misses: 0
          k. L2 miss rate:              0
          l. number of L2 writebacks:   0
          m. total memory traffic:      %s
                     """, l1_cache.read_misses + l1_cache.write_misses + l1_cache.write_back);
    } else {
      double l2_missrate = ((double) l2_cache.read_misses + l2_cache.write_misses)
          / (l2_cache.read_hits + l2_cache.write_hits);
      outputResult += String.format("""
          g. number of L2 reads:        %s
          h. number of L2 read misses:  %s
          i. number of L2 writes:       %s
          j. number of L2 write misses: %s
          k. L2 miss rate:              %.6f
          l. number of L2 writebacks:   %s
          m. total memory traffic:      %s
                     """, l2_cache.read_hits, l2_cache.read_misses, l2_cache.write_hits, l2_cache.write_misses,
          l2_missrate, l2_cache.write_back, l2_cache.read_misses + l2_cache.write_misses + l2_cache.write_back);
    }

    System.out.print(outputResult);

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
        if (trace_file_line.length > 1) {
          String op_code = trace_file_line[0];
          String hex_address = trace_file_line[1];
          trace_list[trace_index] = new TraceObject(op_code, hexToBinary(hex_address));
        }
        trace_index++;
      }
      trace_file_read.close();

    } catch (Exception e) {
      System.out.println("Error reading file");
      System.out.println("Error: " + e);
      System.exit(0);
    }

    return trace_list;
  }

  public String hexToBinary(String hex_address) {
    // Converts hex to decimal, then converts decimal to a binary string.
    long decimal_representation = (Long.parseLong(hex_address.toUpperCase(), 16));
    String binary_string = Long.toBinaryString(decimal_representation);
    // Adds leading zeroes because some input hex values omit them
    while (binary_string.length() < 31) {
      binary_string = "0" + binary_string;
    }
    return binary_string;
  }

  public String binaryToHex(String binary_address) {
    // Regex to remove leading zeros from a string
    String regex = "^0+(?!$)";

    // Replaces the matched value with given string
    binary_address = binary_address.replaceAll(regex, "");
    int decimal_string = Integer.parseInt(binary_address, 2);
    String hex_string = Integer.toString(decimal_string, 16);
    return hex_string;
  }
}

class TraceObject {
  String op_code;
  String binary_address;

  TraceObject(String op_code, String binary_address) {
    this.op_code = op_code;
    this.binary_address = binary_address;
  }

}
