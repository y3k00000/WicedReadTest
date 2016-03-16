package y3k.wicedreadtest;

import java.util.Date;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class WicedSimpleReader extends BluetoothGattCallback implements LeScanCallback, Handler.Callback{
	private static final String tag = "WICEDREADER";
	
    private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SENSOR_SERVICE_UUID = UUID.fromString("739298B6-87B6-4984-A5DC-BDC18B068985");
    private static final UUID SENSOR_NOTIFICATION_UUID = UUID.fromString("33EF9113-3B55-413E-B553-FEA1EAADA459");

    private BluetoothDevice wicedTagDevice;
    private BluetoothGatt bluetoothGatt;
    private Context context;
    private Handler valueReadHandler;
    private Listener listener;
    private Long readInterval;
    
	public WicedSimpleReader(Context context, Listener listener, Long readInterval) {
    	this.context = context;
    	this.listener = listener;
    	this.readInterval = readInterval;
    	this.valueReadHandler = new Handler(this);
        ((BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().startLeScan(this);
	}

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		Log.d(tag, "onLeScan("+device.getName()+")");
		if(device.getName()==null){
			return;
		}
		if(device.getName().contains("WICED")&&this.wicedTagDevice==null){
//			device.createBond();
			device.connectGatt(this.context, false, this);
			this.wicedTagDevice = device;
		}
	}
	
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//		Log.d(tag, "onConnectionStateChange("+(newState==BluetoothGatt.STATE_CONNECTED?"STATE_CONNECTED":"STATE_DISCONNECTED")+")");
		if(newState==BluetoothGatt.STATE_CONNECTED){
			this.bluetoothGatt = gatt;
			gatt.discoverServices();
		}
        else if(newState==BluetoothGatt.STATE_DISCONNECTED&&gatt.getDevice().getAddress().equals(this.wicedTagDevice.getAddress())){
            this.wicedTagDevice = null;
        }
	}
	
	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//		Log.d(tag, "onServicesDiscovered("+gatt.getServices().size()+")");
//		Log.d(tag, gatt.getDevice().getName());
		BluetoothGattDescriptor notificationDescriptor = gatt.getService(SENSOR_SERVICE_UUID).getCharacteristic(SENSOR_NOTIFICATION_UUID).getDescriptor(CLIENT_CONFIG_UUID);
//		Log.d(tag, "Desctipter Current Value = "+Arrays.toString(notificationDescriptor.getValue()));
		notificationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//		Log.d(tag, "Write Descriptor BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE = "+Arrays.toString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
		gatt.writeDescriptor(notificationDescriptor);
//		Log.d(tag, "gatt.writeDescriptor(notificationDescriptor)=="+(gatt.writeDescriptor(notificationDescriptor)?"true":"false"));
	}
	
	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//		Log.d(tag, "onDescriptorWrite("+Arrays.toString(descriptor.getValue())+")");
		gatt.setCharacteristicNotification(gatt.getService(SENSOR_SERVICE_UUID).getCharacteristic(SENSOR_NOTIFICATION_UUID), true);
	}
	
	@Override
	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		
	}
	
    long lastUpdateMillis = -1;
	
	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		Log.d(tag, "onCharacteristicChanged("+characteristic.getValue().length+")");
    	long currentMillis = new Date().getTime();
    	if(currentMillis-lastUpdateMillis>(this.readInterval==null?0:this.readInterval)||lastUpdateMillis<0){
    		lastUpdateMillis = currentMillis;
			Message message = new Message();
			message.obj = characteristic.getValue();
			valueReadHandler.sendMessage(message);
    	}
	}
	
	public final static class WicedInfo{
		public int[] acceleroMeterData, gyroData, magnoMeterData;
		public Float angle, humidity, pressure, temperatureCelsius;
		public WicedInfo(
				final int[] acceleroMeterData,
				final int[] gyroData,
				final int[] magnoMeterData,
				final Float angle,
				final Float humidity,
				final Float pressure,
				final Float temperatureCelsius){
			this.acceleroMeterData = acceleroMeterData;
			this.gyroData = gyroData;
			this.magnoMeterData = magnoMeterData;
			this.angle = angle;
			this.humidity = humidity;
			this.pressure = pressure;
			this.temperatureCelsius = temperatureCelsius;
		}
	}
	
	public interface Listener{
		void onDataRead(WicedInfo info);
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		byte[] data = (byte[])msg.obj;
    	switch (data.length) {
    		case 19:
                // packet type specifying accelerometer, gyro, magno
                int[] acceleroMeterData = SensorDataParser.getAccelorometerData(data, 1);
                int[] gyroData = SensorDataParser.getGyroData(data, 1+SensorDataParser.SENSOR_ACCEL_DATA_SIZE);
                int[] magnoMeterData =SensorDataParser.getMagnometerData(data, 1+SensorDataParser.SENSOR_ACCEL_DATA_SIZE+SensorDataParser.SENSOR_GYRO_DATA_SIZE);
                float angle = SensorDataParser.getCompassAngleDegrees(magnoMeterData);
                this.listener.onDataRead(new WicedInfo(acceleroMeterData, gyroData, magnoMeterData, angle, null, null, null));
                break;
            case 7:
                // packet type specifying temp, humid, press
                float humidity = SensorDataParser.getHumidityPercent(data, 1);
                float pressure = SensorDataParser.getPressureMBar(data, 1+SensorDataParser.SENSOR_HUMD_DATA_SIZE);
                float temperatureCelsius = SensorDataParser.getTemperatureC(data, 1+SensorDataParser.SENSOR_HUMD_DATA_SIZE+SensorDataParser.SENSOR_PRES_DATA_SIZE);
                this.listener.onDataRead(new WicedInfo(null, null, null, null, humidity, pressure, temperatureCelsius));
                break;
            }
		return true;
	}
	
	public final void destroy(){
		if(this.bluetoothGatt!=null){
			BluetoothGattDescriptor notificationDescriptor = bluetoothGatt.getService(SENSOR_SERVICE_UUID).getCharacteristic(SENSOR_NOTIFICATION_UUID).getDescriptor(CLIENT_CONFIG_UUID);
			notificationDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
			this.bluetoothGatt.writeDescriptor(notificationDescriptor);
			this.bluetoothGatt.setCharacteristicNotification(this.bluetoothGatt.getService(SENSOR_SERVICE_UUID).getCharacteristic(SENSOR_NOTIFICATION_UUID), false);
			this.bluetoothGatt.disconnect();
		}
	}
}
