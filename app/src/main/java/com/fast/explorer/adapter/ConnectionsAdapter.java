package com.fast.explorer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fast.explorer.R;
import com.fast.explorer.cloud.CloudConnection;
import com.fast.explorer.misc.IconColorUtils;
import com.fast.explorer.misc.IconUtils;
import com.fast.explorer.network.NetworkConnection;
import com.fast.explorer.provider.ExplorerProvider;

import static com.fast.explorer.DocumentsApplication.isTelevision;
import static com.fast.explorer.model.DocumentInfo.getCursorString;
import static com.fast.explorer.provider.CloudStorageProvider.TYPE_CLOUD;

public class ConnectionsAdapter extends BaseAdapter{
    private Cursor mCursor;
    private int mCursorCount;
    private View.OnClickListener mListener;

    public ConnectionsAdapter(View.OnClickListener listener){
        mListener = listener;
    }

    public void swapResult(Cursor result) {
        mCursor = result;
        mCursorCount = mCursor != null ? mCursor.getCount() : 0;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getDocumentView(position, convertView, parent);

    }

    private View getDocumentView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();

        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_connection_list, parent, false);
        }

        final Cursor cursor = getItem(position);
        String type = getCursorString(cursor, ExplorerProvider.ConnectionColumns.TYPE);

        final ImageView iconMime = (ImageView) convertView.findViewById(R.id.icon_mime);
        final View iconMimeBackground = convertView.findViewById(R.id.icon_mime_background);
        final TextView title = (TextView) convertView.findViewById(android.R.id.title);
        final ImageView icon1 = (ImageView) convertView.findViewById(android.R.id.icon1);
        final TextView summary = (TextView) convertView.findViewById(android.R.id.summary);
        final View popupButton = convertView.findViewById(R.id.button_popup);
        popupButton.setVisibility(isTelevision() ? View.INVISIBLE : View.VISIBLE);
        popupButton.setOnClickListener(mListener);

        NetworkConnection networkConnection = NetworkConnection.fromConnectionsCursor(cursor);
        if(networkConnection.type.startsWith(TYPE_CLOUD)){
            title.setText(CloudConnection.getTypeName(networkConnection.type));
            summary.setText(networkConnection.username);
            iconMimeBackground.setVisibility(View.VISIBLE);
            iconMimeBackground.setBackgroundColor(
                    IconColorUtils.loadCloudColor(context, networkConnection.getType()));
            iconMime.setImageDrawable(IconUtils.loadCloudIcon(context, networkConnection.type));
        } else {
            title.setText(networkConnection.getName());
            summary.setText(networkConnection.getSummary());
            iconMimeBackground.setVisibility(View.VISIBLE);
            iconMimeBackground.setBackgroundColor(
                    IconColorUtils.loadSchmeColor(context, networkConnection.getType()));
            iconMime.setImageDrawable(IconUtils.loadSchemeIcon(context, networkConnection.type));
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return mCursorCount;
    }

    @Override
    public Cursor getItem(int position) {
        if (position < mCursorCount) {
            mCursor.moveToPosition(position);
            return mCursor;
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }
}