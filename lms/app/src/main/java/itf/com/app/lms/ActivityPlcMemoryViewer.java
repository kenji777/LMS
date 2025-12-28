package itf.com.app.lms;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import itf.com.app.lms.kiosk.BaseKioskActivity;
import itf.com.app.lms.util.LogManager;
import itf.com.app.lms.util.PlcMemoryScanner;
import itf.com.app.lms.util.UsbCommandManager;

/**
 * PLC 메모리 뷰어 Activity
 * PLC의 전체 메모리를 스캔하여 데이터가 저장된 메모리 정보를 그리드 형태로 표시
 */
public class ActivityPlcMemoryViewer extends BaseKioskActivity {
    private static final String TAG = "PlcMemoryViewer";
    
    private Spinner spinnerMemoryType;
    private EditText etStartAddress;
    private EditText etEndAddress;
    private Button btnScan;
    private Button btnStop;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private TableLayout tableMemoryData;
    private FloatingActionButton fabClose;
    
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private CompletableFuture<PlcMemoryScanner.ScanResult> currentScanFuture = null;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plc_memory_viewer);
        
        // USB 통신 인프라 초기화
        UsbCommandManager.getInstance().initialize(this);
        
        initializeViews();
        setupSpinner();
        setupButtons();
    }
    
    private void initializeViews() {
        spinnerMemoryType = findViewById(R.id.spinner_memory_type);
        etStartAddress = findViewById(R.id.et_start_address);
        etEndAddress = findViewById(R.id.et_end_address);
        btnScan = findViewById(R.id.btn_scan);
        btnStop = findViewById(R.id.btn_stop);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        tableMemoryData = findViewById(R.id.table_memory_data);
        fabClose = findViewById(R.id.fab_close);
        
        // 초기 상태
        btnStop.setEnabled(false);
        progressBar.setVisibility(View.GONE);
    }
    
    private void setupSpinner() {
        // 메모리 타입 목록
        String[] memoryTypes = {
            "DW - Double Word (32비트)",
            "DM - Data Memory (16비트)",
            "D - Data Register (16비트)",
            "W - Word (16비트)",
            "R - Relay (비트)"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            memoryTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMemoryType.setAdapter(adapter);
    }
    
    private void setupButtons() {
        btnScan.setOnClickListener(v -> startScan());
        btnStop.setOnClickListener(v -> stopScan());
        fabClose.setOnClickListener(v -> finish());
    }
    
    private void startScan() {
        // USB 통신 인프라 확인
        UsbCommandManager usbManager = UsbCommandManager.getInstance();
        if (!usbManager.isInitialized()) {
            showError("PLC 연결이 필요합니다.\nUSB 통신이 초기화되지 않았습니다.");
            return;
        }
        
        // 입력값 검증
        String startAddrStr = etStartAddress.getText().toString().trim();
        String endAddrStr = etEndAddress.getText().toString().trim();
        
        if (TextUtils.isEmpty(startAddrStr) || TextUtils.isEmpty(endAddrStr)) {
            showError("시작 주소와 종료 주소를 입력해주세요.");
            return;
        }
        
        int startAddress, endAddress;
        try {
            startAddress = Integer.parseInt(startAddrStr);
            endAddress = Integer.parseInt(endAddrStr);
        } catch (NumberFormatException e) {
            showError("주소는 숫자로 입력해주세요.");
            return;
        }
        
        if (startAddress < 0 || endAddress < 0) {
            showError("주소는 0 이상이어야 합니다.");
            return;
        }
        
        if (startAddress > endAddress) {
            showError("시작 주소는 종료 주소보다 작거나 같아야 합니다.");
            return;
        }
        
        if (endAddress - startAddress > 1000) {
            new AlertDialog.Builder(this)
                .setTitle("경고")
                .setMessage("스캔 범위가 너무 큽니다 (" + (endAddress - startAddress + 1) + "개 주소).\n계속하시겠습니까?")
                .setPositiveButton("계속", (dialog, which) -> performScan(startAddress, endAddress))
                .setNegativeButton("취소", null)
                .show();
        } else {
            performScan(startAddress, endAddress);
        }
    }
    
    private void performScan(int startAddress, int endAddress) {
        if (isScanning.get()) {
            return;
        }
        
        // 메모리 타입 선택
        int selectedPosition = spinnerMemoryType.getSelectedItemPosition();
        PlcMemoryScanner.MemoryType memoryType;
        switch (selectedPosition) {
            case 0:
                memoryType = PlcMemoryScanner.MemoryType.DW;
                break;
            case 1:
                memoryType = PlcMemoryScanner.MemoryType.DM;
                break;
            case 2:
                memoryType = PlcMemoryScanner.MemoryType.D;
                break;
            case 3:
                memoryType = PlcMemoryScanner.MemoryType.W;
                break;
            case 4:
                memoryType = PlcMemoryScanner.MemoryType.R;
                break;
            default:
                memoryType = PlcMemoryScanner.MemoryType.DW;
        }
        
        isScanning.set(true);
        btnScan.setEnabled(false);
        btnStop.setEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        clearTable();
        
        // UsbCommandManager를 사용하여 명령 전송
        UsbCommandManager usbManager = UsbCommandManager.getInstance();
        PlcMemoryScanner.PlcCommandSender sender = (command, description) -> {
            return usbManager.sendUsbCommandWithResponse(
                new String(command),
                description,
                2000 // 2초 타임아웃
            );
        };
        
        // 진행 상황 콜백
        PlcMemoryScanner.ProgressCallback progressCallback = new PlcMemoryScanner.ProgressCallback() {
            @Override
            public void onProgress(int current, int total, String message) {
                mainHandler.post(() -> {
                    tvProgress.setText(message);
                    if (total > 0) {
                        int progress = (int) ((current * 100.0) / total);
                        progressBar.setProgress(progress);
                    }
                });
            }
            
            @Override
            public void onComplete(String message) {
                mainHandler.post(() -> {
                    tvProgress.setText(message);
                    progressBar.setProgress(100);
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    tvProgress.setText("오류: " + error);
                    showError(error);
                });
            }
        };
        
        // 스캔 시작
        currentScanFuture = PlcMemoryScanner.scanMemory(
            sender,
            memoryType,
            startAddress,
            endAddress,
            progressCallback
        );
        
        // 스캔 완료 처리
        currentScanFuture.whenComplete((result, throwable) -> {
            mainHandler.post(() -> {
                isScanning.set(false);
                btnScan.setEnabled(true);
                btnStop.setEnabled(false);
                progressBar.setVisibility(View.GONE);
                
                if (throwable != null) {
                    LogManager.e(LogManager.LogCategory.ER, TAG, "Scan failed", throwable);
                    tvProgress.setText("스캔 실패: " + throwable.getMessage());
                    showError("스캔 실패: " + throwable.getMessage());
                } else if (result != null) {
                    tvProgress.setText(String.format("스캔 완료: %d개 주소 중 %d개 데이터 발견 (소요 시간: %.2f초)",
                                                    result.totalScanned, result.dataFound, result.scanTimeMs / 1000.0));
                    displayResults(result);
                }
            });
        });
    }
    
    private void stopScan() {
        if (currentScanFuture != null && !currentScanFuture.isDone()) {
            currentScanFuture.cancel(true);
        }
        isScanning.set(false);
        btnScan.setEnabled(true);
        btnStop.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        tvProgress.setText("스캔이 중지되었습니다.");
    }
    
    private void clearTable() {
        // 헤더 행을 제외한 모든 행 제거
        int childCount = tableMemoryData.getChildCount();
        for (int i = childCount - 1; i > 0; i--) {
            tableMemoryData.removeViewAt(i);
        }
    }
    
    private void displayResults(PlcMemoryScanner.ScanResult result) {
        clearTable();
        
        if (result.memoryDataList == null || result.memoryDataList.isEmpty()) {
            // 데이터가 없을 때 메시지 표시
            TableRow row = new TableRow(this);
            TextView tv = new TextView(this);
            tv.setText("데이터가 저장된 메모리가 없습니다.");
            tv.setPadding(20, 20, 20, 20);
            tv.setTextSize(16);
            tv.setGravity(android.view.Gravity.CENTER);
            row.addView(tv);
            tableMemoryData.addView(row);
            return;
        }
        
        // 데이터 행 추가
        for (PlcMemoryScanner.MemoryData data : result.memoryDataList) {
            TableRow row = new TableRow(this);
            row.setPadding(0, 5, 0, 5);
            
            // 주소
            TextView tvAddress = createTableCell(data.address.addressString, false);
            row.addView(tvAddress);
            
            // 10진수 값
            TextView tvDecimal = createTableCell(String.valueOf(data.intValue), false);
            row.addView(tvDecimal);
            
            // 16진수 값
            TextView tvHex = createTableCell("0x" + data.hexValue, false);
            row.addView(tvHex);
            
            // 2진수 값 (최대 16비트만 표시)
            String binary = data.binaryValue;
            if (binary.length() > 16) {
                binary = binary.substring(binary.length() - 16);
            }
            TextView tvBinary = createTableCell(binary, false);
            row.addView(tvBinary);
            
            // 원시값
            TextView tvRaw = createTableCell(data.rawValue, true);
            tvRaw.setMaxWidth(200);
            row.addView(tvRaw);
            
            tableMemoryData.addView(row);
        }
    }
    
    private TextView createTableCell(String text, boolean isRaw) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(10, 10, 10, 10);
        tv.setTextSize(14);
        tv.setGravity(android.view.Gravity.CENTER);
        // tv.setFontFamily(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        
        if (isRaw) {
            tv.setMaxLines(1);
            tv.setEllipsize(TextUtils.TruncateAt.END);
        }
        
        return tv;
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }
}

