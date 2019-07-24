package com.example.luowenliang.idouban.moviedetail.service;

import com.example.luowenliang.idouban.moviedetail.entity.CommentsItem;

import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TotalCommentsService {
    @GET("{id}/comments")
    rx.Observable<CommentsItem> getTotalComments(@Path("id")String id);
}