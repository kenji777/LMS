package itf.com.app.simple_line_test_ovio_new.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class CustomYAxisRenderer extends YAxisRenderer {
    private Paint subGridPaint;
    private float subGridGranularity = 10f; // 서브 그리드 간격
    
    public CustomYAxisRenderer(ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans) {
        super(viewPortHandler, yAxis, trans);
        
        // 서브 그리드 페인트 설정
        subGridPaint = new Paint();
        subGridPaint.setColor(0x99CCCCCC); // 흐린 회색 (ARGB: 4D = 약 30% 투명도, 더 잘 보이도록)
        subGridPaint.setStrokeWidth(1f); // 선 두께 (더 잘 보이도록)
        subGridPaint.setStyle(Paint.Style.STROKE);
    }
    
    @Override
    public void renderGridLines(Canvas c) {
        // 먼저 메인 그리드 그리기
        super.renderGridLines(c);
        
        // 서브 그리드 그리기
        if (!mYAxis.isDrawGridLinesEnabled() || !mYAxis.isEnabled())
            return;
        
        float[] positions = new float[2];
        float min = mYAxis.getAxisMinimum();
        float max = mYAxis.getAxisMaximum();
        
        // Y축의 레이블 개수에 따라 메인 그리드 간격 계산
        int labelCount = mYAxis.getLabelCount();
        float mainInterval = 0f;
        if (labelCount > 1) {
            mainInterval = (max - min) / (labelCount - 1);
        } else {
            mainInterval = max - min;
        }
        
        // 서브 그리드를 1단위로 그리기
        float subGridStep = 1.0f; // 1단위 간격으로 고정
        
        // 서브 그리드 라인 그리기 (min부터 max까지 1단위씩)
        for (float i = min; i <= max; i += subGridStep) {
            // 메인 그리드 위치는 건너뛰기 (메인 그리드 간격의 배수인 경우)
            if (mainInterval > 0) {
                float remainder = (i - min) % mainInterval;
                // 메인 그리드 위치인지 확인 (약간의 오차 허용)
                if (Math.abs(remainder) < 0.01f || Math.abs(remainder - mainInterval) < 0.01f) {
                    continue; // 메인 그리드 위치는 건너뛰기
                }
            }
            
            positions[1] = i;
            mTrans.pointValuesToPixel(positions);
            
            // 화면 범위 내에 있는지 확인
            float yPos = positions[1];
            if (yPos >= mViewPortHandler.offsetTop() && 
                yPos <= mViewPortHandler.getChartHeight() - mViewPortHandler.offsetBottom()) {
                c.drawLine(mViewPortHandler.offsetLeft(), yPos, 
                          mViewPortHandler.getChartWidth() - mViewPortHandler.offsetRight(), yPos, 
                          subGridPaint);
            }
        }
    }
    
    public void setSubGridGranularity(float granularity) {
        this.subGridGranularity = granularity;
    }
    
    public void setSubGridColor(int color) {
        subGridPaint.setColor(color);
    }
    
    public void setSubGridWidth(float width) {
        subGridPaint.setStrokeWidth(width);
    }
}

