package com.example.luowenliang.idouban.moviehot.service;



import com.example.luowenliang.idouban.moviehot.entity.HotMovieItem;

import retrofit2.http.GET;

public interface ComingSoonService {
    @GET("coming_soon?start=0&count=6")
    rx.Observable<HotMovieItem> getComingSoonResult();//
}
