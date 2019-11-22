package com.example.friendlychat;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MessageAdapter extends ArrayAdapter<FriendlyMessage> {
    public MessageAdapter(Context context, int resource, List<FriendlyMessage> objects) {
        super(context, resource, objects);
    }

    static String user2 ="";

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FriendlyMessage message = getItem(position);


        if (convertView == null) {
                convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
        }

        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        LinearLayout txtNameLinearLayout = convertView.findViewById(R.id.txt_name_id);
        LinearLayout linearLayout = convertView.findViewById(R.id.message_layout);

        authorTextView.setText(message.getName());
        if ((message.getName()).equals(user2)) {
            txtNameLinearLayout.setBackgroundResource(R.drawable.rounded_corners2);
            linearLayout.setHorizontalGravity(Gravity.RIGHT);
        }
        else if(!(message.getName()).equals(user2))
        {
            txtNameLinearLayout.setBackgroundResource(R.drawable.rounded_corners);
            linearLayout.setHorizontalGravity(Gravity.LEFT);
        }

        boolean isPhoto = message.getPhotoUrl() != null;
        if (isPhoto) {
            messageTextView.setVisibility(View.GONE);
            photoImageView.setVisibility(View.VISIBLE);
            Glide.with(photoImageView.getContext())
                    .load(message.getPhotoUrl())
                    .into(photoImageView);
        } else {
            messageTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            messageTextView.setText(message.getText());
        }


        return convertView;
    }
    public static void setNameMessageAdapter(String mUser2){
        user2 = mUser2;
    }


}
