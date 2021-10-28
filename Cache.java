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
  Cache prevCache = null;
  Cache nextCache = null;

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
      this.lru_object = new BasicLRU(this.cache_assoc, this.sets);
      break;
    case 1:
      this.lru_object = new PseudoLRU(this.cache_assoc, this.sets);
      break;
    case 2:
      this.lru_object = new OptimalLRU(this.cache_assoc, this.sets, this.trace_list);
      break;
    default:
      System.out.println("Unsupported Replacement Policy, 0 for LRU, 1 for PLRU, 2 for Optimal.");
      System.exit(0);
    }

    // Initiates cache memory where we keep our records.
    this.cache_memory = new String[this.cache_assoc][this.sets];
  }

  public String checkCache(int trace_index, String op_code, String binary_address) {
    if (op_code == null) {
      op_code = this.trace_list[trace_index].op_code;
    }
    if (binary_address == null) {
      binary_address = this.trace_list[trace_index].binary_address;
    }

    // Get index and tag, we don't care for block offset in this project.
    String address_offset = binary_address.substring(31 - this.block_offset);
    String address_index = binary_address.substring(31 - (this.index_offset + this.block_offset),
        31 - this.block_offset);
    String address_tag = binary_address.substring(0, 31 - (this.index_offset + this.block_offset));

    // Converts binary address_index to an integer number since we will use it as an
    // index.
    int address_index_integer = Integer.parseInt(address_index, 2);
    int assoc_index;

    if (op_code.equals("r")) {
      this.read_hits++;
    } else if (op_code.equals("w")) {
      this.write_hits++;
    }

    for (assoc_index = 0; assoc_index < this.cache_assoc; assoc_index++) {
      if (cache_memory[assoc_index][address_index_integer] != null
          && cache_memory[assoc_index][address_index_integer].substring(1).compareTo(address_tag) == 0) {
        if (op_code.equals("w")) {
          // Writes to existing indexes makes it dirty
          cache_memory[assoc_index][address_index_integer] = "1" + address_tag;
        }
        // Every access must be registered in LRU policy.
        lru_object.cacheAccess(assoc_index, address_index_integer, trace_index, address_tag + address_index);
        // Hits do not cascade, so we end.
        return null;
      }
    }

    // Read/write miss cascade to lower cache as read.
    if (this.nextCache != null) {
      this.nextCache.checkCache(trace_index, "r", null);
    }

    if (op_code.equals("r")) {
      this.read_misses++;
    } else {
      this.write_misses++;
    }
    // Read and write misses will trigger a write.
    assoc_index = lru_object.getLRU(address_index_integer);
    lru_object.cacheAccess(assoc_index, address_index_integer, trace_index, address_tag + address_index);

    if (cache_memory[assoc_index][address_index_integer] != null) {
      // Check for dirty bit bit.
      char first_bit = cache_memory[assoc_index][address_index_integer].charAt(0);
      if (first_bit == '1') {
        // If bit is dirty, trigger write back
        if (this.nextCache != null) {
          String current_tag = cache_memory[assoc_index][address_index_integer].substring(1);
          this.nextCache.checkCache(trace_index, "w", current_tag + address_index + address_offset);
        }

        this.write_back++;
      }
    }

    // "dirty_indicator" + "tag"
    // Since we are doing an eviction, new bit is not dirty.
    if (op_code.equals("r")) {
      cache_memory[assoc_index][address_index_integer] = "0" + address_tag;
    } else {
      cache_memory[assoc_index][address_index_integer] = "1" + address_tag;
    }

    return null;
  }
}

// Basic LRU, used as parent so all other LRU types can inherit
class LRU {
  int[][] lru_list;
  int sets;
  int cache_assoc;

  LRU(int cache_assoc, int sets) {
    // Creates storage for hits
    this.lru_list = new int[cache_assoc][sets];
    this.sets = sets;
    this.cache_assoc = cache_assoc;
  }
}

interface LRUInterface {
  public void cacheAccess(int assoc_index, int address_index_integer, int traceIndex, String binary_address);

  public int getLRU(int address_index_integer);
}

// BasicLRU type, increments counter and find smallest one as the LRU.
class BasicLRU extends LRU implements LRUInterface {
  int counters;

  public BasicLRU(int cache_assoc, int sets) {
    super(cache_assoc, sets);
  }

  // When accessing block, assign counter value to set, making it largest.
  public void cacheAccess(int assoc_index, int address_index_integer, int traceIndex, String binary_address) {
    this.counters++;
    lru_list[assoc_index][address_index_integer] = this.counters;
  }

  // Sets LRU as the first set, then loop through set values looking for lowest
  // value.
  public int getLRU(int address_index_integer) {
    int lru_count = lru_list[0][address_index_integer];
    int lru_results = 0;
    for (int i = 1; i < super.cache_assoc; i++) {
      if (lru_list[i][address_index_integer] < lru_count) {
        lru_results = i;
      }
    }
    return lru_results;
  }
}

// PseudoLRU type, uses tree structure to find LRU.
class PseudoLRU extends LRU implements LRUInterface {

  public PseudoLRU(int cache_assoc, int sets) {
    // Reverts order or cache assoc and sets.
    super(sets, cache_assoc);
    // Initiates all values to 1, so when we look for free we look to left.
    for (int[] fill_array : super.lru_list) {
      Arrays.fill(fill_array, 1);
    }
  }

  // When accessing block, marks latest access to the set
  public void cacheAccess(int assoc_index, int address_index_integer, int traceIndex, String binary_address) {
    setCacheAccess(address_index_integer, 0, super.sets - 1, assoc_index);
  }

  public int getLRU(int address_index_integer) {
    return getPseudoLRU(address_index_integer, 0, super.sets - 1);
  }

  // Essentially a binary search setting the values to 0 and 1 as it finds the
  // address index.
  private void setCacheAccess(int address_index_integer, int left_index, int right_index, int assoc_index) {
    // Left and right index will be getting closer, eventually crossing when at end
    // of tree.
    if (right_index >= left_index) {
      int mid = left_index + (right_index - left_index) / 2;

      // If value is to left of mid index mark as 0, if to right mark as 1.
      // Recursive call to that branch of tree.
      if (assoc_index <= mid) {
        lru_list[address_index_integer][mid] = 0;
        setCacheAccess(address_index_integer, left_index, mid - 1, assoc_index);
      } else {
        lru_list[address_index_integer][mid] = 1;
        setCacheAccess(address_index_integer, mid + 1, right_index, assoc_index);
      }
    }
  }

  private int getPseudoLRU(int address_index_integer, int left_index, int right_index) {
    // Same logic as above, implemented as while loop.
    int mid = 0;
    // Right and left index are converging in this loop.
    while (right_index >= left_index) {
      // Pick mid point of array.
      mid = left_index + (right_index - left_index) / 2;
      // If value 1, go left, if 0, go right
      if (lru_list[address_index_integer][mid] == 1) {
        lru_list[address_index_integer][mid] = 0;
        right_index = mid - 1;
      } else {
        lru_list[address_index_integer][mid] = 1;
        left_index = mid + 1;
      }
    }
    // After converging, return value.
    int result = mid + lru_list[address_index_integer][mid];
    if (result >= sets) {
      result = sets - 1;
    }
    return result;
  }

}

// OptimalLRU type, Looks into future for LRU.
class OptimalLRU extends LRU implements LRUInterface {
  TraceObject trace_list[];

  public OptimalLRU(int cache_assoc, int sets, TraceObject trace_list[]) {
    super(cache_assoc, sets);
    this.trace_list = trace_list;
  }

  //
  public void cacheAccess(int assoc_index, int address_index_integer, int trace_index, String binary_address) {
    // We will look for 0 values as possibility of replacement, this is placeholder.
    lru_list[assoc_index][address_index_integer] = 0;
    trace_index++;
    // Loop through trace_list to find next occurance of address.
    for (int i = trace_index; i < this.trace_list.length; i++) {
      if (this.trace_list[i].binary_address.substring(0, binary_address.length()).compareTo(binary_address) == 0) {
        lru_list[assoc_index][address_index_integer] = i;
        break;
      }
    }
  }

  public int getLRU(int address_index_integer) {
    // Sets default to 0 for never accessed or empty blocks.
    int next_LRU = 0;
    // Finds block with access furthest in the future.
    for (int i = 0; i < this.cache_assoc; i++) {
      // System.out.println(lru_list[i][address_index_integer]);
      if (lru_list[i][address_index_integer] == 0) {
        return i;
      } else if (lru_list[i][address_index_integer] > lru_list[next_LRU][address_index_integer]) {
        next_LRU = i;
      }
    }
    return next_LRU;
  }
}
