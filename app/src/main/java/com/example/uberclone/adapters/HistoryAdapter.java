package com.example.uberclone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uberclone.R;
import com.example.uberclone.models.HistoryBooking;
import com.example.uberclone.providers.AuthProvider;
import com.example.uberclone.providers.ClientsProvider;
import com.example.uberclone.providers.DriversProvider;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryAdapter extends FirestoreRecyclerAdapter <HistoryBooking, HistoryAdapter.ViewHolder>{

    Context context;
    AuthProvider mAuthProvider;
    DriversProvider mDriverProvider;
    ClientsProvider mClientProvider;

    public HistoryAdapter(FirestoreRecyclerOptions<HistoryBooking>options, Context context){
        super(options);
        this.context = context;
        mAuthProvider = new AuthProvider();
        mDriverProvider = new DriversProvider();
        mClientProvider = new ClientsProvider();
    }


    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // instancia del cardview que se va a usar
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_history_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull HistoryBooking historyBooking) {

        holder.mTxtVOrigin.setText(historyBooking.getOrigin());
        holder.mTxtVDestination.setText(historyBooking.getDestination());
        // dando formato a la fecha de campo timestamp
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        String date = format.format(historyBooking.getTimestamp());
        holder.mTxtVDate.setText(date);

        // en caso de ser cliente muestra info de driver
        if (mAuthProvider.getUid().equals(historyBooking.getIdClient())){

            holder.mTxtVCalification.setText(String.valueOf(historyBooking.getCalificationDriver()));

            mDriverProvider.getUserById(historyBooking.getIdDriver()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {

                    holder.mTxtVName.setText(documentSnapshot.getString("username"));
                    if (documentSnapshot.contains("imageProfile")){
                        String imgUrl = documentSnapshot.getString("imageProfile");
                        if (imgUrl != null && !imgUrl.isEmpty()){
                            Picasso.get().load(documentSnapshot.getString("imageProfile")).into(holder.mImgCImage);
                        }
                    }

                }
            });
            // en caso de ser driver muestra info de cliente
        }else if(mAuthProvider.getUid().equals(historyBooking.getIdDriver())){

            holder.mTxtVCalification.setText(String.valueOf(historyBooking.getCalificationClient()));

            mClientProvider.getUserById(historyBooking.getIdClient()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {

                    holder.mTxtVName.setText(documentSnapshot.getString("username"));
                    if (documentSnapshot.contains("imageProfile")){
                        String imgUrl = documentSnapshot.getString("imageProfile");
                        if (imgUrl != null && !imgUrl.isEmpty()){
                            Picasso.get().load(documentSnapshot.getString("imageProfile")).into(holder.mImgCImage);
                        }
                    }
                }
            });
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView mTxtVName, mTxtVCalification, mTxtVPay,
                mTxtVOrigin, mTxtVDestination, mTxtVDate;

        CircleImageView mImgCImage;

        public ViewHolder(View itemView){
            super(itemView);

            mTxtVName = itemView.findViewById(R.id.txtVHistoryUsername);
            mTxtVCalification = itemView.findViewById(R.id.txtVHistoryCalification);
            mTxtVPay = itemView.findViewById(R.id.txtVHistoryPay);
            mTxtVDate = itemView.findViewById(R.id.txtVHistoryDate);
            mTxtVOrigin = itemView.findViewById(R.id.txtVHistoryOrigin);
            mTxtVDestination = itemView.findViewById(R.id.txtVHistoryDestination);

            mImgCImage = itemView.findViewById(R.id.imgCHistoryProfile);
        }
    }
}
