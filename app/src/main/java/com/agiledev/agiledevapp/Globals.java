package com.agiledev.agiledevapp;

import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class Globals
{

    //Lists
    private static SparseArray<String> genreTags = new SparseArray<>();
    private static List<trackedMovie> trackedMovies = new ArrayList<>();
    private static List<trackedTV> trackedTV = new ArrayList<>();
    private static List<trendingMovie> trendingMovies = new ArrayList<>();
    private static List<trendingTvShow> trendingTvShows = new ArrayList<>();
//    private static Map<String, Object> trackedMovies = new HashMap<>();

    //Genres getter and setter
    public static SparseArray<String> getGenreTags() {
        return genreTags;
    }
    public static synchronized void setGenreTags(SparseArray<String> genres) {
        genreTags = genres;
    }


    //Tracked movies configurators
    public static List<trackedMovie> getTrackedMovies() {
        return trackedMovies;
    }

    public static List<trackedTV> getTrackedTvShows() {
        return trackedTV;
    }


    //----------- Setters ------------
    public static synchronized void setTrackedMovies(List<trackedMovie> trackedMovies) {
        Globals.trackedMovies = trackedMovies;
    }
    public static synchronized void setTrackedTvShows(List<trackedTV> trackedTvShows) {
        Globals.trackedTV = trackedTvShows;
    }


    //----------- Adding ------------
    public static synchronized void addToTrackedMovies(trackedMovie movie) {
        Globals.trackedMovies.add(movie);
    }
    public static synchronized void addToTrackedTvShows(trackedTV TV) {
        Globals.trackedTV.add(TV);
    }


    //----------- Removing ------------
    public static synchronized void removeFromTrackedMovies(String id) {
        for (int i = 0; i < Globals.trackedMovies.size(); i++) {
            if (Globals.trackedMovies.get(i).id.equals(id)) {
                Globals.trackedMovies.remove(i);
                break;
            }
        }
    }
    public static synchronized void removeFromTrackedTvShows(String id) {
        for (int i = 0; i < Globals.trackedTV.size(); i++) {
            if (Globals.trackedTV.get(i).id.equals(id)) {
                Globals.trackedTV.remove(i);
                break;
            }
        }
    }


    //----------- Contains ------------
    public static boolean trackedMoviesContains(String id) {
        for (trackedMovie m : Globals.trackedMovies) {
            if(m.id.equals(id))
                return true;
        }
        return false;
    }
    public static boolean trackedTVContains(String id) {
        for (trackedTV m : Globals.trackedTV) {
            if(m.id.equals(id))
                return true;
        }
        return false;
    }


    //----------- Sorting ------------
    public static synchronized void sortTrackedMovies() {
        if (Globals.trackedMovies.size() > 0) {
            Collections.sort(Globals.trackedMovies, new Comparator<trackedMovie>() {
                @Override
                public int compare(Globals.trackedMovie o1, Globals.trackedMovie o2) {
                    return o2.date.compareTo(o1.date);
                }
            });
        }
    }
    public static synchronized void sortTrackedTvShows() {
        if (Globals.trackedTV.size() > 0) {
            Collections.sort(Globals.trackedTV, new Comparator<trackedTV>() {
                @Override
                public int compare(Globals.trackedTV o1, Globals.trackedTV o2) {
                    return o2.date.compareTo(o1.date);
                }
            });
        }
    }


    //-------Tracked Movies--------
    public static class trackedMovie implements Comparable<trackedMovie> {
        String id, name;
        Date date;
        String poster_path;
        SparseArray<String> genres = new SparseArray<>();

        @Override
        public int compareTo(@NonNull trackedMovie o) {
            return o.date.compareTo(date);
        }
    }

    //-------Tracked TV Shows--------
    public static class trackedTV implements Comparable<trackedTV>
    {
        String id, title;
        Date date;
        String poster_path;
        SparseArray<String> genres = new SparseArray<>();

        @Override
        public int compareTo(@NonNull trackedTV o) {
            return o.date.compareTo(date);
        }
    }


    //-------Trending Movie--------
    public static class trendingMovie {
        String id;
        String poster_path;
        Float vote_average;
    }

    public static List<trendingMovie> getTrendingMovies()
    {
            return trendingMovies;
    }

    public static void setTrendingMovies(List<trendingMovie> trendingMovies)
    {
            Globals.trendingMovies = trendingMovies;
    }

    public static void addToTrendingMovies(trendingMovie movie)
    {
            Globals.trendingMovies.add(movie);
    }


    //-------Trending TvShow--------
    public static class trendingTvShow
    {
        String id;
        String poster_path;
        Float vote_average;
    }

    public static List<trendingTvShow> getTrendingTvShows()
    {
        return trendingTvShows;
    }

    public static void setTrendingTvShows(List<trendingTvShow> trendingTvShows)
    {
        Globals.trendingTvShows = trendingTvShows;
    }

    public static void addToTrendingTvShows(trendingTvShow tvshow)
    {
        Globals.trendingTvShows.add(tvshow);
    }


}
