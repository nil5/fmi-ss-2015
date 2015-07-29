package de.thm.nfcmemory.model.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import de.thm.nfcmemory.R;

/**
 * Created by Nils on 29.07.2015.
 */
public class BluetoothDeviceListAdapter extends BaseAdapter{
    private Context context;
    private List<BluetoothDevice> btDevices;

    public BluetoothDeviceListAdapter(Context context, List<BluetoothDevice> btDevices){
        this.context = context;
        this.btDevices = btDevices;
    }

    public void add(BluetoothDevice device){
        add(device, btDevices.size());
    }

    public void add(BluetoothDevice device, int index){
        btDevices.add(index, device);
        notifyDataSetChanged();
    }

    public void remove(BluetoothDevice device){
        btDevices.remove(device);
        notifyDataSetChanged();
    }

    public void remove(int index){
        btDevices.remove(index);
        notifyDataSetChanged();
    }

    public void clear(){
        btDevices.clear();
        notifyDataSetChanged();
    }

    public BluetoothDevice get(int index){
        return (BluetoothDevice) getItem(index);
    }

    @Override
    public int getCount() {
        return btDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return btDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if(convertView == null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.list_item_bluetooth_device, parent, false);

            holder.name = (TextView) convertView.findViewById(R.id.bluetooth_device_name);

            convertView.setTag(holder);
        } else holder = (ViewHolder) convertView.getTag();

        BluetoothDevice btDevice = (BluetoothDevice) getItem(position);

        holder.name.setText(btDevice.getName());

        return convertView;
    }

    private class ViewHolder{
        protected TextView name;
    }
}
