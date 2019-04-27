package com.agiledev.agiledevapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MediaTracking {

    public static enum Media {
        MOVIE,
        TV
    }

    static String title, poster_path;
    private static int runtime;
    private static Map<FullTvShowDetails.season, Boolean> flags = new HashMap<>();
    static final Map<String, Object> trackedSeasons = new HashMap<>();

    public static AlertDialog trackMovie(final Context mContext, final Activity mActivity, final String username, final String id, final MaterialProgressBar progressBar) {
        final AlertDialog dialog = SimpleDialog.create(DialogOption.YesCancel, mContext, "Track Movie", "Are you sure you want to track this movie?");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (progressBar != null)
                    progressBar.setVisibility(View.VISIBLE);
                final DocumentReference movieRef = FirebaseFirestore.getInstance().collection("TrackedMovies").document(username);
                movieRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            final DocumentSnapshot doc = task.getResult();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    SyncHttpClient client = new SyncHttpClient();
                                    String url = "https://api.themoviedb.org/3/" + "movie/" + id + "?api_key=" + TmdbClient.key;
                                    client.get(url, null, new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            FullMovieDetails movieDetails = new Gson().fromJson(response.toString(), FullMovieDetails.class);
                                            if (movieDetails == null)
                                                return;
                                            title = movieDetails.getTitle();
                                            poster_path = movieDetails.getPoster_path();
                                            final ArrayList<FullMovieDetails.Genre> genreList = movieDetails.getGenres();
                                            runtime = movieDetails.getRuntime();

                                            Map<String, Object> trackedMovie = new HashMap<>();
                                            Map<String, Object> trackData = new HashMap<>();
                                            trackData.put("date", new Date());
                                            trackData.put("name", title);
                                            trackData.put("poster_path", poster_path);

                                            Map<String, String> genres = new HashMap<>();
                                            for (FullMovieDetails.Genre g : genreList) {
                                                genres.put(String.valueOf(g.id), g.name);
                                            }
                                            trackData.put("genres", genres);

                                            trackedMovie.put(id, trackData);
                                            if (!doc.exists()) {
                                                movieRef.set(trackedMovie);
                                            } else {
                                                movieRef.update(trackedMovie);
                                            }
                                            Globals.trackedMovie movie = new Globals.trackedMovie();
                                            movie.id = id;
                                            movie.date = new Date();
                                            movie.poster_path = poster_path;
                                            movie.name = title;
                                            for (HashMap.Entry<String, String> e : genres.entrySet()) {
                                                movie.genres.put(Integer.parseInt(e.getKey()), e.getValue());
                                            }
                                            Globals.addToTrackedMovies(movie);
                                            Globals.sortTrackedMovies();

                                            final DocumentReference userRef = FirebaseFirestore.getInstance().collection("UserDetails").document(username);
                                            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        DocumentSnapshot doc = task.getResult();
                                                        Map<String, Object> userData = new HashMap<>();
                                                        userData.put("timeWatched", Integer.valueOf(doc.get("timeWatched").toString()) + runtime);
                                                        Map<String, Long> userGenres = (Map<String, Long>)doc.get("genresWatched");
                                                        for (FullMovieDetails.Genre g : genreList) {
                                                            if (userGenres.containsKey(g.name)) {
                                                                userGenres.put(g.name, userGenres.get(g.name) + 1);
                                                            } else {
                                                                userGenres.put(g.name, 1L);
                                                            }
                                                        }
                                                        userData.put("genresWatched", userGenres);
                                                        userRef.update(userData);

                                                        mActivity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                if (progressBar != null)
                                                                    progressBar.setVisibility(View.GONE);
                                                                Toast.makeText(mContext.getApplicationContext(), "Movie Tracked", Toast.LENGTH_LONG).show();
                                                            }
                                                        });
                                                    }
                                                }
                                            });

                                        }
                                    });
                                }
                            }).start();

                        }
                    }
                });
            }
        });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
            }
        });

        return dialog;
    }

    public static AlertDialog trackTV(Context mContext, Activity mActivity, String type, String username, String seriesId, @Nullable Integer seasonNum, @Nullable Integer episodeNum, MaterialProgressBar progressBar) {
        switch (type) {
            case "series":
                return trackTVShow(mContext, mActivity, username, seriesId, progressBar);
            case "season":
                if (seasonNum != null)
                    return trackTVSeason(mContext, mActivity, username, seriesId, seasonNum, progressBar);
                else
                    Log.e("Tracking Season", "SeasonNum was null.");
                break;
            case "episode":
                if (seasonNum != null && episodeNum != null)
                    return trackTVEpisode(mContext, mActivity, username, seriesId, seasonNum, episodeNum, progressBar);
                else
                    Log.e("Tracking Episode", "SeasonNum or EpisodeNum was null.");
                break;
            default:
                Log.e("Tracking", "Type was invalid.");
        }
        return null;
    }

    private static AlertDialog trackTVEpisode(final Context mContext, final Activity mActivity, final String username, final String seriesId, final int seasonNum, final int episodeNum, final MaterialProgressBar progressBar) {
        final AlertDialog dialog = SimpleDialog.create(DialogOption.YesCancel, mContext, "Track TV", "Are you sure you want to track this episode?");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progressBar.setVisibility(View.VISIBLE);
                final DocumentReference ref = FirebaseFirestore.getInstance().collection("TrackedTV").document(username);
                ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            final DocumentSnapshot doc = task.getResult();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final SyncHttpClient client = new SyncHttpClient();
                                    String url = "https://api.themoviedb.org/3/" + "tv/" + seriesId + "/season/" + seasonNum + "/episode/" + episodeNum + "?api_key=" + TmdbClient.key;
                                    client.get(url, null, new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            final FullTvEpisodeDetails episode = new Gson().fromJson(response.toString(), FullTvEpisodeDetails.class);
                                            if (episode == null)
                                                return;
                                            final Map<String, Object> trackedTV = new HashMap<>();
                                            final Map<String, Object> trackedSeason = new HashMap<>();
                                            final Map<String, Object> trackedEpisode = new HashMap<>();
                                            final Map<String, Object> trackData = new HashMap<>();

                                            trackData.put("date", new Date());
                                            trackData.put("episodeName", episode.getName());
                                            trackData.put("episodeNum", episode.getEpisode_number());

                                            String url = "https://api.themoviedb.org/3/" + "tv/" + seriesId + "?api_key=" + TmdbClient.key;
                                            client.get(url, null, new JsonHttpResponseHandler() {
                                                @Override
                                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                                    final FullTvShowDetails show = new Gson().fromJson(response.toString(), FullTvShowDetails.class);
                                                    if (show == null)
                                                        return;

                                                    trackData.put("id", episode.getId());
                                                    trackData.put("seriesName", show.getName());

                                                    String episodeString = "Episode " + episode.getEpisode_number();
                                                    trackedEpisode.put(episodeString, trackData);
                                                    String seasonString = "Season " + episode.getSeason_number();
                                                    trackedSeason.put(seasonString, trackedEpisode);
                                                    trackedSeason.put("name", show.getName());
                                                    trackedSeason.put("poster_path", show.getPoster_path());
                                                    Date date = new Date();
                                                    trackedSeason.put("lastWatched", date);
                                                    ArrayList<FullTvShowDetails.Genre> genreList = show.getGenres();
                                                    Map<String, String> genres = new HashMap<>();
                                                    for (FullTvShowDetails.Genre g : genreList) {
                                                        genres.put(String.valueOf(g.id), g.name);
                                                    }
                                                    trackedSeason.put("genres", genres);
                                                    trackedTV.put(show.getId(), trackedSeason);

                                                    Globals.trackedTV tv = null;
                                                    if (Globals.trackedTVContains(show.getId())) {
                                                        for (Globals.trackedTV t : Globals.getTrackedTvShows()) {
                                                            if (t.id.equals(show.getId())) {
                                                                tv = t;
                                                            }
                                                        }
                                                    } else {
                                                        tv = new Globals.trackedTV();
                                                    }
                                                    tv.date = date;
                                                    tv.id = show.getId();
                                                    tv.name = show.getName();
                                                    tv.poster_path = show.getPoster_path();
                                                    SparseArray<String> trackedGenres = new SparseArray<>();
                                                    for (FullTvShowDetails.Genre g : (ArrayList<FullTvShowDetails.Genre>)genreList) {
                                                        trackedGenres.put(g.id, g.name);
                                                    }
                                                    tv.genres = trackedGenres;

                                                    Globals.trackedTV.Episode e = new Globals.trackedTV.Episode();
                                                    e.date = date;
                                                    e.episodeName = episode.getName();
                                                    e.id = episode.getId();
                                                    e.seriesName = show.getName();
                                                    e.episodeNum = episode.getEpisode_number();
                                                    tv.addEpisode(e);


                                                    if (!doc.exists()) {
                                                        ref.set(trackedTV);
                                                    } else {
                                                        if (doc.contains(show.getId())) { // Show exists
                                                            if (((HashMap)doc.get(show.getId())).containsKey(seasonString)) { // Season exists
                                                                if (!(((HashMap)((HashMap)doc.get(show.getId())).get(seasonString)).containsKey(episodeString))) { // Episode doesn't exist
                                                                    HashMap currentEps = ((HashMap)((HashMap)doc.get(show.getId())).get(seasonString));
                                                                    currentEps.put(episodeString, trackedEpisode.get(episodeString));
                                                                    ref.update(show.getId() + "." + seasonString, currentEps);
                                                                }
                                                            } else {
                                                                HashMap currentSeasons = ((HashMap)doc.get(show.getId()));
                                                                currentSeasons.put(seasonString, trackedSeason.get(seasonString));
                                                                ref.update(show.getId(), currentSeasons);
                                                            }
                                                        } else {
                                                            ref.update(trackedTV);
                                                        }
                                                    }
                                                    mActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            progressBar.setVisibility(View.GONE);
                                                            Toast.makeText(mContext.getApplicationContext(), "TV Episode Tracked!", Snackbar.LENGTH_LONG).show();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            }).start();

                        }
                    }
                });
            }
        });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
            }
        });

        return dialog;
    }

    private static AlertDialog trackTVSeason(final Context mContext, final Activity mActivity, final String username, final String seriesId, final int seasonNum, final MaterialProgressBar progressBar) {
        final AlertDialog dialog = SimpleDialog.create(DialogOption.YesCancel, mContext, "Track TV", "Are you sure you want to track this entire season?");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progressBar.setVisibility(View.VISIBLE);
                final DocumentReference ref = FirebaseFirestore.getInstance().collection("TrackedTV").document(username);
                ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            final DocumentSnapshot doc = task.getResult();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final SyncHttpClient client = new SyncHttpClient();
                                    String url = "https://api.themoviedb.org/3/" + "tv/"+ seriesId + "/season/" + seasonNum + "?api_key=" + TmdbClient.key;
                                    client.get(url, null, new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            final FullTvSeasonDetails season = new Gson().fromJson(response.toString(), FullTvSeasonDetails.class);
                                            if (season == null)
                                                return;

                                            String url = "https://api.themoviedb.org/3/" + "tv/" + seriesId + "?api_key=" + TmdbClient.key;
                                            client.get(url, null, new JsonHttpResponseHandler() {
                                                @Override
                                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                                    final FullTvShowDetails show = new Gson().fromJson(response.toString(), FullTvShowDetails.class);
                                                    if (show == null)
                                                        return;

                                                    ArrayList<FullTvSeasonDetails.Episode> episodes = season.getEpisodes();
                                                    final Map<String, Object> trackedTV = new HashMap<>();
                                                    final Map<String, Object> trackedSeason = new HashMap<>();
                                                    final Map<String, Object> trackedEpisode = new HashMap<>();

                                                    Date date = new Date();

                                                    Globals.trackedTV tv = null;
                                                    if (Globals.trackedTVContains(show.getId())) {
                                                        for (Globals.trackedTV t : Globals.getTrackedTvShows()) {
                                                            if (t.id.equals(show.getId())) {
                                                                tv = t;
                                                            }
                                                        }
                                                    } else {
                                                        tv = new Globals.trackedTV();
                                                    }
                                                    tv.date = date;
                                                    tv.id = show.getId();
                                                    tv.name = show.getName();
                                                    tv.poster_path = show.getPoster_path();
                                                    SparseArray<String> trackedGenres = new SparseArray<>();
                                                    ArrayList<FullTvShowDetails.Genre> genreList = show.getGenres();
                                                    for (FullTvShowDetails.Genre g : genreList) {
                                                        trackedGenres.put(g.id, g.name);
                                                    }
                                                    tv.genres = trackedGenres;

                                                    for (final FullTvSeasonDetails.Episode e : episodes) {
                                                        final Map<String, Object> trackData = new HashMap<>();

                                                        trackData.put("date", date);
                                                        trackData.put("episodeName", e.getName());
                                                        trackData.put("episodeNum", e.getEpisode_number());

                                                        trackData.put("id", e.getId());
                                                        trackData.put("seriesName", show.getName());

                                                        String episodeString = "Episode " + e.getEpisode_number();
                                                        trackedEpisode.put(episodeString, trackData);

                                                        Globals.trackedTV.Episode ep = new Globals.trackedTV.Episode();
                                                        ep.date = date;
                                                        ep.episodeName = e.getName();
                                                        ep.id = e.getId();
                                                        ep.seriesName = show.getName();
                                                        ep.episodeNum = e.getEpisode_number();
                                                        tv.addEpisode(ep);
                                                    }

                                                    String seasonString = "Season " + seasonNum;
                                                    trackedSeason.put(seasonString, trackedEpisode);
                                                    trackedSeason.put("name", show.getName());
                                                    trackedSeason.put("poster_path", show.getPoster_path());
                                                    trackedSeason.put("lastWatched", date);
                                                    genreList = show.getGenres();
                                                    Map<String, String> genres = new HashMap<>();
                                                    for (FullTvShowDetails.Genre g : genreList) {
                                                        genres.put(String.valueOf(g.id), g.name);
                                                    }
                                                    trackedSeason.put("genres", genres);
                                                    trackedTV.put(show.getId(), trackedSeason);

                                                    if (!doc.exists()) {
                                                        ref.set(trackedTV);
                                                    } else {
                                                        if (doc.contains(show.getId())) { // Show exists
                                                            if (!(((HashMap)doc.get(show.getId())).containsKey(seasonString))) { // Season doesn't exist
                                                                HashMap<String, Object> currentSeasons = ((HashMap<String, Object>)doc.get(show.getId()));
                                                                currentSeasons.put(seasonString, trackedSeason.get(seasonString));
                                                                ref.update(show.getId(), currentSeasons);
                                                            } else { //Season exists
                                                                HashMap<String, Object> currentSeasons = ((HashMap<String, Object>)doc.get(show.getId()));
                                                                HashMap<String, Object> currentSeason = (HashMap<String, Object>)currentSeasons.get(seasonString);
                                                                for (Map.Entry<String, Object> entry : trackedEpisode.entrySet()) {
                                                                    if (!currentSeason.containsKey(entry.getKey())) {
                                                                        currentSeason.put(entry.getKey(), entry.getValue());
                                                                    }
                                                                }
                                                                currentSeasons.put(seasonString, currentSeason);
                                                                ref.update(show.getId(), currentSeasons);
                                                            }
                                                        } else {
                                                            ref.update(trackedTV);
                                                        }
                                                    }
                                                    mActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            progressBar.setVisibility(View.GONE);
                                                            Toast.makeText(mContext.getApplicationContext(), "TV Season Tracked!", Snackbar.LENGTH_LONG).show();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            }).start();

                        }
                    }
                });
            }
        });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
            }
        });

        return dialog;
    }

    private static synchronized AlertDialog trackTVShow(final Context mContext, final Activity mActivity, final String username, final String seriesId, final MaterialProgressBar progressBar) {
        final AlertDialog dialog = SimpleDialog.create(DialogOption.YesCancel, mContext, "Track TV", "Are you sure you want to track this entire tv show?");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progressBar.setVisibility(View.VISIBLE);
                final DocumentReference ref = FirebaseFirestore.getInstance().collection("TrackedTV").document(username);
                ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            final DocumentSnapshot doc = task.getResult();
                            TmdbClient.getFullTvShowDetails(seriesId, null, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    final FullTvShowDetails show = new Gson().fromJson(response.toString(), FullTvShowDetails.class);
                                    if (show == null)
                                        return;

                                    final Map<String, Object> trackedTV = new HashMap<>();

                                    final ArrayList<FullTvShowDetails.Genre> genreList = show.getGenres();

                                    for (final FullTvShowDetails.season s : show.getSeason()) {
                                        flags.put(s, false);
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Map.Entry<String, Object> entry = trackSeasonForShowTrack(seriesId, s, show, genreList);
                                                trackedSeasons.put(entry.getKey(), entry.getValue());
                                            }
                                        }).start();
                                    }

                                    final Thread flagCheck = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            while (checkForNotCompletes(flags)) {

                                            }
                                            trackedSeasons.put("name", show.getName());
                                            trackedSeasons.put("poster_path", show.getPoster_path());
                                            trackedSeasons.put("lastWatched", new Date());
                                            Map<String, String> genres = new HashMap<>();
                                            for (FullTvShowDetails.Genre g : genreList) {
                                                genres.put(String.valueOf(g.id), g.name);
                                            }
                                            trackedSeasons.put("genres", genres);
                                            trackedTV.put(show.getId(), trackedSeasons);

                                            if (!doc.exists()) {
                                                ref.set(trackedTV);
                                            } else {
                                                if (doc.contains(show.getId())) { // Show exists
                                                    HashMap currentStoredSeasons = ((HashMap)doc.get(show.getId()));
                                                    for (Map.Entry<String, Object> entry : trackedSeasons.entrySet()) {
                                                        String seasonString;
                                                        if (entry.getKey().contains("Season")) {
                                                            seasonString = entry.getKey();
                                                            if ((((HashMap)doc.get(show.getId())).containsKey(seasonString))) { // Season exists
                                                                HashMap currentStoredSeason = ((HashMap)((HashMap)doc.get(show.getId())).get(seasonString));
                                                                HashMap<String, Object> allEpisodes = (HashMap<String, Object>)entry.getValue();
                                                                for (Map.Entry<String, Object> e : allEpisodes.entrySet()) {
                                                                    if (!currentStoredSeason.containsKey(e.getKey())) {
                                                                        currentStoredSeason.put(e.getKey(), e.getValue());
                                                                    }
                                                                }
                                                                if (!currentStoredSeason.isEmpty())
                                                                    currentStoredSeasons.put(seasonString, currentStoredSeason);
                                                            } else { // Season doesn't exist
                                                                HashMap<String, Object> allEpisodes = (HashMap<String, Object>)entry.getValue();
                                                                currentStoredSeasons.put(seasonString, allEpisodes);
                                                            }
                                                        }
                                                    }
                                                    ref.update(show.getId(), currentStoredSeasons);
                                                } else {
                                                    ref.update(show.getId(), trackedTV.get(show.getId()));
                                                }
                                            }
                                            mActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(mContext.getApplicationContext(), "TV Show Tracked!", Snackbar.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    });
                                    flagCheck.start();
                                }
                            });
                        }
                    }
                });
            }
        });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
            }
        });

        return dialog;
    }

    private static boolean checkForNotCompletes(Map<FullTvShowDetails.season, Boolean> map) {
        for (Map.Entry<FullTvShowDetails.season, Boolean> e : map.entrySet()) {
            if (!e.getValue())
                return true;
        }
        return false;
    }

    private static synchronized Map.Entry<String, Object> trackSeasonForShowTrack(String seriesId, final FullTvShowDetails.season s, final FullTvShowDetails show, final ArrayList<FullTvShowDetails.Genre> genreList) {
        final HashMap<String, Object> tempMap = new HashMap<>();
        SyncHttpClient client = new SyncHttpClient();
        String url = "https://api.themoviedb.org/3/" + "tv/"+ seriesId + "/season/" + s.season_number + "?api_key=" + TmdbClient.key;
        client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                final FullTvSeasonDetails season = new Gson().fromJson(response.toString(), FullTvSeasonDetails.class);
                if (season == null) {
                    flags.put(s, true);
                    return;
                }

                final Map<String, Object> trackedEpisode = new HashMap<>();

                ArrayList<FullTvSeasonDetails.Episode> episodes = season.getEpisodes();

                Date date = new Date();

                Globals.trackedTV tv = null;
                if (Globals.trackedTVContains(show.getId())) {
                    for (Globals.trackedTV t : Globals.getTrackedTvShows()) {
                        if (t.id.equals(show.getId())) {
                            tv = t;
                        }
                    }
                } else {
                    tv = new Globals.trackedTV();
                }
                tv.date = date;
                tv.id = show.getId();
                tv.name = show.getName();
                tv.poster_path = show.getPoster_path();
                SparseArray<String> trackedGenres = new SparseArray<>();
                for (FullTvShowDetails.Genre g : genreList) {
                    trackedGenres.put(g.id, g.name);
                }
                tv.genres = trackedGenres;

                for (final FullTvSeasonDetails.Episode e : episodes) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        Date episodeDate = sdf.parse(e.getAir_date());
                        if (episodeDate.after(date)) {
                            flags.put(s, true);
                            break;
                        }
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }

                    final Map<String, Object> trackData = new HashMap<>();
                    trackData.put("date", date);
                    trackData.put("episodeName", e.getName());
                    trackData.put("episodeNum", e.getEpisode_number());

                    trackData.put("id", e.getId());
                    trackData.put("seriesName", show.getName());

                    String episodeString = "Episode " + e.getEpisode_number();
                    trackedEpisode.put(episodeString, trackData);

                    Globals.trackedTV.Episode ep = new Globals.trackedTV.Episode();
                    ep.date = date;
                    ep.episodeName = e.getName();
                    ep.id = e.getId();
                    ep.seriesName = show.getName();
                    ep.episodeNum = e.getEpisode_number();
                    tv.addEpisode(ep);
                }

                String seasonString = "Season " + s.season_number;
                tempMap.put(seasonString, trackedEpisode);

                flags.put(s, true);
            }
        });
        return new AbstractMap.SimpleEntry<>("Season " + s.season_number, tempMap.get("Season " + s.season_number));
    }

    public static AlertDialog untrackMovie(final Context mContext, final Activity mActivity, final String username, final String id, final MaterialProgressBar progressBar) {
        final AlertDialog dialog = SimpleDialog.create(DialogOption.YesCancel, mContext, "Untrack Movie", "Are you sure you want to untrack this movie?");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (progressBar != null)
                    progressBar.setVisibility(View.VISIBLE);
                DocumentReference ref = FirebaseFirestore.getInstance().collection("TrackedMovies").document(username);
                ref.update(id, FieldValue.delete());

                Globals.removeFromTrackedMovies(id);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SyncHttpClient client = new SyncHttpClient();
                        String url = "https://api.themoviedb.org/3/" + "movie/" + id + "?api_key=" + TmdbClient.key;
                        client.get(url, null, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                FullMovieDetails movieDetails = new Gson().fromJson(response.toString(), FullMovieDetails.class);
                                if (movieDetails == null)
                                    return;
                                final ArrayList<FullMovieDetails.Genre> genreList = movieDetails.getGenres();

                                final DocumentReference userRef = FirebaseFirestore.getInstance().collection("UserDetails").document(username);
                                userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot doc = task.getResult();
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("timeWatched", Integer.valueOf(doc.get("timeWatched").toString()) - runtime);
                                            Map<String, Long> userGenres = (Map<String, Long>)doc.get("genresWatched");
                                            for (FullMovieDetails.Genre g : genreList) {
                                                userGenres.put(g.name, userGenres.get(g.name) - 1);
                                            }
                                            userData.put("genresWatched", userGenres);
                                            userRef.update(userData);

                                            mActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (progressBar != null)
                                                        progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(mContext.getApplicationContext(), "Movie Untracked!", Snackbar.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    }
                }).start();
            }
        });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
            }
        });

        return dialog;
    }

    public static void untrackTV() {

    }
}