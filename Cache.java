public class Cache {
  public int block_size;
  public int cache_size;
  public int cache_assoc;
  public int replacement_policy;
  public int inclusion_property;
  // Each instance of cache keeps track of its record
  public int readHits = 0;
  public int readMisses = 0;
  public int writeHits = 0;
  public int writeMisses = 0;
  public int writeBack = 0;

  public int sets;
  public int block_offset;
  public int index;

  String[][] cache_memory;

  public Cache(int block_size, int cache_size, int cache_assoc, int replacement_policy, int inclusion_property) {
    this.block_size = block_size;
    this.cache_size = cache_size;
    this.cache_assoc = cache_assoc;
    this.replacement_policy = replacement_policy;
    this.inclusion_property = inclusion_property;
    // Calculate set size, block offset, and index. Tag is assumed to be remaining
    // bits.

    this.sets = cache_size / (block_size * cache_assoc);
    this.block_offset = (int) (Math.log(block_size) / Math.log(2));
    this.index = (int) (Math.log(sets) / Math.log(2));

    // Initiates cache memory where we keep our records.
    this.cache_memory = new String[cache_assoc][sets];

  }

  public void checkCache(int traceIndex, TraceObject[] traceList, Cache nextCache) {
    String opCode = traceList[traceIndex].opCode;
    String binaryAddress = traceList[traceIndex].binaryAddress;
    // String address_block_offset = binaryAddress.substring(31 - block_offset);
    String address_index = binaryAddress.substring(31 - (index + block_offset), 31 - block_offset);
    String address_tag = binaryAddress.substring(0, 31 - (index + block_offset));
    // Converts binary address_index to an integer number since we will use it as an
    // index.
    int address_index_integer = Integer.parseInt(address_index, 2);

    if (opCode.equals("r")) {
      readAddress(binaryAddress, address_index_integer, address_tag, nextCache);
    } else {
      writeAddress(binaryAddress, address_index_integer, address_tag, nextCache, opCode);
    }
  }

  public String readAddress(String binaryAddress, int address_index_integer, String address_tag, Cache nextCache) {
    for (int assoc_index = 0; assoc_index < cache_assoc; assoc_index++) {
      if (cache_memory[assoc_index][address_index_integer].compareTo(address_tag) == 0) {
        readHits++;
        // *******
        // ACCOUNT FOR LAST ACCESSED ITEM HERE
        // *******
        return address_tag;
      }
    }
    readMisses++;
    // If there is a lower cache, check if there.
    if (nextCache != null) {
      // nextCache.checkCache("r", binaryAddress, null);
    }
    // *******
    // Call write address here, account for the fact it comes from a read miss.
    // *******
    return address_tag;
  }

  public void writeAddress(String binaryAddress, int address_index_integer, String address_tag, Cache nextCache,
      String opCode) {
    for (int assoc_index = 0; assoc_index < cache_assoc; assoc_index++) {
      if (cache_memory[assoc_index][address_index_integer].compareTo(address_tag) == 0) {
        writeHits++;
        // *******
        // ACCOUNT FOR LAST ACCESSED ITEM HERE
        // *******
        return;
      } else if (cache_memory[assoc_index][address_index_integer] == null) {
        // *******
        // Case where there is empty space, just write.
        // *******
        return;
      }
    }
    writeMisses++;

  }

}
