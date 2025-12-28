package itf.com.app.lms.services;

import itf.com.app.lms.util.LogManager;

/**
 * LoggingService - Centralized logging service wrapper
 *
 * Responsibilities:
 * - Provide consistent logging interface
 * - Wrap LogManager for easier usage
 * - Add any additional logging features if needed
 *
 * Usage:
 * <pre>
 * LoggingService logger = new LoggingService();
 * logger.info(LogCategory.BT, "Message");
 * logger.error(LogCategory.ER, "Error message", exception);
 * </pre>
 */
public class LoggingService {

    private static final String TAG = "LoggingService";

    /**
     * Log info message
     */
    public void logInfo(LogManager.LogCategory category, String message) {
        LogManager.i(category, TAG, message);
    }

    /**
     * Log debug message
     */
    public void logDebug(LogManager.LogCategory category, String message) {
        LogManager.d(category, TAG, message);
    }

    /**
     * Log warning message
     */
    public void logWarn(LogManager.LogCategory category, String message) {
        LogManager.w(category, TAG, message);
    }

    /**
     * Log error message
     */
    public void logError(LogManager.LogCategory category, String message, Throwable throwable) {
        LogManager.e(category, TAG, message, throwable);
    }

    /**
     * Log error message without exception
     */
    public void logError(LogManager.LogCategory category, String message) {
        LogManager.e(category, TAG, message);
    }

    /**
     * Log Bluetooth test response
     */
    public void logBtTestResponse(String command, String response, double electricValue,
                                  String result, String checkValue) {
        // This can be customized based on your logging requirements
        logInfo(LogManager.LogCategory.BT,
                String.format("BT Test Response - Command: %s, Response: %s, Value: %.2f, Result: %s, Check: %s",
                        command, response, electricValue, result, checkValue));
    }

    /**
     * Log Bluetooth test response (simple version)
     */
    public void logBtTestResponseSimple(String command, String response, String result) {
        logInfo(LogManager.LogCategory.BT,
                String.format("BT Test - Command: %s, Response: %s, Result: %s",
                        command, response, result));
    }
}


