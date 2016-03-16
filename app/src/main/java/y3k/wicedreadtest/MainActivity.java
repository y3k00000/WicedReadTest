package y3k.wicedreadtest;

import java.util.Arrays;
import y3k.wicedreadtest.WicedSimpleReader.WicedInfo;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity implements WicedSimpleReader.Listener{
    
	WicedSimpleReader wicedSimpleReader;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.wicedSimpleReader = new WicedSimpleReader(this,this,null);
    }
    
    @Override
    public void onDataRead(WicedInfo info) {
    	if(info.acceleroMeterData!=null){
    		((TextView)findViewById(R.id.accelerometer)).setText("Accelerometer Data : "+Arrays.toString(info.acceleroMeterData));
    	}
    	if(info.gyroData!=null){
    		((TextView)findViewById(R.id.gyro)).setText("Gyrocope Data : "+Arrays.toString(info.gyroData));
    	}
    	if(info.magnoMeterData!=null){
    		((TextView)findViewById(R.id.magno)).setText("Magnometer Data : "+Arrays.toString(info.magnoMeterData));
    	}
    	if(info.angle!=null){
    		((TextView)findViewById(R.id.angle)).setText("Angel = "+info.angle);
    	}
    	if(info.humidity!=null){
    		((TextView)findViewById(R.id.humidity)).setText("Humidity = "+info.humidity);
    	}
    	if(info.pressure!=null){
    		((TextView)findViewById(R.id.pressure)).setText("Pressure = "+info.pressure);
    	}
    	if(info.temperatureCelsius!=null){
    		((TextView)findViewById(R.id.temperature)).setText("Temperature(C) = "+info.temperatureCelsius);
    	}
    }
    
    @Override
    protected void onDestroy() {
    	this.wicedSimpleReader.destroy();
    	super.onDestroy();
    }
}
