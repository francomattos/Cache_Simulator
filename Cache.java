public class Cache {
  public int block_size;
  public int cache_size;
  public int cache_assoc;
  public int replacement_policy;
  public int inclusion_property;

  public int readHits = 0;
  public int readMisses = 0;
  public int writeHits = 0;
  public int writeMisses = 0;
  public int writeBack = 0;

  public int sets;
  public int block_offset;
  public int index;

  StringBuilder[][] cache_memory;
  StringBuilder[] address_list;

  public Cache(int block_size, int cache_size, int cache_assoc, int replacement_policy, int inclusion_property) {
    this.block_size = block_size;
    this.cache_size = cache_size;
    this.cache_assoc = cache_assoc;
    this.replacement_policy = replacement_policy;
    this.inclusion_property = inclusion_property;

    this.sets = cache_size / (block_size * cache_assoc);
    this.block_offset = (int) (Math.log(block_size) / Math.log(2));
    this.index = (int) (Math.log(sets) / Math.log(2));

    this.cache_memory = new StringBuilder[sets][cache_assoc];
    this.address_list = new StringBuilder[sets];
  }

  public void checkCache(String opCode, String binaryAddress, Cache nextCache) {
    String address_block_offset = binaryAddress.substring(31 - block_offset);
    String address_index = binaryAddress.substring(31 - (index + block_offset), 31 - block_offset);
    String address_tag = binaryAddress.substring(0, 31 - (index + block_offset));
    System.out.println("Address: " + binaryAddress);
    System.out.println("Block Offset: " + address_block_offset);
    System.out.println("Index: " + address_index);
    System.out.println("Tag: " + address_tag);

  }

}
