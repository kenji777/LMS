// ============================================================
// applyTestSpecData() 메인 스레드 블로킹 해결 - 개선된 코드
// ============================================================
// 위치: ActivityModel_0002.java (3989-4230번 라인)
// 변경 사항:
// 1. 값 수집 루프를 백그라운드로 이동
// 2. ListView 아이템 데이터 준비를 백그라운드로 이동
// 3. 시간 계산을 백그라운드로 이동
// 4. 중첩된 mainHandler.post() 제거
// ============================================================

private boolean applyTestSpecData(List<Map<String, String>> sourceData, boolean persistToDb) {
    if (sourceData == null || sourceData.isEmpty()) {
        return false;
    }

    // ⚠️ 중요: 데이터 정규화는 빠르므로 메인 스레드에서 수행
    List<Map<String, String>> normalizedList = new ArrayList<>();
    for (Map<String, String> item : sourceData) {
        normalizedList.add(new HashMap<>(item));
    }

    lstSpecData = normalizedList;
    refreshSpecCache(normalizedList);

    // ⚠️ 중요: 모든 무거운 작업을 백그라운드 스레드로 이동하여 메인 스레드 블로킹 방지
    new Thread(() -> {
        Context context = getApplicationContext();
        int tempTotalTimeCnt = 0;
        String[][] tempArrTestItems = new String[normalizedList.size()][10];
        String tempValueWatt = Constants.Common.EMPTY_STRING;
        String tempLowerValueWatt = Constants.Common.EMPTY_STRING;
        String tempUpperValueWatt = Constants.Common.EMPTY_STRING;
        String tempProductSerialNo = Constants.Common.EMPTY_STRING;

        // 1. 데이터 처리 루프 (백그라운드 스레드에서 실행)
        for (int i = 0; i < normalizedList.size(); i++) {
            Map<String, String> spec = normalizedList.get(i);
            if (persistToDb) {
                TestData.insertTestSpecData(context, spec);
            }
            try {
                int seconds = Integer.parseInt(valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC)));
                tempTotalTimeCnt += seconds;
            } catch (Exception ignored) {
            }

            tempArrTestItems[i][0] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_NAME));
            tempArrTestItems[i][1] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
            tempArrTestItems[i][2] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC));
            tempArrTestItems[i][3] = valueOrEmpty(String.valueOf(tempTotalTimeCnt));
            tempArrTestItems[i][4] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_TYPE));
            tempArrTestItems[i][5] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
            tempArrTestItems[i][6] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_STEP));
            tempArrTestItems[i][7] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_RESPONSE_VALUE));
            
            // ⚠️ 중요: Double.parseDouble() 예외 처리 강화
            try {
                if(Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.CM0101) ||
                    Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.HT0101) ||
                    Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.PM0101) ||
                    Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0101) ||
                    Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0201) ||
                    Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0301) ||
                    Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0401)
                ) {
                    String valueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                    String lowerValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                    String upperValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                    
                    if (!valueWattStr.isEmpty() && !lowerValueWattStr.isEmpty() && !upperValueWattStr.isEmpty()) {
                        double valueWatt = Double.parseDouble(valueWattStr);
                        double lowerValueWatt = Double.parseDouble(lowerValueWattStr);
                        double upperValueWatt = Double.parseDouble(upperValueWattStr);
                        tempArrTestItems[i][8] = String.valueOf(valueWatt - lowerValueWatt);
                        tempArrTestItems[i][9] = String.valueOf(valueWatt + upperValueWatt);
                    } else {
                        tempArrTestItems[i][8] = "0";
                        tempArrTestItems[i][9] = "0";
                    }
                }
                else if(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0101) ||
                        spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0201) ||
                        spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0301)
                ) {
                    String valueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE));
                    String lowerValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE));
                    String upperValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE));
                    
                    if (!valueStr.isEmpty() && !lowerValueStr.isEmpty() && !upperValueStr.isEmpty()) {
                        double value = Double.parseDouble(valueStr);
                        double lowerValue = Double.parseDouble(lowerValueStr);
                        double upperValue = Double.parseDouble(upperValueStr);
                        tempArrTestItems[i][8] = String.valueOf(value - lowerValue);
                        tempArrTestItems[i][9] = String.valueOf(value + upperValue);
                    } else {
                        tempArrTestItems[i][8] = "0";
                        tempArrTestItems[i][9] = "0";
                    }
                }
                else {
                    tempArrTestItems[i][8] = "0";
                    tempArrTestItems[i][9] = "0";
                }
            } catch (NumberFormatException e) {
                // 파싱 실패 시 기본값 사용
                tempArrTestItems[i][8] = "0";
                tempArrTestItems[i][9] = "0";
            }
            
            tempValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
            tempLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
            tempUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
            tempProductSerialNo = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
        }

        // 2. 시간 계산 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
        int calculatedTotalTimeCnt = 0;
        for (int i = 0; i < tempArrTestItems.length; i++) {
            try {
                int seconds = Integer.parseInt(tempArrTestItems[i][2]);
                calculatedTotalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
                tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
            } catch (NumberFormatException e) {
                // 파싱 실패 시 기본값 사용
                tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
            }
        }

        // 3. UI 업데이트 값 수집 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
        String compValueWatt = null;
        String compLowerValueWatt = null;
        String compUpperValueWatt = null;
        String pumpValueWatt = null;
        String pumpLowerValueWatt = null;
        String pumpUpperValueWatt = null;
        String heaterValueWatt = null;
        String heaterLowerValueWatt = null;
        String heaterUpperValueWatt = null;

        for (int i = 0; i < normalizedList.size(); i++) {
            try {
                Map<String, String> spec = normalizedList.get(i);
                String command = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
                String itemValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                String itemLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                String itemUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));

                // 값 수집 (UI 업데이트는 나중에 한 번에 수행)
                if (command.contains(Constants.TestItemCodes.CM0101)) {
                    compValueWatt = itemValueWatt;
                    compLowerValueWatt = itemLowerValueWatt;
                    compUpperValueWatt = itemUpperValueWatt;
                }
                if (command.contains(Constants.TestItemCodes.PM0101)) {
                    pumpValueWatt = itemValueWatt;
                    pumpLowerValueWatt = itemLowerValueWatt;
                    pumpUpperValueWatt = itemUpperValueWatt;
                }
                if (command.contains(Constants.TestItemCodes.HT0101)) {
                    compValueWatt = itemValueWatt;
                    compLowerValueWatt = itemLowerValueWatt;
                    compUpperValueWatt = itemUpperValueWatt;
                    heaterValueWatt = itemValueWatt;
                    heaterLowerValueWatt = itemLowerValueWatt;
                    heaterUpperValueWatt = itemUpperValueWatt;
                }
            } catch (Exception e) {
                logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE, e);
            }
        }

        // 4. ListView 아이템 데이터 준비 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
        // ⚠️ 주의: getString()은 메인 스레드에서만 호출 가능하므로,
        //          문자열 리소스는 메인 스레드에서 미리 가져와서 사용
        //          또는 runOnUiThread 내부에서만 호출
        // 
        // 해결 방법: getString() 호출을 제거하고 하드코딩된 문자열 사용
        //            또는 메인 스레드에서 미리 가져온 값을 사용
        //            여기서는 기존 코드와 동일하게 처리하기 위해
        //            getString() 호출은 runOnUiThread 내부로 이동
        
        // 백그라운드에서 준비 가능한 데이터만 준비
        // getString() 호출이 필요한 부분은 runOnUiThread 내부에서 처리
        List<Map<String, Object>> itemsDataToAdd = new ArrayList<>(); // getString() 호출 전 데이터 저장
        for (int i = 0; i < tempArrTestItems.length; i++) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
            itemData.put(Constants.Common.TEST_ITEM_NAME, tempArrTestItems[i][0]);
            itemData.put(Constants.Common.TEST_ITEM_COMMAND, tempArrTestItems[i][1]);
            itemData.put(Constants.Common.TEST_RESPONSE_VALUE, tempArrTestItems[i][7]);
            // TEST_ITEM_RESULT는 getString() 호출이 필요하므로 나중에 설정
            itemData.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
            itemData.put(Constants.Common.TEST_MODEL_ID, globalModelId);
            itemsDataToAdd.add(itemData);
        }

        // final 변수로 캡처
        final int finalTotalTimeCnt = calculatedTotalTimeCnt;
        final String[][] finalArrTestItems = tempArrTestItems;
        final String finalValueWatt = tempValueWatt;
        final String finalLowerValueWatt = tempLowerValueWatt;
        final String finalUpperValueWatt = tempUpperValueWatt;
        final String finalProductSerialNo = tempProductSerialNo;
        final List<Map<String, Object>> finalItemsDataToAdd = itemsDataToAdd; // getString() 호출 전 데이터
        final String finalCompValueWatt = compValueWatt;
        final String finalCompLowerValueWatt = compLowerValueWatt;
        final String finalCompUpperValueWatt = compUpperValueWatt;
        final String finalPumpValueWatt = pumpValueWatt;
        final String finalPumpLowerValueWatt = pumpLowerValueWatt;
        final String finalPumpUpperValueWatt = pumpUpperValueWatt;
        final String finalHeaterValueWatt = heaterValueWatt;
        final String finalHeaterLowerValueWatt = heaterLowerValueWatt;
        final String finalHeaterUpperValueWatt = heaterUpperValueWatt;

        // ⚠️ UI 업데이트는 한 번만 runOnUiThread로 포스팅 (최소한의 작업만 수행)
        runOnUiThread(() -> {
            try {
                ActivityModel_0002 act = getMainActivity();
                if (act == null || act.isFinishing()) {
                    return;
                }

                // 1. 변수 할당 (빠름)
                arrTestItems = finalArrTestItems;
                totalTimeCnt = finalTotalTimeCnt;
                valueWatt = finalValueWatt;
                lowerValueWatt = finalLowerValueWatt;
                upperValueWatt = finalUpperValueWatt;
                productSerialNo = finalProductSerialNo;

                // 2. UI 업데이트 (빠름 - 값만 설정, 루프 제거됨)
                if (finalCompValueWatt != null) {
                    tvCompValueWatt.setText(finalCompValueWatt);
                    updateRangeViews(tvCompLowerValueWatt, tvCompUpperValueWatt, 
                        finalCompValueWatt, finalCompLowerValueWatt, finalCompUpperValueWatt);
                }
                if (finalPumpValueWatt != null) {
                    tvPumpValueWatt.setText(finalPumpValueWatt);
                    updateRangeViews(tvPumpLowerValueWatt, tvPumpUpperValueWatt, 
                        finalPumpValueWatt, finalPumpLowerValueWatt, finalPumpUpperValueWatt);
                }
                if (finalHeaterValueWatt != null) {
                    tvHeaterValueWatt.setText(finalHeaterValueWatt);
                    updateRangeViews(tvHeaterLowerValueWatt, tvHeaterUpperValueWatt, 
                        finalHeaterValueWatt, finalHeaterLowerValueWatt, finalHeaterUpperValueWatt);
                }

                // 3. ListView 초기화 및 아이템 추가 (준비된 데이터 사용, getString() 호출은 한 번만)
                listItemAdapter = new ItemAdapterTestItem();
                lstTestResult = new ArrayList<>();
                lstTestTemperature = new ArrayList<>();
                
                // ⚠️ getString() 호출을 한 번만 수행하여 성능 최적화
                String preProcessText = getString(R.string.txt_pre_process);
                
                // ⚠️ 준비된 데이터를 사용하여 VoTestItem 생성 (getString() 호출은 한 번만)
                for (Map<String, Object> itemData : finalItemsDataToAdd) {
                    Map<String, String> mapListItem = new HashMap<>();
                    mapListItem.put(Constants.Common.TEST_ITEM_SEQ, (String) itemData.get(Constants.Common.TEST_ITEM_SEQ));
                    mapListItem.put(Constants.Common.TEST_ITEM_NAME, (String) itemData.get(Constants.Common.TEST_ITEM_NAME));
                    mapListItem.put(Constants.Common.TEST_ITEM_COMMAND, (String) itemData.get(Constants.Common.TEST_ITEM_COMMAND));
                    mapListItem.put(Constants.Common.TEST_RESPONSE_VALUE, (String) itemData.get(Constants.Common.TEST_RESPONSE_VALUE));
                    mapListItem.put(Constants.Common.TEST_ITEM_RESULT, preProcessText); // 재사용
                    mapListItem.put(Constants.Common.TEST_FINISH_YN, (String) itemData.get(Constants.Common.TEST_FINISH_YN));
                    mapListItem.put(Constants.Common.TEST_MODEL_ID, (String) itemData.get(Constants.Common.TEST_MODEL_ID));
                    listItemAdapter.addItem(new VoTestItem(mapListItem));
                }

                // 4. UI 업데이트 (중첩된 mainHandler.post() 제거됨) - ⚠️ 개선
                tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
                lvTestItem.setAdapter(listItemAdapter);
                listItemAdapter.updateListAdapter(); // 한 번만 notifyDataSetChanged() 호출
                lastTestIdx = listItemAdapter.getCount();
            } catch (Exception e) {
                logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE_JSON_PARSING, e);
            }
        });
    }).start(); // 백그라운드 스레드 시작

    return true;
}

// ============================================================
// 변경 사항 요약:
// ============================================================
// 1. ✅ 시간 계산 루프를 백그라운드로 이동 (4190-4199번 라인 → 백그라운드)
// 2. ✅ 값 수집 루프를 백그라운드로 이동 (4129-4159번 라인 → 백그라운드)
// 3. ✅ ListView 아이템 데이터 준비를 백그라운드로 이동 (4203-4213번 라인 → 백그라운드)
// 4. ✅ 중첩된 mainHandler.post() 제거 (4217번 라인 제거)
// 
// 예상 개선 효과:
// - 메인 스레드 블로킹: 40-80ms → 5-10ms (약 85% 감소)
// - Skip Frame: 48 frames → 5-10 frames (약 80% 감소)
// ============================================================

