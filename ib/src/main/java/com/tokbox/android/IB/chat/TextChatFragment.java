package com.tokbox.android.IB.chat;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.tokbox.android.IB.R;
import com.tokbox.android.IB.events.EventUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
* A Fragment for adding and controling the text chat user interface.
*/
public class TextChatFragment extends Fragment {

    private final static String LOG_TAG = "TextChatFragment";

    private Context mContext;
    Handler mHandler;

    private ArrayList<ChatMessage> mMsgsList = new ArrayList<ChatMessage>();
    private MessageAdapter mMessageAdapter;

    private ListView mListView;
    private ImageButton mSendButton;
    private ImageButton mCloseChat;
    private EditText mMsgEditText;
    private TextView mMsgNotificationView;
    private TextView mTopBarTitle;
    private View mMsgDividerView;
    private Typeface mFont;

    private int maxTextLength = 1000; // By default the maximum length is 1000.

    private String senderId;
    private String senderAlias;

    public TextChatFragment() {
        //Init the sender information for the output messages
        this.senderId = UUID.randomUUID().toString();
        this.senderAlias = "me";
        Log.i(LOG_TAG, "senderstuff  " + this.senderId + this.senderAlias);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity.getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.textchat_fragment_layout,
                container, false);
        mFont = EventUtils.getFont(mContext);
        mListView = (ListView) rootView.findViewById(R.id.msgs_list);
        mSendButton = (ImageButton) rootView.findViewById(R.id.send_button);
        mCloseChat = (ImageButton) rootView.findViewById(R.id.close_chat);
        mMsgNotificationView = (TextView) rootView.findViewById(R.id.new_msg_notification);
        mMsgEditText = (EditText) rootView.findViewById(R.id.edit_msg);
        mMsgDividerView = (View) rootView.findViewById(R.id.divider_notification);
        mTopBarTitle = (TextView) rootView.findViewById(R.id.top_bar_title);
        mSendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        mCloseChat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                closeChat();
            }
        });

        mMessageAdapter = new MessageAdapter(getActivity(), R.layout.sent_msg_row_layout, mMsgsList);
        mListView.setAdapter(mMessageAdapter);
        setupFonts();


        /*mMsgEditText.setOnKeyListener(new View.OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });*/

        mMsgEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.i(LOG_TAG, "Done pressed");
                sendMessage();
                return false;
            }
        });


        return rootView;
    }

    private void setupFonts() {

        mMsgEditText.setTypeface(mFont);
        mTopBarTitle.setTypeface(mFont);
    }

    /**
     * An interface for receiving events when a text chat message is ready to send.
     */
    public interface TextChatListener {
        /**
         * Called when a message in the TextChatFragment is ready to send. A message is
         * ready to send when the user clicks the Send button in the TextChatFragment
         * user interface.
         */
        boolean onMessageReadyToSend(ChatMessage msg);

        void hideChat();
    }

    private TextChatListener textChatListener;

    /**
     * Set the object that receives events for this TextChatListener.
     */
    public void setTextChatListener(TextChatListener textChatListener) {
        this.textChatListener = textChatListener;
    }

    /**
     * Set the maximum length of a text chat message (in characters).
     */
    public void setMaxTextLength(int length) {
        maxTextLength = length;
    }

    /**
     * Set the sender alias and the sender ID of the outgoing messages.
     */
    public void setSenderInfo(String senderId, String senderAlias) {
        if ( senderAlias == null || senderId == null ) {
            throw new IllegalArgumentException("The sender alias and the sender id cannot be null");
        }
        this.senderAlias = senderAlias;
        this.senderId = senderId;
    }
    /**
     * Add a message to the TextChatListener received message list.
     */
    public void addMessage(ChatMessage msg) {
        Log.i(LOG_TAG, "New message " + msg.getText() + " is ready to be added.");

        if (msg != null) {

            //check the origin of the message
            if ( msg.getSenderId() != this.senderId ){
                msg.setStatus(ChatMessage.MessageStatus.RECEIVED_MESSAGE);
            }


            //generate message timestamp
            Date date = new Date();
            if (msg.getTimestamp() == 0) {
                msg.setTimestamp(date.getTime());
            }

            mMessageAdapter.add(msg);
            updateScrollPosition();
        }
    }

    // Called when the user clicks the send button.
    private void sendMessage() {
        //checkMessage
        mMsgEditText.setEnabled(false);
        String msgStr = mMsgEditText.getText().toString();
        if (!msgStr.isEmpty()) {

            if ( msgStr.length() > maxTextLength ) {
                showError();
            }
            else {
                ChatMessage myMsg = new ChatMessage(senderId, senderAlias, msgStr, ChatMessage.MessageStatus.SENT_MESSAGE);
                boolean msgError = onMessageReadyToSend(myMsg);

                if (msgError) {
                    Log.d(LOG_TAG, "Error to send the message");
                    showError();

                } else {
                    mMsgEditText.setEnabled(true);
                    mMsgEditText.setFocusable(true);
                    mMsgEditText.setText("");


                    //add the message to the component
                    addMessage(myMsg);

                    updateScrollPosition();
                }

            }

        }
        else{
            mMsgEditText.setEnabled(true);
        }
    }

    private void closeChat() {
        this.textChatListener.hideChat();
    }

    // Add a notification about a new message
    private void showMsgNotification(boolean visible) {
        if (visible) {
            mMsgDividerView.setVisibility(View.VISIBLE);
            mMsgNotificationView.setVisibility(View.VISIBLE);
        } else {
            mMsgNotificationView.setVisibility(View.GONE);
            mMsgDividerView.setVisibility(View.GONE);
        }
    }

    // Update scroll position to the last item in the list
    private void updateScrollPosition() {
        mListView.smoothScrollToPosition(mMessageAdapter.getCount());
        mListView.smoothScrollToPosition(mMessageAdapter.getMessagesList().size() - 1);
    }


    private void showError() {
        mMsgEditText.setEnabled(true);
        mMsgEditText.setFocusable(true);
        mMsgNotificationView.setText("Unable to send message. Retry");
        mMsgNotificationView.setTextColor(Color.RED);
        showMsgNotification(true);
    }

    /**
     * Called when a message in the TextChatFragment is ready to send. A message is
     * ready to send when the user clicks the Send button in the TextChatFragment
     * user interface.
     *
     * If you subclass the TextChatFragment class and implement this method,
     * you do not need to set a TextChatListener.
     */
    private boolean onMessageReadyToSend(ChatMessage msg) {
        if (this.textChatListener != null) {
            Log.d(LOG_TAG, "onMessageReadyToSend");
            return this.textChatListener.onMessageReadyToSend(msg);
        }
        return false;
    }


}
