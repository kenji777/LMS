package itf.com.app.lms.util;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.text.TextUtils;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import itf.com.app.lms.item.ItemAdapterTestItem;
import itf.com.app.lms.vo.VoTestItem;

public class UiBundleApplier {
    public interface TestCountUpdater {
        void onCountsUpdated(int okCount, int ngCount);
    }

    public interface ControlModeUpdateHandler {
        void onItemUpdated(UiUpdateBundle bundle);
    }

    private final ConstraintLayout clDialogForPreprocess;
    private final TextView tvDialogMessage;
    private final TableRow trPreprocessContent;
    private final TextView tvCurrentProcess;
    private final TextView tvTemperature;
    private final TextView tvCompWattValue;
    private final TextView tvPopupProcessResultCompValue;
    private final TextView tvHeaterWattValue;
    private final TextView tvPopupProcessResultHeaterValue;
    private final TextView tvPumpWattValue;
    private final TextView tvLog;
    private final TextView tvTestOkCnt;
    private final TextView tvTestNgCnt;
    private final TestCountUpdater testCountUpdater;

    public UiBundleApplier(
            ConstraintLayout clDialogForPreprocess,
            TextView tvDialogMessage,
            TableRow trPreprocessContent,
            TextView tvCurrentProcess,
            TextView tvTemperature,
            TextView tvCompWattValue,
            TextView tvPopupProcessResultCompValue,
            TextView tvHeaterWattValue,
            TextView tvPopupProcessResultHeaterValue,
            TextView tvPumpWattValue,
            TextView tvLog,
            TextView tvTestOkCnt,
            TextView tvTestNgCnt,
            TestCountUpdater testCountUpdater
    ) {
        this.clDialogForPreprocess = clDialogForPreprocess;
        this.tvDialogMessage = tvDialogMessage;
        this.trPreprocessContent = trPreprocessContent;
        this.tvCurrentProcess = tvCurrentProcess;
        this.tvTemperature = tvTemperature;
        this.tvCompWattValue = tvCompWattValue;
        this.tvPopupProcessResultCompValue = tvPopupProcessResultCompValue;
        this.tvHeaterWattValue = tvHeaterWattValue;
        this.tvPopupProcessResultHeaterValue = tvPopupProcessResultHeaterValue;
        this.tvPumpWattValue = tvPumpWattValue;
        this.tvLog = tvLog;
        this.tvTestOkCnt = tvTestOkCnt;
        this.tvTestNgCnt = tvTestNgCnt;
        this.testCountUpdater = testCountUpdater;
    }

    public void apply(UiUpdateBundle bundle, ControlModeUpdateHandler controlModeUpdateHandler) {
        if (bundle == null) {
            return;
        }

        if (bundle.dialogVisible) {
            if (clDialogForPreprocess.getVisibility() != VISIBLE) {
                clDialogForPreprocess.setVisibility(VISIBLE);
            }
            tvDialogMessage.setText(bundle.dialogMessage);
            trPreprocessContent.setBackgroundColor(bundle.dialogColor);
        } else if (bundle.dialogHidden) {
            clDialogForPreprocess.setVisibility(INVISIBLE);
            tvDialogMessage.setText(Constants.Common.EMPTY_STRING);
        }

        if (!TextUtils.isEmpty(bundle.currentProcessName)) {
            tvCurrentProcess.setText(bundle.currentProcessName);
        }

        if (!TextUtils.isEmpty(bundle.temperatureText)) {
            tvTemperature.setText(bundle.temperatureText);
        }

        if (!TextUtils.isEmpty(bundle.compWattText)) {
            tvCompWattValue.setText(bundle.compWattText);
            tvPopupProcessResultCompValue.setText(bundle.compWattText);
        }

        if (!TextUtils.isEmpty(bundle.heaterWattText)) {
            tvHeaterWattValue.setText(bundle.heaterWattText);
            tvPopupProcessResultHeaterValue.setText(bundle.heaterWattText);
        }

        if (!TextUtils.isEmpty(bundle.pumpWattText)) {
            tvPumpWattValue.setText(bundle.pumpWattText);
        }

        if (!TextUtils.isEmpty(bundle.logText)) {
            tvLog.setText(bundle.logText);
        }

        if (bundle.updateListAdapter && bundle.listItemAdapter != null && !TextUtils.isEmpty(bundle.updateItemCommand)) {
            boolean itemUpdated = false;
            for (int i = 0; i < bundle.listItemAdapter.getCount(); i++) {
                VoTestItem item = (VoTestItem) bundle.listItemAdapter.getItem(i);
                if (!bundle.updateItemCommand.equals(item.getTest_item_command())) {
                    continue;
                }

                itemUpdated = true;

                if (bundle.receiveCommandResponseOK != null
                        && bundle.receiveCommandResponseOK.equals(bundle.updateItemCommand)
                        && bundle.updateItemResult.equals(Constants.ResultStatus.NG)) {
                    // placeholder for NG specific logging
                }
                if (Constants.TestItemCodes.CM0100.equals(item.getTest_item_command())) {
                    item.setTest_item_name(item.getTest_item_name() + Constants.Common.LOGGER_DEVIDER_01 + bundle.updateItemNameSuffix);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.CM0101)) {
                    item.setTest_item_info(bundle.temperatureValueCompDiff);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.HT0101)
                        || bundle.updateItemCommand.contains(Constants.TestItemCodes.PM0101)
                        || bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0101)
                        || bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0201)
                        || bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0301)
                        || bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0401)) {
                    item.setTest_item_info(bundle.resultInfo);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.TH0101)) {
                    item.setTest_item_info(bundle.decTemperatureHotValue);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.TH0201)) {
                    item.setTest_item_info(bundle.decTemperatureColdValue);
                }
                item.setTest_result_check_value(bundle.updateItemCheckValue);
                item.setTest_item_result(bundle.updateItemResult);
                item.setTest_finish_yn(Constants.ResultStatus.YES);
                if (bundle.finalReadMessage != null) {
                    item.setTest_bt_raw_message(
                            bundle.finalReadMessage.substring(
                                    bundle.finalReadMessage.indexOf(Constants.CharCodes.STX) + 1,
                                    bundle.finalReadMessage.indexOf(Constants.CharCodes.ETX)
                            )
                    );
                }
                if (bundle.finalReceiveCommandResponse != null) {
                    item.setTest_bt_raw_response(bundle.finalReceiveCommandResponse);
                }
                if (!TextUtils.isEmpty(bundle.finalCalculatedResultValue)) {
                    item.setTest_bt_processed_value(bundle.finalCalculatedResultValue);
                }
            }

            if (bundle.shouldUpdateCounts && itemUpdated) {
                recalcTestCountsFromAdapter(bundle.listItemAdapter);
            }

            bundle.listItemAdapter.updateListAdapter();

            if (itemUpdated && controlModeUpdateHandler != null) {
                controlModeUpdateHandler.onItemUpdated(bundle);
            }
        }

        if (bundle.finalCurrentTestItem != null
                && bundle.finalCurrentTestItem.contains(Constants.TestItemCodes.SN0101)) {
            // reserved for additional logic
        }
    }

    public void recalcTestCounts(ItemAdapterTestItem adapter) {
        recalcTestCountsFromAdapter(adapter);
    }

    private void recalcTestCountsFromAdapter(ItemAdapterTestItem adapter) {
        if (adapter == null) {
            return;
        }

        int calculatedOk = 0;
        int calculatedNg = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            VoTestItem item = (VoTestItem) adapter.getItem(i);
            String result = item.getTest_item_result();
            switch (result) {
                case Constants.ResultStatus.OK:
                    calculatedOk++;
                    break;
                case Constants.ResultStatus.NG:
                    calculatedNg++;
                    break;
            }
        }

        if (testCountUpdater != null) {
            testCountUpdater.onCountsUpdated(calculatedOk, calculatedNg);
        }
        tvTestOkCnt.setText(String.valueOf(calculatedOk));
        tvTestNgCnt.setText(String.valueOf(calculatedNg));
    }
}
