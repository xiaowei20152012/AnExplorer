package com.fast.explorer.directory;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import com.fast.explorer.R;
import com.fast.explorer.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import com.fast.explorer.misc.IconHelper;
import com.fast.explorer.model.DocumentInfo;

import static com.fast.explorer.DocumentsApplication.isTelevision;

public abstract class DocumentHolder extends BaseHolder implements View.OnClickListener{

    protected final Context mContext;

    protected OnItemClickListener mOnItemClickListener;
    protected DocumentsAdapter.Environment mEnv;
    protected IconHelper mIconHelper;
    protected DocumentInfo mDoc;
    protected MultiChoiceHelper multiChoiceHelper;

    protected final ImageView iconMime;
    protected final ImageView iconThumb;
    protected final View iconMimeBackground;
    protected final TextView title;
    protected final ImageView icon1;
    protected final ImageView icon2;
    protected final TextView summary;
    protected final TextView date;
    protected final TextView size;
    protected final View popupButton;
    protected final View line1;
    protected final View line2;
    protected final View iconView;

    public DocumentHolder(Context context, ViewGroup parent, int layout,
                          OnItemClickListener onItemClickListener,
                              DocumentsAdapter.Environment environment) {
        this(context, inflateLayout(context, parent, layout));

        mEnv = environment;
        mIconHelper = mEnv.getIconHelper();
        multiChoiceHelper = mEnv.getMultiChoiceHelper();
        mDoc = new DocumentInfo();

        mOnItemClickListener = onItemClickListener;
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onItemClick(v, getLayoutPosition());
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMultiChoiceActive()) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        multiChoiceHelper.toggleItemChecked(position, false);
                        updateCheckedState(position);
                    }
                } else {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(view, getLayoutPosition());
                    }
                }
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if ((multiChoiceHelper == null) || isMultiChoiceActive()) {
                    return false;
                }
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    multiChoiceHelper.setItemChecked(position, true, false);
                    updateCheckedState(position);
                }
                return true;
            }
        });
    }

    public DocumentHolder(Context context, View item) {
        super(item);
        mContext = context;

        iconMime = (ImageView) itemView.findViewById(R.id.icon_mime);
        iconThumb = (ImageView) itemView.findViewById(R.id.icon_thumb);
        iconMimeBackground = itemView.findViewById(R.id.icon_mime_background);
        title = (TextView) itemView.findViewById(android.R.id.title);
        icon1 = (ImageView) itemView.findViewById(android.R.id.icon1);
        icon2 = (ImageView) itemView.findViewById(android.R.id.icon2);
        summary = (TextView) itemView.findViewById(android.R.id.summary);
        date = (TextView) itemView.findViewById(R.id.date);
        size = (TextView) itemView.findViewById(R.id.size);
        popupButton = itemView.findViewById(R.id.button_popup);
        line1 = itemView.findViewById(R.id.line1);
        line2 = itemView.findViewById(R.id.line2);
        iconView = itemView.findViewById(android.R.id.icon);
        popupButton.setOnClickListener(this);
        popupButton.setVisibility(isTelevision() ? View.INVISIBLE : View.VISIBLE);
    }

    public void setData(Cursor cursor, int position) {
        super.setData(cursor, position);
        if (multiChoiceHelper != null) {
            updateCheckedState(position);
        }
    }

    public void setSelected(boolean selected, boolean animate) {
        itemView.setActivated(selected);
        itemView.setSelected(selected);
    }

    public void setEnabled(boolean enabled) {
        setEnabledRecursive(itemView, enabled);
    }

    void updateCheckedState(int position) {
        final boolean isChecked = multiChoiceHelper.isItemChecked(position);
        if (itemView instanceof Checkable) {
            ((Checkable) itemView).setChecked(isChecked);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            itemView.setActivated(isChecked);
            setSelected(isChecked, true);
        }
    }

    public boolean isMultiChoiceActive() {
        return (multiChoiceHelper != null) && (multiChoiceHelper.getCheckedItemCount() > 0);
    }

    @Override
    public void onClick(View v) {
        if(null != mOnItemClickListener) {
            mOnItemClickListener.onItemViewClick(v, getLayoutPosition());
        }
    }

    private static void setEnabledRecursive(View v, boolean enabled) {
        if (v == null)
            return;
        if (v.isEnabled() == enabled)
            return;
        v.setEnabled(enabled);

        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setEnabledRecursive(vg.getChildAt(i), enabled);
            }
        }
    }
}
