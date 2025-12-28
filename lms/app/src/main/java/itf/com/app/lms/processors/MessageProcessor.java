package itf.com.app.lms.processors;

import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.LogManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * MessageProcessor - Handles message parsing and processing
 *
 * Responsibilities:
 * - Bluetooth message parsing
 * - Message validation
 * - Message data extraction
 * - Response generation
 *
 * Usage:
 * <pre>
 * MessageProcessor processor = new MessageProcessor();
 * processor.processBtMessage(rawBytes);
 * </pre>
 */
public class MessageProcessor {

    private static final String TAG = "MessageProcessor";

    /**
     * Listener interface for message processing events
     */
    public interface MessageProcessorListener {
        /**
         * Called when a message is successfully parsed
         */
        void onMessageParsed(String command, String response, byte[] rawData);

        /**
         * Called when message parsing fails
         */
        void onMessageParseError(String error, byte[] rawData);

        /**
         * Called when message data is extracted
         */
        void onMessageDataExtracted(String command, String data);
    }

    private MessageProcessorListener listener;

    /**
     * Constructor
     */
    public MessageProcessor() {
    }

    /**
     * Set message processor listener
     */
    public void setMessageProcessorListener(MessageProcessorListener listener) {
        this.listener = listener;
    }

    /**
     * Process Bluetooth message
     */
    public ProcessResult processBtMessage(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return new ProcessResult(false, null, null, null);
        }

        try {
            // Convert bytes to string
            String message = new String(raw, StandardCharsets.UTF_8);
            
            // Extract command and response
            String command = extractCommand(message);
            String response = extractResponse(message);
            
            if (command != null || response != null) {
                if (listener != null) {
                    listener.onMessageParsed(command, response, raw);
                }
                return new ProcessResult(true, command, response, message);
            } else {
                if (listener != null) {
                    listener.onMessageParseError("Failed to extract command or response", raw);
                }
                return new ProcessResult(false, null, null, message);
            }
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error processing BT message", e);
            if (listener != null) {
                listener.onMessageParseError(e.getMessage(), raw);
            }
            return new ProcessResult(false, null, null, null);
        }
    }

    /**
     * Extract command from message
     */
    private String extractCommand(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        // Look for command patterns (e.g., ST0101, CM0101, etc.)
        // Check common test item codes
        String[] commonCodes = {
            Constants.TestItemCodes.ST0101,
            Constants.TestItemCodes.CM0101,
            Constants.TestItemCodes.CM0102,
            Constants.TestItemCodes.HT0101,
            Constants.TestItemCodes.PM0101,
            Constants.TestItemCodes.SV0101,
            Constants.TestItemCodes.SV0201,
            Constants.TestItemCodes.SV0301,
            Constants.TestItemCodes.SV0401,
            Constants.TestItemCodes.TH0101,
            Constants.TestItemCodes.TH0201
        };
        
        for (String testCode : commonCodes) {
            if (testCode != null && message.contains(testCode)) {
                return testCode;
            }
        }

        return null;
    }

    /**
     * Extract response from message
     */
    private String extractResponse(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        // Extract response data based on message format
        // This is a simplified version - actual implementation may vary
        if (message.contains(Constants.CharCodes.STX) && message.contains(Constants.CharCodes.ETX)) {
            int stxIndex = message.indexOf(Constants.CharCodes.STX);
            int etxIndex = message.indexOf(Constants.CharCodes.ETX);
            if (stxIndex >= 0 && etxIndex > stxIndex) {
                return message.substring(stxIndex + 1, etxIndex);
            }
        }

        return message;
    }

    /**
     * Validate message format
     */
    public boolean validateMessage(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return false;
        }

        try {
            String message = new String(raw, StandardCharsets.UTF_8);
            // Basic validation - check for STX/ETX or other markers
            return message.contains(Constants.CharCodes.STX) || 
                   message.contains(Constants.CharCodes.ETX) ||
                   message.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract numeric value from message
     */
    public Double extractNumericValue(String message, String pattern) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        try {
            // Extract numeric value based on pattern
            // This is a simplified version - actual implementation may vary
            String numericPart = message.replaceAll("[^0-9.]", "");
            if (!numericPart.isEmpty()) {
                return Double.parseDouble(numericPart);
            }
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error extracting numeric value", e);
        }

        return null;
    }

    /**
     * Process result class
     */
    public static class ProcessResult {
        private final boolean success;
        private final String command;
        private final String response;
        private final String rawMessage;

        public ProcessResult(boolean success, String command, String response, String rawMessage) {
            this.success = success;
            this.command = command;
            this.response = response;
            this.rawMessage = rawMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCommand() {
            return command;
        }

        public String getResponse() {
            return response;
        }

        public String getRawMessage() {
            return rawMessage;
        }
    }
}

