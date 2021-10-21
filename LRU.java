// Basic LRU
public class LRU {
    class Basic_LRU {
        int[][] lruList;
        int sets;
        int counters;

        Basic_LRU(int cache_assoc, int sets) {
            // Creates storage for hits
            this.lruList = new int[cache_assoc][sets];
            this.sets = sets;
        }

        // When accessing block, set latest access to the set
        public void cacheAccess(int cache_assoc, int address_index_integer) {
            this.counters++;
            this.lruList[cache_assoc][address_index_integer] = this.counters;
        }

        public int getLRU(int cache_assoc) {
            int lruResult = this.lruList[cache_assoc][0];
            for (int i = 1; i < sets; i++) {
                if (this.lruList[cache_assoc][i] < lruResult) {
                    lruResult = i;
                }
            }
            return lruResult;
        }
    }
}
