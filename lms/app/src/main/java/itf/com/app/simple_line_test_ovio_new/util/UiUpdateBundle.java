package itf.com.app.simple_line_test_ovio_new.util;

import itf.com.app.simple_line_test_ovio_new.item.ItemAdapterTestItem;

/**
 * UI 업데이트를 위한 데이터 번들 클래스
 * Builder 패턴을 사용하여 복잡한 UI 업데이트 데이터를 구성합니다.
 */
public class UiUpdateBundle {
    public final boolean dialogVisible;
    public final boolean dialogHidden;
    public final int dialogColor;
    public final String dialogMessage;
    public final String temperatureText;
    public final String compWattText;
    public final String heaterWattText;
    public final String pumpWattText;
    public final String logText;
    public final String updateItemCommand;
    public final String updateItemResult;
    public final String updateItemCheckValue;
    public final String updateItemInfo;
    public final String updateItemNameSuffix;
    public final boolean updateListAdapter;
    public final String finalReceiveCommandResponse;
    public final String finalCalculatedResultValue;
    public final String finalReadMessage;
    public final String temperatureValueCompDiff;
    public final String resultInfo;
    public final String decTemperatureHotValue;
    public final String decTemperatureColdValue;
    public final String finalCurrentTestItem;
    public final int testItemIdx;
    public final int testOkCnt;
    public final int testNgCnt;
    public final String receiveCommandResponseOK;
    public final boolean shouldUpdateCounts;
    public final ItemAdapterTestItem listItemAdapter;
    public final String currentProcessName;
    public final int receivedMessageCnt;

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

    public static class Builder {
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

        public Builder setDialogVisible(boolean value) {
            this.dialogVisible = value;
            return this;
        }

        public Builder setDialogHidden(boolean value) {
            this.dialogHidden = value;
            return this;
        }

        public Builder setDialogColor(int value) {
            this.dialogColor = value;
            return this;
        }

        public Builder setDialogMessage(String value) {
            this.dialogMessage = value;
            return this;
        }

        public Builder setTemperatureText(String value) {
            this.temperatureText = value;
            return this;
        }

        public Builder setCompWattText(String value) {
            this.compWattText = value;
            return this;
        }

        public Builder setHeaterWattText(String value) {
            this.heaterWattText = value;
            return this;
        }

        public Builder setPumpWattText(String value) {
            this.pumpWattText = value;
            return this;
        }

        public Builder setLogText(String value) {
            this.logText = value;
            return this;
        }

        public Builder setUpdateItemCommand(String value) {
            this.updateItemCommand = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        public Builder setUpdateItemResult(String value) {
            this.updateItemResult = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        public Builder setUpdateItemCheckValue(String value) {
            this.updateItemCheckValue = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        public Builder setUpdateItemInfo(String value) {
            this.updateItemInfo = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        public Builder setUpdateItemNameSuffix(String value) {
            this.updateItemNameSuffix = value == null ? Constants.Common.EMPTY_STRING : value;
            return this;
        }

        public Builder setUpdateListAdapter(boolean value) {
            this.updateListAdapter = value;
            return this;
        }

        public Builder setFinalReceiveCommandResponse(String value) {
            this.finalReceiveCommandResponse = value;
            return this;
        }

        public Builder setFinalCalculatedResultValue(String value) {
            this.finalCalculatedResultValue = value;
            return this;
        }

        public Builder setFinalReadMessage(String value) {
            this.finalReadMessage = value;
            return this;
        }

        public Builder setTemperatureValueCompDiff(String value) {
            this.temperatureValueCompDiff = value;
            return this;
        }

        public Builder setResultInfo(String value) {
            this.resultInfo = value;
            return this;
        }

        public Builder setDecTemperatureHotValue(String value) {
            this.decTemperatureHotValue = value;
            return this;
        }

        public Builder setDecTemperatureColdValue(String value) {
            this.decTemperatureColdValue = value;
            return this;
        }

        public Builder setFinalCurrentTestItem(String value) {
            this.finalCurrentTestItem = value;
            return this;
        }

        public Builder setTestItemIdx(int value) {
            this.testItemIdx = value;
            return this;
        }

        public Builder setTestOkCnt(int value) {
            this.testOkCnt = value;
            return this;
        }

        public Builder setTestNgCnt(int value) {
            this.testNgCnt = value;
            return this;
        }

        public Builder setReceiveCommandResponseOK(String value) {
            this.receiveCommandResponseOK = value;
            return this;
        }

        public Builder setShouldUpdateCounts(boolean value) {
            this.shouldUpdateCounts = value;
            return this;
        }

        public Builder setListItemAdapter(ItemAdapterTestItem adapter) {
            this.listItemAdapter = adapter;
            return this;
        }

        public Builder setCurrentProcessName(String value) {
            this.currentProcessName = value;
            return this;
        }

        public Builder setReceivedMessageCnt(int value) {
            this.receivedMessageCnt = value;
            return this;
        }

        public UiUpdateBundle build() {
            return new UiUpdateBundle(this);
        }
    }
}

