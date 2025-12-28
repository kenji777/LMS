package itf.com.app.simple_line_test_ovio_new.vo;


import itf.com.app.simple_line_test_ovio_new.item.ItemAdapterTestItem;
import itf.com.app.simple_line_test_ovio_new.util.Constants;

public class UiUpdateBundle {
    final boolean dialogVisible;
    final boolean dialogHidden;
    final int dialogColor;
    final String dialogMessage;
    final String temperatureText;
    final String compWattText;
    final String heaterWattText;
    final String pumpWattText;
    final String logText;
    final String updateItemCommand;
    final String updateItemResult;
    final String updateItemCheckValue;
    final String updateItemInfo;
    final String updateItemNameSuffix;
    final boolean updateListAdapter;
    final String finalReceiveCommandResponse;
    final String finalCalculatedResultValue;
    final String finalReadMessage;
    final String temperatureValueCompDiff;
    final String resultInfo;
    final String decTemperatureHotValue;
    final String decTemperatureColdValue;
    final String finalCurrentTestItem;
    final int testItemIdx;
    final int testOkCnt;
    final int testNgCnt;
    final String receiveCommandResponseOK;
    final boolean shouldUpdateCounts;
    final ItemAdapterTestItem listItemAdapter;
    final String currentProcessName;
    final int receivedMessageCnt;

    private UiUpdateBundle(Builder builder) {
        this.dialogVisible = builder.dialogVisible;
        this.dialogHidden = builder.dialogHidden;
        this.dialogColor = builder.dialogColor;
        this.dialogMessage = builder.dialogMessage;
        this.temperatureText = builder.temperatureText;
        this.compWattText = builder.compWattText;
        this.heaterWattText = builder.heaterWattText;
        this.pumpWattText = builder.pumpWattText;
        this.logText = builder.logText;
        this.updateItemCommand = builder.updateItemCommand;
        this.updateItemResult = builder.updateItemResult;
        this.updateItemCheckValue = builder.updateItemCheckValue;
        this.updateItemInfo = builder.updateItemInfo;
        this.updateItemNameSuffix = builder.updateItemNameSuffix;
        this.updateListAdapter = builder.updateListAdapter;
        this.finalReceiveCommandResponse = builder.finalReceiveCommandResponse;
        this.finalCalculatedResultValue = builder.finalCalculatedResultValue;
        this.finalReadMessage = builder.finalReadMessage;
        this.temperatureValueCompDiff = builder.temperatureValueCompDiff;
        this.resultInfo = builder.resultInfo;
        this.decTemperatureHotValue = builder.decTemperatureHotValue;
        this.decTemperatureColdValue = builder.decTemperatureColdValue;
        this.finalCurrentTestItem = builder.finalCurrentTestItem;
        this.testItemIdx = builder.testItemIdx;
        this.testOkCnt = builder.testOkCnt;
        this.testNgCnt = builder.testNgCnt;
        this.receiveCommandResponseOK = builder.receiveCommandResponseOK;
        this.shouldUpdateCounts = builder.shouldUpdateCounts;
        this.listItemAdapter = builder.listItemAdapter;
        this.currentProcessName = builder.currentProcessName;
        this.receivedMessageCnt = builder.receivedMessageCnt;
    }

    static class Builder {
        private boolean dialogVisible;
        private boolean dialogHidden;
        private int dialogColor;
        private String dialogMessage;
        private String temperatureText;
        private String compWattText;
        private String heaterWattText;
        private String pumpWattText;
        private String logText;
        private String updateItemCommand = Constants.Common.EMPTY_STRING;
        private String updateItemResult = Constants.Common.EMPTY_STRING;
        private String updateItemCheckValue = Constants.Common.EMPTY_STRING;
        private String updateItemInfo = Constants.Common.EMPTY_STRING;
        private String updateItemNameSuffix = Constants.Common.EMPTY_STRING;
        private boolean updateListAdapter;
        private String finalReceiveCommandResponse;
        private String finalCalculatedResultValue;
        private String finalReadMessage;
        private String temperatureValueCompDiff;
        private String resultInfo;
        private String decTemperatureHotValue;
        private String decTemperatureColdValue;
        private String finalCurrentTestItem;
        private int testItemIdx;
        private int testOkCnt;
        private int testNgCnt;
        private String receiveCommandResponseOK;
        private boolean shouldUpdateCounts;
        private ItemAdapterTestItem listItemAdapter;
        private String currentProcessName;
        private int receivedMessageCnt;

        Builder setDialogVisible(boolean value) {
            this.dialogVisible = value;
            return this;
        }

        Builder setDialogHidden(boolean value) {
            this.dialogHidden = value;
            return this;
        }

        Builder setDialogColor(int value) {
            this.dialogColor = value;
            return this;
        }

        Builder setDialogMessage(String value) {
            this.dialogMessage = value;
            return this;
        }

        Builder setTemperatureText(String value) {
            this.temperatureText = value;
            return this;
        }

        Builder setCompWattText(String value) {
            this.compWattText = value;
            return this;
        }

        Builder setHeaterWattText(String value) {
            this.heaterWattText = value;
            return this;
        }

        Builder setPumpWattText(String value) {
            this.pumpWattText = value;
            return this;
        }

        Builder setLogText(String value) {
            this.logText = value;
            return this;
        }

        Builder setUpdateItemCommand(String value) {
            this.updateItemCommand = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        Builder setUpdateItemResult(String value) {
            this.updateItemResult = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        Builder setUpdateItemCheckValue(String value) {
            this.updateItemCheckValue = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        Builder setUpdateItemInfo(String value) {
            this.updateItemInfo = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        Builder setUpdateItemNameSuffix(String value) {
            this.updateItemNameSuffix = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        Builder setUpdateListAdapter(boolean value) {
            this.updateListAdapter = value;
            return this;
        }

        Builder setFinalReceiveCommandResponse(String value) {
            this.finalReceiveCommandResponse = value;
            return this;
        }

        Builder setFinalCalculatedResultValue(String value) {
            this.finalCalculatedResultValue = value;
            return this;
        }

        Builder setFinalReadMessage(String value) {
            this.finalReadMessage = value;
            return this;
        }

        Builder setTemperatureValueCompDiff(String value) {
            this.temperatureValueCompDiff = value;
            return this;
        }

        Builder setResultInfo(String value) {
            this.resultInfo = value;
            return this;
        }

        Builder setDecTemperatureHotValue(String value) {
            this.decTemperatureHotValue = value;
            return this;
        }

        Builder setDecTemperatureColdValue(String value) {
            this.decTemperatureColdValue = value;
            return this;
        }

        Builder setFinalCurrentTestItem(String value) {
            this.finalCurrentTestItem = value;
            return this;
        }

        Builder setTestItemIdx(int value) {
            this.testItemIdx = value;
            return this;
        }

        Builder setTestOkCnt(int value) {
            this.testOkCnt = value;
            return this;
        }

        Builder setTestNgCnt(int value) {
            this.testNgCnt = value;
            return this;
        }

        Builder setReceiveCommandResponseOK(String value) {
            this.receiveCommandResponseOK = value;
            return this;
        }

        Builder setShouldUpdateCounts(boolean value) {
            this.shouldUpdateCounts = value;
            return this;
        }

        Builder setListItemAdapter(ItemAdapterTestItem adapter) {
            this.listItemAdapter = adapter;
            return this;
        }

        Builder setCurrentProcessName(String value) {
            this.currentProcessName = value;
            return this;
        }

        Builder setReceivedMessageCnt(int value) {
            this.receivedMessageCnt = value;
            return this;
        }

        UiUpdateBundle build() {
            return new UiUpdateBundle(this);
        }
    }
}