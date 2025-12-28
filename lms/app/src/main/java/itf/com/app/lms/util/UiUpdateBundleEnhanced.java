package itf.com.app.lms.util;

import itf.com.app.lms.item.ItemAdapterTestItem;

/**
 * UI 업데이트를 위한 데이터 번들 클래스 (개선 버전)
 * 
 * 기존 UiUpdateBundle에 merge 기능을 추가한 버전
 * 여러 UI 업데이트를 하나로 묶어서 처리할 수 있음
 */
public class UiUpdateBundleEnhanced {
    // 기존 UiUpdateBundle과 동일한 필드들
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

    private UiUpdateBundleEnhanced(Builder builder) {
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

    /**
     * 다른 UiUpdateBundle과 병합
     * 나중에 설정된 값이 우선순위를 가짐
     */
    public Builder merge(UiUpdateBundleEnhanced other) {
        Builder builder = new Builder();
        
        // 다른 번들의 값이 있으면 사용, 없으면 현재 값 사용
        builder.dialogVisible = other.dialogVisible || this.dialogVisible;
        builder.dialogHidden = other.dialogHidden || this.dialogHidden;
        builder.dialogColor = other.dialogColor != 0 ? other.dialogColor : this.dialogColor;
        builder.dialogMessage = other.dialogMessage != null ? other.dialogMessage : this.dialogMessage;
        builder.temperatureText = other.temperatureText != null ? other.temperatureText : this.temperatureText;
        builder.compWattText = other.compWattText != null ? other.compWattText : this.compWattText;
        builder.heaterWattText = other.heaterWattText != null ? other.heaterWattText : this.heaterWattText;
        builder.pumpWattText = other.pumpWattText != null ? other.pumpWattText : this.pumpWattText;
        builder.logText = other.logText != null ? other.logText : this.logText;
        builder.updateItemCommand = other.updateItemCommand != null && !other.updateItemCommand.isEmpty() 
            ? other.updateItemCommand : this.updateItemCommand;
        builder.updateItemResult = other.updateItemResult != null && !other.updateItemResult.isEmpty() 
            ? other.updateItemResult : this.updateItemResult;
        builder.updateItemCheckValue = other.updateItemCheckValue != null && !other.updateItemCheckValue.isEmpty() 
            ? other.updateItemCheckValue : this.updateItemCheckValue;
        builder.updateItemInfo = other.updateItemInfo != null && !other.updateItemInfo.isEmpty() 
            ? other.updateItemInfo : this.updateItemInfo;
        builder.updateItemNameSuffix = other.updateItemNameSuffix != null && !other.updateItemNameSuffix.isEmpty() 
            ? other.updateItemNameSuffix : this.updateItemNameSuffix;
        builder.updateListAdapter = other.updateListAdapter || this.updateListAdapter;
        builder.finalReceiveCommandResponse = other.finalReceiveCommandResponse != null 
            ? other.finalReceiveCommandResponse : this.finalReceiveCommandResponse;
        builder.finalCalculatedResultValue = other.finalCalculatedResultValue != null 
            ? other.finalCalculatedResultValue : this.finalCalculatedResultValue;
        builder.finalReadMessage = other.finalReadMessage != null 
            ? other.finalReadMessage : this.finalReadMessage;
        builder.temperatureValueCompDiff = other.temperatureValueCompDiff != null 
            ? other.temperatureValueCompDiff : this.temperatureValueCompDiff;
        builder.resultInfo = other.resultInfo != null ? other.resultInfo : this.resultInfo;
        builder.decTemperatureHotValue = other.decTemperatureHotValue != null 
            ? other.decTemperatureHotValue : this.decTemperatureHotValue;
        builder.decTemperatureColdValue = other.decTemperatureColdValue != null 
            ? other.decTemperatureColdValue : this.decTemperatureColdValue;
        builder.finalCurrentTestItem = other.finalCurrentTestItem != null 
            ? other.finalCurrentTestItem : this.finalCurrentTestItem;
        builder.testItemIdx = other.testItemIdx != 0 ? other.testItemIdx : this.testItemIdx;
        builder.testOkCnt = other.testOkCnt != 0 ? other.testOkCnt : this.testOkCnt;
        builder.testNgCnt = other.testNgCnt != 0 ? other.testNgCnt : this.testNgCnt;
        builder.receiveCommandResponseOK = other.receiveCommandResponseOK != null 
            ? other.receiveCommandResponseOK : this.receiveCommandResponseOK;
        builder.shouldUpdateCounts = other.shouldUpdateCounts || this.shouldUpdateCounts;
        builder.listItemAdapter = other.listItemAdapter != null ? other.listItemAdapter : this.listItemAdapter;
        builder.currentProcessName = other.currentProcessName != null 
            ? other.currentProcessName : this.currentProcessName;
        builder.receivedMessageCnt = other.receivedMessageCnt != 0 ? other.receivedMessageCnt : this.receivedMessageCnt;
        
        return builder;
    }

    /**
     * 기존 UiUpdateBundle로 변환 (호환성)
     */
    public UiUpdateBundle toUiUpdateBundle() {
        return new UiUpdateBundle.Builder()
            .setDialogVisible(dialogVisible)
            .setDialogHidden(dialogHidden)
            .setDialogColor(dialogColor)
            .setDialogMessage(dialogMessage)
            .setTemperatureText(temperatureText)
            .setCompWattText(compWattText)
            .setHeaterWattText(heaterWattText)
            .setPumpWattText(pumpWattText)
            .setLogText(logText)
            .setUpdateItemCommand(updateItemCommand)
            .setUpdateItemResult(updateItemResult)
            .setUpdateItemCheckValue(updateItemCheckValue)
            .setUpdateItemInfo(updateItemInfo)
            .setUpdateItemNameSuffix(updateItemNameSuffix)
            .setUpdateListAdapter(updateListAdapter)
            .setFinalReceiveCommandResponse(finalReceiveCommandResponse)
            .setFinalCalculatedResultValue(finalCalculatedResultValue)
            .setFinalReadMessage(finalReadMessage)
            .setTemperatureValueCompDiff(temperatureValueCompDiff)
            .setResultInfo(resultInfo)
            .setDecTemperatureHotValue(decTemperatureHotValue)
            .setDecTemperatureColdValue(decTemperatureColdValue)
            .setFinalCurrentTestItem(finalCurrentTestItem)
            .setTestItemIdx(testItemIdx)
            .setTestOkCnt(testOkCnt)
            .setTestNgCnt(testNgCnt)
            .setReceiveCommandResponseOK(receiveCommandResponseOK)
            .setShouldUpdateCounts(shouldUpdateCounts)
            .setListItemAdapter(listItemAdapter)
            .setCurrentProcessName(currentProcessName)
            .setReceivedMessageCnt(receivedMessageCnt)
            .build();
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

        // 기존 UiUpdateBundle.Builder와 동일한 setter 메서드들
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

        /**
         * 다른 Builder의 값을 현재 Builder에 병합
         */
        public Builder mergeFrom(UiUpdateBundleEnhanced other) {
            if (other.dialogVisible) this.dialogVisible = true;
            if (other.dialogHidden) this.dialogHidden = true;
            if (other.dialogColor != 0) this.dialogColor = other.dialogColor;
            if (other.dialogMessage != null) this.dialogMessage = other.dialogMessage;
            if (other.temperatureText != null) this.temperatureText = other.temperatureText;
            if (other.compWattText != null) this.compWattText = other.compWattText;
            if (other.heaterWattText != null) this.heaterWattText = other.heaterWattText;
            if (other.pumpWattText != null) this.pumpWattText = other.pumpWattText;
            if (other.logText != null) this.logText = other.logText;
            if (other.updateItemCommand != null && !other.updateItemCommand.isEmpty()) 
                this.updateItemCommand = other.updateItemCommand;
            if (other.updateItemResult != null && !other.updateItemResult.isEmpty()) 
                this.updateItemResult = other.updateItemResult;
            if (other.updateItemCheckValue != null && !other.updateItemCheckValue.isEmpty()) 
                this.updateItemCheckValue = other.updateItemCheckValue;
            if (other.updateItemInfo != null && !other.updateItemInfo.isEmpty()) 
                this.updateItemInfo = other.updateItemInfo;
            if (other.updateItemNameSuffix != null && !other.updateItemNameSuffix.isEmpty()) 
                this.updateItemNameSuffix = other.updateItemNameSuffix;
            if (other.updateListAdapter) this.updateListAdapter = true;
            if (other.finalReceiveCommandResponse != null) 
                this.finalReceiveCommandResponse = other.finalReceiveCommandResponse;
            if (other.finalCalculatedResultValue != null) 
                this.finalCalculatedResultValue = other.finalCalculatedResultValue;
            if (other.finalReadMessage != null) this.finalReadMessage = other.finalReadMessage;
            if (other.temperatureValueCompDiff != null) 
                this.temperatureValueCompDiff = other.temperatureValueCompDiff;
            if (other.resultInfo != null) this.resultInfo = other.resultInfo;
            if (other.decTemperatureHotValue != null) 
                this.decTemperatureHotValue = other.decTemperatureHotValue;
            if (other.decTemperatureColdValue != null) 
                this.decTemperatureColdValue = other.decTemperatureColdValue;
            if (other.finalCurrentTestItem != null) 
                this.finalCurrentTestItem = other.finalCurrentTestItem;
            if (other.testItemIdx != 0) this.testItemIdx = other.testItemIdx;
            if (other.testOkCnt != 0) this.testOkCnt = other.testOkCnt;
            if (other.testNgCnt != 0) this.testNgCnt = other.testNgCnt;
            if (other.receiveCommandResponseOK != null) 
                this.receiveCommandResponseOK = other.receiveCommandResponseOK;
            if (other.shouldUpdateCounts) this.shouldUpdateCounts = true;
            if (other.listItemAdapter != null) this.listItemAdapter = other.listItemAdapter;
            if (other.currentProcessName != null) this.currentProcessName = other.currentProcessName;
            if (other.receivedMessageCnt != 0) this.receivedMessageCnt = other.receivedMessageCnt;
            return this;
        }

        public UiUpdateBundleEnhanced build() {
            return new UiUpdateBundleEnhanced(this);
        }
    }
}

