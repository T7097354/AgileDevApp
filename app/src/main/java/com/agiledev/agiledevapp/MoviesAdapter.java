package com.agiledev.agiledevapp;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MyViewHolder> {

    private Context mContext;
    private List mediaList;
    public FragmentManager manager;
    private String type;

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, genres, release_date;
        ImageView poster;
        String id;
        RelativeLayout layout;

        MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.movieCardTitle);
            genres = view.findViewById(R.id.movieCardGenres);
            release_date = view.findViewById(R.id.movieCardReleaseDate);
            poster = view.findViewById(R.id.movieCardPoster);
            layout = view.findViewById(R.id.movieCard);
        }
    }

    MoviesAdapter(Context mContext, List mediaList, FragmentManager manager, String type) {
        this.mContext = mContext;
        this.mediaList = mediaList;
        this.manager = manager;
        this.type = type;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_movie_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position)  {
        if (type.equals("Movie")) {
            BasicMovieDetails movie = (BasicMovieDetails)mediaList.get(position);

            holder.title.setText(movie.getTitle());
            holder.genres.setText(movie.getGenreNames());

            holder.release_date.setText((movie.getRelease_date().equals("") ? "No Release" : mContext.getString(R.string.movie_card_released, movie.getRelease_date())));
            holder.id = movie.getId();
            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MovieFullScreenDialog dialog = MovieFullScreenDialog.newInstance(holder.id);
                    dialog.show(manager, MovieFullScreenDialog.TAG);
                }
            });

            TmdbClient.loadImage(mContext, movie.getPoster_path(), holder.poster, TmdbClient.imageType.ICON);
        } else if (type.equals("TV")) {
            BasicTvShowDetails tv = (BasicTvShowDetails) mediaList.get(position);

            holder.title.setText(tv.getName());
            holder.genres.setText(tv.getGenreNames());

            holder.release_date.setText((tv.getFirst_air_date().equals("") ? "No Release" : mContext.getString(R.string.movie_card_released, tv.getFirst_air_date())));
            holder.id = tv.getId();
            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TvShowFullScreenDialog dialog = TvShowFullScreenDialog.newInstance(holder.id);
                    dialog.show(manager, TvShowFullScreenDialog.TAG);
                }
            });

            TmdbClient.loadImage(mContext, tv.getPoster_path(), holder.poster, TmdbClient.imageType.ICON);
        }

    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }
}
