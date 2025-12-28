package itf.com.lms.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class CustomXAxisRenderer extends XAxisRenderer {
    private Paint subGridPaint;
    private float subGridGranularity = 0.5f; // 서브 그리드 간격 (메인 그리드의 절반)
    private float dataMinX = Float.NaN; // 실제 데이터의 최소 X값
    private float dataMaxX = Float.NaN; // 실제 데이터의 최대 X값
    
    public CustomXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
        super(viewPortHandler, xAxis, trans);
        
        // 서브 그리드 페인트 설정
        subGridPaint = new Paint();
        subGridPaint.setColor(0x99CCCCCC); // 흐린 회색 (ARGB: 4D = 약 30% 투명도, 더 잘 보이도록)
        subGridPaint.setStrokeWidth(1f); // 선 두께 (더 잘 보이도록)
        subGridPaint.setStyle(Paint.Style.STROKE);
    }
    
    // 실제 데이터 범위를 설정하는 메서드
    public void setDataRange(float minX, float maxX) {
        this.dataMinX = minX;
        this.dataMaxX = maxX;
    }
    
    @Override
    public void renderGridLines(Canvas c) {
        // 먼저 메인 그리드 그리기 (부모 클래스 메서드 호출)
        super.renderGridLines(c);
        
        // 서브 그리드 그리기
        if (!mXAxis.isDrawGridLinesEnabled() || !mXAxis.isEnabled())
            return;
        
        float[] positions = new float[2];
        
        // 실제 데이터 범위 또는 축 범위 가져오기
        float min = 0f;
        float max = 0f;
        
        // 먼저 설정된 실제 데이터 범위 사용
        if (!Float.isNaN(dataMinX) && !Float.isNaN(dataMaxX)) {
            min = dataMinX;
            max = dataMaxX;
        } else {
            // 데이터 범위가 없으면 리플렉션으로 축 범위 가져오기
            try {
                java.lang.reflect.Field minField = XAxis.class.getDeclaredField("mAxisMinimum");
                minField.setAccessible(true);
                min = minField.getFloat(mXAxis);
                
                java.lang.reflect.Field maxField = XAxis.class.getDeclaredField("mAxisMaximum");
                maxField.setAccessible(true);
                max = maxField.getFloat(mXAxis);
            } catch (Exception e) {
                // 리플렉션 실패 시 ViewPortHandler를 통해 계산
                // 충분히 넓은 범위로 설정 (나중에 화면 밖은 자동으로 제외됨)
                min = 0f;
                max = 10000f; // 충분히 큰 값
            }
        }
        
        // 서브 그리드 라인 그리기 - 실제 데이터 범위보다 약간 넓게 (양쪽으로 1 단위씩)
        float startValue = Math.max(0f, min - 1f);
        float endValue = max + 1f;
        
        for (float i = startValue; i <= endValue; i += subGridGranularity) {
            // 메인 그리드 위치는 건너뛰기 (정수 위치)
            float remainder = Math.abs(i % 1.0f);
            if (remainder < 0.01f || remainder > 0.99f) {
                continue;
            }
            
            positions[0] = i;
            positions[1] = 0f; // Y값은 0으로 설정 (X축 그리드이므로)
            mTrans.pointValuesToPixel(positions);
            
            // 화면 범위 내에 있는지 확인 (약간의 여유를 둠)
            float xPos = positions[0];
            float leftBound = mViewPortHandler.offsetLeft() - 5f;
            float rightBound = mViewPortHandler.getChartWidth() - mViewPortHandler.offsetRight() + 5f;
            
            if (xPos >= leftBound && xPos <= rightBound) {
                float top = mViewPortHandler.offsetTop();
                float bottom = mViewPortHandler.getChartHeight() - mViewPortHandler.offsetBottom();
                c.drawLine(xPos, top, xPos, bottom, subGridPaint);
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


