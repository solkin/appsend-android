package com.tomclaw.appteka.main.adapter.holder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tomclaw.appteka.R;
import com.tomclaw.appteka.main.adapter.BaseItemAdapter;
import com.tomclaw.appteka.main.item.CouchItem;

/**
 * Created by ivsolkin on 06.09.16.
 */
public class CouchItemHolder extends AbstractItemHolder<CouchItem> {

    private View itemView;
    private TextView couchText;
    private View divider;
    private ViewGroup couchButtons;

    public CouchItemHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        couchText = itemView.findViewById(R.id.couch_text);
        divider = itemView.findViewById(R.id.divider);
        couchButtons = itemView.findViewById(R.id.couch_buttons);
    }

    public void bind(Context context, final CouchItem item, final boolean isLast, final BaseItemAdapter.BaseItemClickListener<CouchItem> listener) {
        if (listener != null) {
            divider.setVisibility(View.VISIBLE);
            couchButtons.setVisibility(View.VISIBLE);
            couchButtons.removeAllViews();
            for (final CouchItem.CouchButton button : item.getButtons()) {
                LayoutInflater inflater = LayoutInflater.from(context);
                Button couchButton = (Button) inflater.inflate(R.layout.couch_button, couchButtons, false);
                couchButton.setText(button.getLabel());
                couchButton.setOnClickListener(v -> listener.onActionClicked(item, button.getAction()));
                couchButtons.addView(couchButton);
            }
        } else {
            divider.setVisibility(View.GONE);
            couchButtons.setVisibility(View.GONE);
            couchButtons.removeAllViews();
        }
        couchText.setText(item.getCouchText());
    }
}
