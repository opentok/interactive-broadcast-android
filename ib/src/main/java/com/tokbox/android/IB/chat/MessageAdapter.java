package com.tokbox.android.IB.chat;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tokbox.android.IB.R;
import com.tokbox.android.IB.events.EventUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


class MessageAdapter extends ArrayAdapter<ChatMessage> {

    private final static int VIEW_TYPE_ROW_SENT = 0;
    private final static int VIEW_TYPE_ROW_RECEIVED = 2;
    private Typeface mFont;


    private List<ChatMessage> messagesList = new ArrayList<>();
    ViewHolder holder;

    private boolean messagesGroup = false;

    private class ViewHolder {
        public TextView aliasText, messageText, timestampText;
        public int viewType;
    }

    public MessageAdapter(Context context, int resource, List<ChatMessage> entities) {
        super(context, resource, entities);
        this.messagesList = entities;
        mFont = EventUtils.getFont(context);
    }


    public List<ChatMessage> getMessagesList() {
        return messagesList;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        if (messagesList.get(position).getStatus().equals(ChatMessage.MessageStatus.SENT_MESSAGE)) {
            return VIEW_TYPE_ROW_SENT;
        } else {
            if (messagesList.get(position).getStatus().equals(ChatMessage.MessageStatus.RECEIVED_MESSAGE)) {
                return VIEW_TYPE_ROW_RECEIVED;
            }
        }

        return VIEW_TYPE_ROW_SENT; //by default
    }

    private boolean checkMessageGroup(int position) {
        //check timestamp for message to group messages (multiple messages sent within a 2 minutes time limit)
        ChatMessage lastMsg = null;
        ChatMessage currentMsg = null;

        List<ChatMessage> myList = new ArrayList<>();
        myList = this.getMessagesList();
        if (myList.size() > 1 && position > 0) {
            lastMsg = messagesList.get(position - 1);
            currentMsg = messagesList.get(position);
            if (lastMsg != null && currentMsg != null && lastMsg.getSenderId().equals(currentMsg.getSenderId())) {
                //check time
                if (checkTimeMsg(lastMsg.getTimestamp(), currentMsg.getTimestamp())) {
                    return true;
                }
            }
        }
        return false;
    }

    //Check the time between the current new message and the last added message
    private boolean checkTimeMsg(long lastMsgTime, long newMsgTime) {
        return (lastMsgTime - newMsgTime <= TimeUnit.MINUTES.toMillis(2));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        ChatMessage message = this.messagesList.get(position);

        holder = new ViewHolder();
        int type = VIEW_TYPE_ROW_SENT;
        type = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();

            switch (type) {
                case VIEW_TYPE_ROW_SENT:
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.sent_msg_row_layout, parent, false);
                    holder.viewType = VIEW_TYPE_ROW_SENT;
                    break;
                case VIEW_TYPE_ROW_RECEIVED:
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.received_msg_row_layout, parent, false);
                    holder.viewType = VIEW_TYPE_ROW_RECEIVED;
                    break;
            }
            holder.aliasText = (TextView) convertView.findViewById(R.id.name);
            holder.messageText = (TextView) convertView.findViewById(R.id.msg_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //Set fonts
        holder.messageText.setTypeface(mFont);
        holder.aliasText.setTypeface(mFont);

        //msg alias
        holder.aliasText.setText(message.getSenderAlias()+":");
        //msg txt
        holder.messageText.setText(message.getText());

        return convertView;
    }

}
