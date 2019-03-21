package com.agiledev.agiledevapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

import static java.lang.Math.min;

public class TvShowFullScreenDialog extends DialogFragment {

    public static String TAG = "TvShowFullScreenDialog";
    public String id, poster_path;
    public FullTvShowDetails tvshowDetails;
    public TextView toolbarTitle;
    public Toolbar toolbar;
    public ImageView trailerVideoImage, trailerVideoPlayImage;
    NestedScrollView pageContent;
    TvShowCastAdapter adapter;
    RecyclerView recyclerView;
    private FragmentActivity mContext;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static TvShowFullScreenDialog newInstance(String id) {
        TvShowFullScreenDialog fragment = new TvShowFullScreenDialog();
        Bundle args = new Bundle();
        args.putString("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.tvshow_dialog_layout, container, false);

        pageContent = view.findViewById(R.id.tvshowContent);

        toolbar = view.findViewById(R.id.tvshowDialogTool_Bar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        recyclerView = view.findViewById(R.id.tvshowcast_recycler_view);


        trailerVideoImage = view.findViewById(R.id.tvshowTrailerImage);
        trailerVideoPlayImage = view.findViewById(R.id.tvshowTrailerPlayIcon);

        //TODO add floating action button to track tvshows
        sharedPref = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        this.id = getArguments().getString("id", "No Title Found");

        FloatingActionButton fab = view.findViewById(R.id.fabTrackMovie);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackTV();
            }
        });

        getTvShowDetails(view);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    protected synchronized void getTvShowDetails(final View view) {
        TmdbClient.getTvShowInfo(id, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                tvshowDetails = new Gson().fromJson(response.toString(), FullTvShowDetails.class);
                if (tvshowDetails == null)
                    return;
                Uri uri = Uri.parse("https://image.tmdb.org/t/p/w1280" + tvshowDetails.getBackdrop_path());

                Glide.with(TvShowFullScreenDialog.this).load(uri).listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        trailerVideoPlayImage.setVisibility(View.VISIBLE);

                        final FullTvShowDetails.Video tempVideo = tvshowDetails.getVideos().get(0);

                        trailerVideoPlayImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                openYoutubeVideo(getContext(), tempVideo.getKey());
                            }
                        });

                        view.findViewById(R.id.tvshowLoadingSpinner).setVisibility(View.GONE);
                        pageContent.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).dontAnimate().into(trailerVideoImage);

                RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getContext(), 1);
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());

                TextView tvshowName = view.findViewById(R.id.tvshowTitle);
                TextView tvshowPlot = view.findViewById(R.id.tvshowInfoPlot);
                TextView tvshowReleaseDate = view.findViewById(R.id.tvshowInfoReleaseDate);
                TextView tvshowNextEpisode = view.findViewById(R.id.tvshowNextEp);
                TextView tvshowGenres = view.findViewById(R.id.tvshowInfoGenres);
                Button tvshowCastMore = view.findViewById(R.id.tvshowInfoCastMore);

                String nextEpString;
                String firstReleased = getResources().getString(R.string.first_released) + " <font color='#ffffff'>" + tvshowDetails.getFirst_air_date() + "</font>";
                if(tvshowDetails.getNext_episode_to_air() == null)
                {
                    nextEpString = getResources().getString(R.string.nextep) + " <font color='#ffffff'>N/A</font>";
                }
                else
                {
                    nextEpString = getResources().getString(R.string.nextep) + " <font color='#ffffff'>" + tvshowDetails.getNext_episode_to_air().air_date + "</font>";
                }

                //TODO implement runtime per episode
                //int runtimeMins = tvshowDetails.getRuntime();
                //int hours = runtimeMins / 60, minutes = runtimeMins % 60;
                //String runtimeString = String.format("%s %s", getResources().getString(R.string.runtime), String.format(" <font color='#ffffff'>%s</font>", String.format("%dhrs %02dmins", hours, minutes)));

                tvshowName.setText(tvshowDetails.getName());
                tvshowPlot.setText(tvshowDetails.getOverview());

                if (tvshowDetails.getNext_episode_to_air() == null)
                {
                    tvshowNextEpisode.setText(" ");
                }
                else
                {
                    tvshowNextEpisode.setText(tvshowDetails.getNext_episode_to_air().air_date);

                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    tvshowReleaseDate.setText(Html.fromHtml(firstReleased, Html.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
                    tvshowNextEpisode.setText(Html.fromHtml(nextEpString, Html.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
                } else {
                    tvshowReleaseDate.setText(Html.fromHtml(firstReleased), TextView.BufferType.SPANNABLE);
                    tvshowNextEpisode.setText(Html.fromHtml(nextEpString), TextView.BufferType.SPANNABLE);
                }

                tvshowGenres.setText(tvshowDetails.getGenresString());
                addCastToLayout(tvshowDetails.getCast(), getActivity().getSupportFragmentManager());
                tvshowCastMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewMoreCast();
                    }
                });
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e(String.valueOf(statusCode), throwable.getMessage());
            }
        });
    }

    public void openYoutubeVideo(Context context, String id) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            context.startActivity(appIntent);
        } catch (ActivityNotFoundException e) {
            context.startActivity(webIntent);
        }
    }

    public void viewMoreCast()
    {
        FullCastDialog dialog = FullCastDialog.newInstance(id, FullCastDialog.mediatype.TV);
        dialog.show(getActivity().getFragmentManager(), FullCastDialog.TAG);
    }

    public void addCastToLayout(ArrayList<FullTvShowDetails.Cast> castList, FragmentManager fragmentManager) {
        Context mContext = getContext();
        List<FullTvShowDetails.Cast> top3Cast = new ArrayList<>();

        for (int i = 0; i < min(3,castList.size()); i++) {
            top3Cast.add(castList.get(i));
        }

        adapter = new TvShowCastAdapter(mContext, top3Cast, fragmentManager);
        recyclerView.setAdapter(adapter);
    }

    public void trackTV() {
        boolean alreadyTracked = false;
        if (Globals.trackedTVContains(id))
            alreadyTracked = true;

        final AlertDialog dialog = SimpleDialog.create(DialogOption.YesCancel, getContext(), "Track TvShow", "Are you sure you want to track this TvShow?");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final DocumentReference ref = FirebaseFirestore.getInstance().collection("TrackedTV").document(sharedPref.getString(getString(R.string.prefs_loggedin_username), null));
                ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            Map<String, Object> trackedTV = new HashMap<>();
                            Map<String, Object> trackData = new HashMap<>();
                            trackData.put("date", new Date());
                            trackData.put("poster_path", poster_path);
                            trackedTV.put(id, trackData);
                            if (!doc.exists()) {
                                ref.set(trackedTV);
                            } else {
                                ref.update(trackedTV);
                            }
                            Globals.trackedTV TV = new Globals.trackedTV();
                            TV.id = id;
                            TV.date = new Date();
                            TV.poster_path = poster_path;
                            Globals.addToTrackedTvShows(TV);
                            Globals.sortTrackedTvShows();
                        }
                    }
                });
                Toast.makeText(getContext(), "Tv Show tracked!", Toast.LENGTH_LONG).show();
            }
        });

        if (alreadyTracked) {
            dialog.setTitle("Untrack TvShow");
            dialog.setMessage("Are you sure you want to untrack this TvShow?");
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    DocumentReference ref = FirebaseFirestore.getInstance().collection("TrackedMovies").document(sharedPref.getString(getString(R.string.prefs_loggedin_username), null));
                    ref.update(id, FieldValue.delete());

                    Globals.removeFromTrackedMovies(id);

                    Toast.makeText(getContext(), "Movie untracked!", Toast.LENGTH_LONG).show();
                }
            });
        }

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

}