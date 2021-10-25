import java.lang.Math;
import java.util.Arrays;

public class Cache {
  public int block_size;
  public int cache_size;
  public int cache_assoc;
  public int replacement_policy;
  public int inclusion_property;
  // Each instance of cache keeps track of its record
  public int read_hits = 0;
  public int read_misses = 0;
  public int write_hits = 0;
  public int write_misses = 0;
  public int write_back = 0;

  public int sets;
  public int block_offset;
  public int index_offset;

  public int trace_index;

  String[][] cache_memory;
  TraceObject trace_list[];
  LRUInterface lru_object;

  public Cache(int block_size, int cache_size, int cache_assoc, int replacement_policy, int inclusion_property,
      TraceObject trace_list[]) {
    this.block_size = block_size;
    this.cache_size = cache_size;
    this.cache_assoc = cache_assoc;
    this.replacement_policy = replacement_policy;
    this.inclusion_property = inclusion_property;
    this.trace_list = trace_list;

    // Calculate set size, block offset, and index offset. Tag is assumed to be
    // remaining bits.
    this.sets = cache_size / (block_size * cache_assoc);
    this.block_offset = (int) (Math.log(block_size) / Math.log(2));
    this.index_offset = (int) (Math.log(sets) / Math.log(2));

    // Check for set as power of 2
    if ((this.sets & this.sets - 1) != 0 || this.sets < 1) {
      System.out.println("Number of sets must be power of 2 and at least 1");
      System.exit(0);
    }

    // 0 for LRU, 1 for PLRU, 2 for Optimal.
    switch (replacement_policy) {
    case 0:
      this.lru_object = new BasicLRU(cache_assoc, sets);
      break;
    case 1:
      this.lru_object = new PseudoLRU(cache_assoc, sets);
      break;
    case 2:
      this.lru_object = new OptimalLRU(cache_assoc, sets, trace_list);
      break;
    default:
      System.out.println("Unsupported Replacement Policy, 0 for LRU, 1 for PLRU, 2 for Optimal.");
      System.exit(0);
    }

    // Initiates cache memory where we keep our records.
    this.cache_memory = new String[cache_assoc][sets];
  }

  public void checkCache(int trace_index, Cache next_cache) {
    String binary_address = this.trace_list[trace_index].binary_address;

    // Get index and tag.
    String address_index = binary_address.substring(31 - (this.index_offset + this.block_offset),
        31 - this.block_offset);
    String address_tag = binary_address.substring(0, 31 - (this.index_offset + this.block_offset));

    // Converts binary address_index to an integer number since we will use it as an
    // index.
    int address_index_integer = Integer.parseInt(address_index, 2);

    if (this.trace_list[trace_index].op_code.equals("r")) {
      readAddress(binary_address, address_index_integer, trace_index, address_tag, next_cache);
    } else {
      writeAddress(binary_address, address_index_integer, address_tag, next_cache);
    }
  }
  // INCLUSION_PROPERTY: Positive integer. 0 for non-inclusive, 1 for inclusive.
  private String readAddress(String binary_address, int address_index_integer, int trace_index, String address_tag,
      Cache next_cache) {

    // lru_object.getLRU(0);
    for (int assoc_index = 0; assoc_index < cache_assoc; assoc_index++) {
      if (cache_memory[assoc_index][address_index_integer] != null
          && cache_memory[assoc_index][address_index_integer].compareTo(address_tag) == 0) {
        // If found, update read hits and mark access in LRU.
        read_hits++;
        lru_object.cacheAccess(assoc_index, address_index_integer, trace_index, binary_address);
        return address_tag;
      }
    }
    read_misses++;
    // If there is a lower cache, check if there.
    if (next_cache != null) {
      // nextCache.checkCache("r", binaryAddress, null);
    }
    // *******
    // Call write address here, account for the fact it comes from a read miss.
    // *******
    return address_tag;
  }

  public void writeAddress(String binary_address, int address_index_integer, String address_tag, Cache next_cache) {
    for (int assoc_index = 0; assoc_index < cache_assoc; assoc_index++) {
      if (cache_memory[assoc_index][address_index_integer] != null
          && cache_memory[assoc_index][address_index_integer].compareTo(address_tag) == 0) {
        write_hits++;
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
    write_misses++;
  }
}

// Basic LRU, used as parent so all other LRU types can inherit
class LRU {
  int[][] lru_list;
  int sets;

  LRU(int cache_assoc, int sets) {
    // Creates storage for hits
    this.lru_list = new int[cache_assoc][sets];
    this.sets = sets;
  }
}

interface LRUInterface {
  public void cacheAccess(int cache_assoc, int address_index_integer, int traceIndex, String binary_address);

  public int getLRU(int cache_assoc);
}

// BasicLRU type, increments counter and find smallest one as the LRU.
class BasicLRU extends LRU implements LRUInterface {
  int counters;

  public BasicLRU(int cache_assoc, int sets) {
    super(cache_assoc, sets);
  }

  // When accessing block, assign counter value to set, making it largest.
  public void cacheAccess(int cache_assoc, int address_index_integer, int traceIndex, String binary_address) {
    this.counters++;
    lru_list[cache_assoc][address_index_integer] = this.counters;
  }

  // Sets LRU as the first set, then loop through set values looking for lowest
  // value.
  public int getLRU(int cache_assoc) {
    int lru_results = lru_list[cache_assoc][0];
    for (int i = 1; i < sets; i++) {
      if (lru_list[cache_assoc][i] < lru_results) {
        lru_results = i;
      }
    }
    return lru_results;
  }
}

// BasicLRU type, increments counter and find smallest one as the LRU.
class PseudoLRU extends LRU implements LRUInterface {
  int[][] pseudo_lru_tree;

  public PseudoLRU(int cache_assoc, int sets) {
    super(cache_assoc, sets);
    // Initiates all values to 1, so when we look for free we look to left.
    for (int[] fill_array : lru_list) {
      Arrays.fill(fill_array, 1);
    }
  }

  // When accessing block, set latest access to the set
  public void cacheAccess(int cache_assoc, int address_index_integer, int traceIndex, String binary_address) {
    setCacheAccess(cache_assoc, 0, this.sets - 1, address_index_integer);
    // System.out.println("cacheAccess " + Arrays.deepToString(lru_list));

  }

  public int getLRU(int cache_assoc) {
    int testor = getPseudoLRU(cache_assoc, 0, this.sets - 1);
    // System.out.println(testor + " getLRU " + Arrays.deepToString(lru_list));
    return testor;
  }

  // Essentially a binary search setting the values to 0 and 1 as it finds the
  // address index.
  private void setCacheAccess(int cache_assoc, int left_index, int right_index, int address_index_integer) {
    // Left and right index will be getting closer, eventually crossing when at end
    // of tree.
    if (right_index >= left_index) {
      int mid = left_index + (right_index - left_index) / 2;

      // If value is to left of mid index mark as 0, if to right mark as 1.
      // Recursive call to that branch of tree.
      if (address_index_integer <= mid) {
        lru_list[cache_assoc][mid] = 0;
        setCacheAccess(cache_assoc, left_index, mid - 1, address_index_integer);
      } else {
        lru_list[cache_assoc][mid] = 1;
        setCacheAccess(cache_assoc, mid + 1, right_index, address_index_integer);
      }
    }
  }

  private int getPseudoLRU(int cache_assoc, int left_index, int right_index) {
    // Same logic as above, implemented as while loop.
    int mid = 0;
    // Right and left index are converging in this loop.
    while (right_index >= left_index) {
      // Pick mid point of array.
      mid = left_index + (right_index - left_index) / 2;
      // If value 1, go left, if 0, go right
      if (lru_list[cache_assoc][mid] == 1) {
        lru_list[cache_assoc][mid] = 0;
        right_index = mid - 1;
      } else {
        lru_list[cache_assoc][mid] = 1;
        left_index = mid + 1;
      }
    }
    // After converging, return value.
    if (mid >= sets) {
      mid = sets - 1;
    }
    return mid + lru_list[cache_assoc][mid];
  }

}

// BasicLRU type, increments counter and find smallest one as the LRU.
class OptimalLRU extends LRU implements LRUInterface {
  TraceObject trace_list[];

  public OptimalLRU(int cache_assoc, int sets, TraceObject trace_list[]) {
    super(cache_assoc, sets);
    this.trace_list = trace_list;
  }

  //
  public void cacheAccess(int cache_assoc, int address_index_integer, int trace_index, String binary_address) {
    // We will look for 0 values as possibility of replacement, this is placeholder.
    lru_list[cache_assoc][address_index_integer] = 0;
    // Loop through trace_list to find next occurance of address.
    for (int i = trace_index; i < this.trace_list.length; i++) {
      if (this.trace_list[i].binary_address.compareTo(binary_address) == 0) {
        lru_list[cache_assoc][address_index_integer] = i;
        break;
      }
    }
  }

  public int getLRU(int cache_assoc) {
    int next_LRU = 0;
    for (int i = 0; i < lru_list[cache_assoc].length; i++) {
      if (lru_list[cache_assoc][i] == 0) {
        return i;
      } else if (lru_list[cache_assoc][i] > next_LRU) {
        next_LRU = lru_list[cache_assoc][i];
      }
    }
    return next_LRU;
  }
}