import java.io.File;
import java.nio.file.*;
import java.util.Scanner;

public class CacheSimulator {
  // Variables used to handle cache parameters,
  public String trace_file;
  Cache l1_cache = null;
  Cache l2_cache = null;

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
    this.trace_file = args[7];

    // Create simulated cache
    this.l1_cache = new Cache(block_size, l1_size, l1_assoc, replacement_policy, inclusion_property);
    if (l2_size > 0) {
      this.l2_cache = new Cache(block_size, l2_size, l2_assoc, replacement_policy, inclusion_property);
    }

    startSimulation();
  }

  // Loads file and begins simulation process.
  public void startSimulation() {
    // Loads file.
    File traceFile = new File(trace_file);

    try {
      // Reads number of lines in file.
      int traceFileLines = (int) Files.lines(Paths.get(trace_file)).count();
      // Initializes array of objects to keep trace data.
      TraceObject traceList[] = new TraceObject[traceFileLines];
      // Sets index to keep track of tracelist.
      int traceIndex = 0;
      // Begin reading file.
      Scanner traceFileRead = new Scanner(traceFile);

      // Goes through each line of file, extracts opcode and address.
      while (traceFileRead.hasNextLine()) {
        String[] traceFileLine = traceFileRead.nextLine().split(" ");
        String opCode = traceFileLine[0];
        String hexAddress = traceFileLine[1];
        traceList[traceIndex] = new TraceObject(opCode, hexToBinary(hexAddress));
        traceIndex++;
      }
      traceFileRead.close();

      // Sends operation for first cache to handle
      for (int i = 0; i < traceFileLines; i++) {
        this.l1_cache.checkCache(i, traceList, l2_cache);
      }

    } catch (Exception e) {
      System.out.println("Unable to open file");
      System.out.println("Error: " + e);
      System.exit(0);
    }
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