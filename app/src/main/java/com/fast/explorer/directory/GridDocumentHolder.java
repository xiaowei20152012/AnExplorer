package com.fast.explorer.directory;

import android.content.Context;
import android.view.ViewGroup;

import com.fast.explorer.R;
import com.fast.explorer.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import com.fast.explorer.directory.DocumentsAdapter.Environment;

public class GridDocumentHolder extends ListDocumentHolder {

    public GridDocumentHolder(Context context, ViewGroup parent,
                              OnItemClickListener onItemClickListener, Environment environment) {
        super(context, parent, R.layout.item_doc_grid, onItemClickListener, environment);
    }

}
