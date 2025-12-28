package itf.com.lms.logic.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple LRU cache for test results.
 */
public class TestResultCache {
    private static final int MAX_SIZE = 100;

    private final Map<String, TestResultModel> cache = new ConcurrentHashMap<>();
    private final LinkedHashMap<String, Boolean> lru = new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            if (size() > MAX_SIZE) {
                cache.remove(eldest.getKey());
                return true;
            }
            return false;
        }
    };

    private String key(String serialNo, String testItemCode) {
        return serialNo + "|" + testItemCode;
    }

    public void add(TestResultModel result) {
        if (result == null) return;
        String key = key(result.getSerialNo(), result.getTestItemCode());
        cache.put(key, result);
        synchronized (lru) {
            lru.put(key, Boolean.TRUE);
        }
    }

    public TestResultModel get(String serialNo, String testItemCode) {
        String k = key(serialNo, testItemCode);
        TestResultModel val = cache.get(k);
        if (val != null) {
            synchronized (lru) {
                lru.put(k, Boolean.TRUE);
            }
        }
        return val;
    }

    public void invalidate(String serialNo, String testItemCode) {
        String k = key(serialNo, testItemCode);
        cache.remove(k);
        synchronized (lru) {
            lru.remove(k);
        }
    }

    public void clear() {
        cache.clear();
        synchronized (lru) {
            lru.clear();
        }
    }
}

