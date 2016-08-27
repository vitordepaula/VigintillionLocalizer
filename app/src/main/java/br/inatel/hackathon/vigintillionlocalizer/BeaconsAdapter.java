package br.inatel.hackathon.vigintillionlocalizer;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Vector;

/**
 * Created by vitor on 27/08/16.
 */
public class BeaconsAdapter extends RecyclerView.Adapter<BeaconsAdapter.ViewHolder> {

    private Context mContext;
    private Vector<String> mDataSet;
    private SparseBooleanArray mSelectedRows = new SparseBooleanArray();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public CardView mCardView;
        public TextView mTextView;
        public ViewHolder(CardView v) {
            super(v);
            mCardView = v;
            mTextView = (TextView)v.findViewById(R.id.text_view);
        }
    }

    public interface ISelectionCallbacks {
        void onThereAreSelectedItems();
        void onNoMoreSelectedItems();
    }

    private WeakReference<ISelectionCallbacks> mSelectionCallbacks;

    public BeaconsAdapter(Context context, ISelectionCallbacks callbacks) {
        mContext = context;
        mDataSet = new Vector<>(3);
        mDataSet.add("66:55:44:A9:AA:31");
        mDataSet.add("66:55:44:A9:AA:47");
        mDataSet.add("66:55:44:AA:18:11");
        mSelectionCallbacks = new WeakReference<>(callbacks);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beacon_view, parent, false);
        return new ViewHolder((CardView)v);
    }

    private void doCallbackSelected() {
        ISelectionCallbacks cb = mSelectionCallbacks.get();
        if (cb != null)
            cb.onThereAreSelectedItems();
    }

    private void doCallbackNotSelected() {
        ISelectionCallbacks cb = mSelectionCallbacks.get();
        if (cb != null)
            cb.onNoMoreSelectedItems();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mTextView.setText(mDataSet.get(position));
        holder.mCardView.setSelected(mSelectedRows.valueAt(position));
        holder.mCardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mSelectedRows.size() == 0) {
                    int pos = holder.getAdapterPosition();
                    mSelectedRows.put(pos, true);
                    holder.mCardView.setSelected(true);
                    notifyItemChanged(pos);
                    doCallbackSelected();
                }
                return false;
            }
        });
        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedRows.size() > 0) {
                    int pos = holder.getAdapterPosition();
                    if (mSelectedRows.get(pos)) {
                        mSelectedRows.delete(pos);
                        holder.mCardView.setSelected(false);
                        if (mSelectedRows.size() == 0)
                            doCallbackNotSelected();
                    } else {
                        mSelectedRows.put(pos, true);
                        holder.mCardView.setSelected(true);
                    }
                    notifyItemChanged(pos);
                }
            }
        });
        holder.mCardView.setCardBackgroundColor(ContextCompat.getColor(mContext,
                mSelectedRows.get(position)?
                        android.R.color.darker_gray :
                        android.R.color.background_light));
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void deleteSelected() {
        for (int i = mSelectedRows.size() - 1; i >= 0; i--)
            mDataSet.remove(mSelectedRows.keyAt(i));
        mSelectedRows.clear();
        notifyDataSetChanged();
        doCallbackNotSelected();
    }
}
