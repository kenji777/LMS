package itf.com.lms.logic.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import itf.com.lms.util.TestData;

/**
 * Wraps TestData operations with async handling and caching.
 * Skeleton implementation for Phase 2 integration.
 */
public class DataRepository {

    private static final String TAG = "DataRepository";

    private final Context appContext;
    private final ExecutorService dbExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, List<TestSpec>> specCache = new ConcurrentHashMap<>();

    public DataRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DataRepository");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    public interface DataListener<T> {
        void onSuccess(T data);

        void onError(DatabaseError error, String message);
    }

    public void getTestSpec(String modelId, DataListener<List<TestSpec>> listener) {
        if (specCache.containsKey(modelId)) {
            mainHandler.post(() -> listener.onSuccess(specCache.get(modelId)));
            return;
        }

        dbExecutor.execute(() -> {
            try {
                String condition = (modelId == null || modelId.isEmpty()) ? "" : " AND clm_model_id='" + modelId + "'";
                List<Map<String, String>> rawData = TestData.selectTestSpecData(appContext, condition);
                List<TestSpec> specs = TestSpecMapper.map(rawData);
                specCache.put(modelId, specs);
                mainHandler.post(() -> listener.onSuccess(specs));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(DatabaseError.QUERY_FAILED, e.getMessage()));
            }
        });
    }

    public void saveTestResult(TestResultModel result, DataListener<Long> listener) {
        dbExecutor.execute(() -> {
            try {
                // Placeholder: use TestData.insertTestResult when available
                long rowId = 0;
                mainHandler.post(() -> listener.onSuccess(rowId));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(DatabaseError.INSERT_FAILED, e.getMessage()));
            }
        });
    }

    public void getTestHistory(String filter, DataListener<List<TestHistory>> listener) {
        dbExecutor.execute(() -> {
            try {
                // Placeholder mapping
                mainHandler.post(() -> listener.onSuccess(Collections.emptyList()));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(DatabaseError.QUERY_FAILED, e.getMessage()));
            }
        });
    }

    public void updateTestResult(String serialNo, String testItemCode, Map<String, String> updates, DataListener<Integer> listener) {
        dbExecutor.execute(() -> {
            try {
                // Placeholder: update count
                int count = 0;
                mainHandler.post(() -> listener.onSuccess(count));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(DatabaseError.UPDATE_FAILED, e.getMessage()));
            }
        });
    }

    public void clearCache() {
        specCache.clear();
    }

    public void cleanup() {
        clearCache();
        dbExecutor.shutdownNow();
        Log.i(TAG, "DataRepository cleaned up");
    }

    /**
     * Simple mapper from raw DB rows to TestSpec models.
     */
    private static class TestSpecMapper {
        static List<TestSpec> map(List<Map<String, String>> raw) {
            if (raw == null) return Collections.emptyList();
            List<TestSpec> list = new java.util.ArrayList<>();
            for (Map<String, String> row : raw) {
                list.add(toSpec(row));
            }
            return list;
        }

        private static TestSpec toSpec(Map<String, String> row) {
            if (row == null) row = new HashMap<>();
            double lower = parseDouble(row.get("lower_limit"));
            double upper = parseDouble(row.get("upper_limit"));
            long timeout = parseLong(row.get("timeout_ms"));
            return TestSpec.builder()
                    .testItemCode(row.getOrDefault("test_item_code", ""))
                    .testItemName(row.getOrDefault("test_item_name", ""))
                    .lowerLimit(lower)
                    .upperLimit(upper)
                    .unit(row.getOrDefault("unit", ""))
                    .timeout(timeout)
                    .build();
        }

        private static double parseDouble(String v) {
            try {
                return Double.parseDouble(v);
            } catch (Exception e) {
                return 0;
            }
        }

        private static long parseLong(String v) {
            try {
                return Long.parseLong(v);
            } catch (Exception e) {
                return 0;
            }
        }
    }
}

