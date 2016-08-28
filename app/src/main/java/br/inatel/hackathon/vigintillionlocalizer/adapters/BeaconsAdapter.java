package br.inatel.hackathon.vigintillionlocalizer.adapters;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import br.inatel.hackathon.vigintillionlocalizer.R;
import br.inatel.hackathon.vigintillionlocalizer.database.DB;

/**
 * Created by vitor on 27/08/16.
 */
public class BeaconsAdapter extends RecyclerView.Adapter<BeaconsAdapter.ViewHolder> {

    private Context mContext;
    private final Vector<DB.TrackedBeacon> mDataSet;
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

    public interface IMultiSelectionCallbacks {
        void onThereAreSelectedItems();
        void onNoMoreSelectedItems();
    }

    public interface IItemClickCallback {
        void onItemClick(String data);
    }

    private WeakReference<IMultiSelectionCallbacks> mSelectionCallbacks;
    private WeakReference<IItemClickCallback> mItemClickCallback;

    public BeaconsAdapter(Context context,
                          IMultiSelectionCallbacks multiSelectionCallbacks,
                          IItemClickCallback itemClickCallback) {
        mContext = context;
        mDataSet = new Vector<>();
        mSelectionCallbacks = (multiSelectionCallbacks == null ? null : new WeakReference<>(multiSelectionCallbacks));
        mItemClickCallback = (itemClickCallback == null ? null : new WeakReference<>(itemClickCallback));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beacon_view, parent, false);
        return new ViewHolder((CardView)v);
    }

    private void doCallbackSelected() {
        IMultiSelectionCallbacks cb = mSelectionCallbacks.get();
        if (cb != null)
            cb.onThereAreSelectedItems();
    }

    private void doCallbackNotSelected() {
        IMultiSelectionCallbacks cb = mSelectionCallbacks.get();
        if (cb != null)
            cb.onNoMoreSelectedItems();
    }

    private void doCallbackItemClicked(String data) {
        IItemClickCallback cb = mItemClickCallback.get();
        if (cb != null)
            cb.onItemClick(data);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mTextView.setText(mDataSet.get(position).beacon_id);
        if (mSelectionCallbacks != null) {
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
                    } else doCallbackItemClicked(holder.mTextView.getText().toString());
                }
            });
            holder.mCardView.setCardBackgroundColor(ContextCompat.getColor(mContext,
                    mSelectedRows.get(position) ?
                            android.R.color.darker_gray :
                            colorIdToRes(mDataSet.get(position).color_id)));
        } else if (mItemClickCallback != null) {
            holder.mCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doCallbackItemClicked(holder.mTextView.getText().toString());
                }
            });
        }
    }

    public static int colorIdToRes(int color_id) {
        switch (color_id) {
            case 1: return android.R.color.holo_green_dark;
            case 2: return android.R.color.holo_red_light;
            case 3: return android.R.color.holo_blue_dark;
            case 4: return android.R.color.holo_orange_light;
            case 5: return android.R.color.holo_purple;
            default: return android.R.color.background_light;
        }
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

    public List<String> getSelected() {
        List<String> result = new LinkedList<>();
        for (int i = 0; i< mSelectedRows.size(); i++)
            result.add(mDataSet.get(mSelectedRows.keyAt(i)).beacon_id);
        return result;
    }

    public final List<DB.TrackedBeacon> getDataSet() { return mDataSet; }
}
