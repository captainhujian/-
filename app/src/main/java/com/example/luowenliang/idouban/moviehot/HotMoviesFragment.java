package com.example.luowenliang.idouban.moviehot;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.luowenliang.idouban.R;
import com.example.luowenliang.idouban.moviedetail.MovieDetailActivity;
import com.example.luowenliang.idouban.moviedetail.utils.SetMovieDetailData;
import com.example.luowenliang.idouban.moviehot.adapter.ComingSoonRecyclerViewAdapter;
import com.example.luowenliang.idouban.moviehot.adapter.HotMovieRecyclerViewAdapter;
import com.example.luowenliang.idouban.moviehot.adapter.PublicPraiseRecyclerViewAdapter;
import com.example.luowenliang.idouban.moviehot.entity.HotMovieInfo;
import com.example.luowenliang.idouban.moviehot.entity.HotMovieItem;
import com.example.luowenliang.idouban.moviehot.service.ComingSoonService;
import com.example.luowenliang.idouban.moviehot.service.HotMovieService;
import com.example.luowenliang.idouban.moviehot.service.PublicPraiseService;
import com.example.luowenliang.idouban.moviehot.service.SearchService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.example.luowenliang.idouban.moviedetail.MovieDetailActivity.PICTURE_PLACE_HOLDER;

public class HotMoviesFragment extends Fragment {
    private static final String TAG = "热门";
    private EditText search;
    private TextView hotMovieTitle,comingSoonTitle,publicPraiseTitle;
    private String searchText;
    private SearchInfo searchInfo;
    private HotMovieInfo hotMovieInfo;
    private List<HotMovieInfo>hotMovieInfos=new ArrayList<>();
    private List<HotMovieInfo>comingSoonMovieInfos=new ArrayList<>();
    private List<HotMovieInfo>publicPraiseInfos=new ArrayList<>();
    private RecyclerView hotMovieRecyclerView;
    private HotMovieRecyclerViewAdapter hotMovieRecyclerViewAdapter;
    private RecyclerView comingSoonRecyclerView;
    private ComingSoonRecyclerViewAdapter comingSoonRecyclerViewAdapter;
    private RecyclerView publicPraiseRecyclerView;
    private PublicPraiseRecyclerViewAdapter publicPraiseRecyclerViewAdapter;
    private ProgressBar hotMovieProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_movies_hot,null);
        Log.d("进度条", "热门onCreateView: ");
        search=view.findViewById(R.id.search_text);
        hotMovieTitle=view.findViewById(R.id.hot_movie_title);
        comingSoonTitle=view.findViewById(R.id.coming_soon_title);
        publicPraiseTitle=view.findViewById(R.id.public_praise_title);
        hotMovieRecyclerView=view.findViewById(R.id.hot_movie_recycler_view);
        comingSoonRecyclerView=view.findViewById(R.id.coming_soon_recycler_view);
        publicPraiseRecyclerView=view.findViewById(R.id.public_praise_recycler_view);
        hotMovieProgressBar=view.findViewById(R.id.hot_movie_progress_bar);
        //网格recyclerView的样式初始化

        //评论不可滑动recyclerview
        GridLayoutManager gridLayoutManager = new GridLayoutManager (getActivity (),3,GridLayoutManager.VERTICAL,false){
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        GridLayoutManager gridLayoutManager2 = new GridLayoutManager (getActivity (),3,GridLayoutManager.VERTICAL,false){
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        //评论不可滑动recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false){
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        hotMovieRecyclerView.setItemAnimator(new DefaultItemAnimator());
        hotMovieRecyclerView.setLayoutManager(gridLayoutManager);
        comingSoonRecyclerView.setItemAnimator(new DefaultItemAnimator());
        comingSoonRecyclerView.setLayoutManager(gridLayoutManager2);
        publicPraiseRecyclerView.setItemAnimator(new DefaultItemAnimator());
        publicPraiseRecyclerView.setLayoutManager(layoutManager);
        searchMovie();
        initHotMovieData();
        initComingSoonMovieData();
        initPublicPraiseMovieData();

        return view;
    }

    /**
     * 搜索功能
     */
    private void searchMovie() {
        Log.d(TAG, "searchText:"+searchText);
        //回车监听必须加上singleline=true，不然再次搜索会变回换行,发现监听执行了两次，
        // 因为onkey事件包含了down和up事件，所以只需要加入其中一个即可。（选择down按下）
         search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
             @Override
             public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                 if ((actionId==EditorInfo.IME_ACTION_SEARCH ||(keyEvent!=null&&keyEvent.getKeyCode()== KeyEvent.KEYCODE_ENTER
                         &&keyEvent.getAction() == KeyEvent.ACTION_DOWN))) {
                     if(search.getText()!=null){
                             searchText = search.getText().toString();
                             Log.d(TAG, "点击搜索");
                             //do something
                             initSearchData(searchText);
                         //打开、关闭软键盘
                         InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                         imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                        // hideInput();
                     }
                     return true;
                 }
                 return false;
             }
         });
    }

    /**
     * 搜索的网络请求
     * @param searchText
     * @return
     */
    private rx.Observable<SearchItem> requsetSearchMovieData(String searchText) {
        Log.d(TAG, "搜索条件："+searchText);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://douban.uieee.com/v2/book/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        SearchService searchService = retrofit.create(SearchService.class);
        return searchService.getSearchResult(searchText);//

    }
    private void initSearchData(String searchText) {
        requsetSearchMovieData(searchText)
                .subscribeOn(Schedulers.io())//io加载数据
                .observeOn(AndroidSchedulers.mainThread())//主线程显示数据
                .subscribe(new Subscriber<SearchItem>() {
                    @Override
                    public void onCompleted() {
                        //hideInput();
                        Intent intent =new Intent(getActivity(),MovieDetailActivity.class);
                        intent.putExtra("id",searchInfo.getSearchId());
                        startActivity(intent);
                        Log.d(TAG, "id:"+searchInfo.getSearchId());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: "+e.toString());
                    }

                    @Override
                    public void onNext(SearchItem searchItem) {
                        if(searchItem.getBooks()!=null){
                                String id =searchItem.getBooks().get(0).getId();
                                searchInfo=new SearchInfo(id);
                        }

                    }
                });
    }



    /**
     * 影院热门的网络请求
     * @return
     */
    private rx.Observable<HotMovieItem> requestHotMovieData() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://douban.uieee.com/v2/movie/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        HotMovieService hotMovieService = retrofit.create(HotMovieService.class);
        return hotMovieService.getHotMovieResult();
    }

    /**
     * 影院热门页面电影数据传递到主线程
     */
    private void initHotMovieData() {
        requestHotMovieData()
                .subscribeOn(Schedulers.io())//io加载数据
                .observeOn(AndroidSchedulers.mainThread())//主线程显示数据
                .subscribe(new Subscriber<HotMovieItem>() {
                    @Override
                    public void onCompleted() {
                        hotMovieProgressBar.setVisibility(View.GONE);
                        showTitle();
                        hotMovieRecyclerViewAdapter=new HotMovieRecyclerViewAdapter(hotMovieInfos);
                        hotMovieRecyclerView.setAdapter(hotMovieRecyclerViewAdapter);
                        HotMovieRecyclerViewOnClickItem();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: "+e.toString());

                    }

                    @Override
                    public void onNext(HotMovieItem hotMovieItem) {

                        hotMovieProgressBar.setVisibility(View.VISIBLE);
                        setHotMovieData(hotMovieItem,hotMovieInfos);
                    }
                });
    }

    /**
     * 即将上映的网络请求
     * @return
     */
    private rx.Observable<HotMovieItem> requestComingSoonData() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://douban.uieee.com/v2/movie/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        ComingSoonService comingSoonService = retrofit.create(ComingSoonService.class);
        return comingSoonService.getComingSoonResult();
    }
    /**
     * 即将上映电影数据传递到主线程
     */
    private void initComingSoonMovieData() {
        requestComingSoonData()
                .subscribeOn(Schedulers.io())//io加载数据
                .observeOn(AndroidSchedulers.mainThread())//主线程显示数据
                .subscribe(new Subscriber<HotMovieItem>() {
                    @Override
                    public void onCompleted() {
                        comingSoonRecyclerViewAdapter=new ComingSoonRecyclerViewAdapter(comingSoonMovieInfos);
                        comingSoonRecyclerView.setAdapter(comingSoonRecyclerViewAdapter);
                        ComingSoonRecyclerViewOnClickItem();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onErrorComingSoon: "+e.toString());
                    }

                    @Override
                    public void onNext(HotMovieItem hotMovieItem) {
                        setHotMovieData(hotMovieItem,comingSoonMovieInfos);
                    }
                });
    }
    /**
     * 口碑榜的网络请求
     * @return
     */
    private rx.Observable<HotMovieItem> requestPublicPraiseData() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://douban.uieee.com/v2/movie/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        PublicPraiseService publicPraiseService = retrofit.create(PublicPraiseService.class);
        return publicPraiseService.getPublicPraiseResult();
    }
    /**
     * 口碑榜电影数据传递到主线程
     */
    private void initPublicPraiseMovieData() {
        requestPublicPraiseData()
                .subscribeOn(Schedulers.io())//io加载数据
                .observeOn(AndroidSchedulers.mainThread())//主线程显示数据
                .subscribe(new Subscriber<HotMovieItem>() {
                    @Override
                    public void onCompleted() {
                        publicPraiseRecyclerViewAdapter=new PublicPraiseRecyclerViewAdapter(publicPraiseInfos);
                        publicPraiseRecyclerView.setAdapter(publicPraiseRecyclerViewAdapter);
                        PublicPraiseRecyclerViewOnClickItem();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onErrorPraise: "+e.toString());
                    }

                    @Override
                    public void onNext(HotMovieItem hotMovieItem) {
                        setHotMovieData(hotMovieItem,publicPraiseInfos);
                        Log.d(TAG, "口碑标题: "+ hotMovieItem.getSubjects().get(0).getTitle());

                    }
                });
    }


    /**
     * 影院热门、即将上映、口碑榜电影数据加载到tempList中传递给adapter
     */
    private void setHotMovieData(HotMovieItem hotMovieItem,List<HotMovieInfo>movieInfoList){
        List<HotMovieInfo>tempList=new ArrayList<>();
        //防止有的图片为空导致recyclerView不显示，这里设置占位图
        String hotMoviePicture;
        double hotMovieRating;
        for(int i=0;i<hotMovieItem.getSubjects().size();i++){
            Log.d(TAG, "item: "+hotMovieItem);
            if(hotMovieItem.getSubjects().get(i).getImages()==null){
                hotMoviePicture=PICTURE_PLACE_HOLDER;
            }
            else{
                hotMoviePicture=hotMovieItem.getSubjects().get(i).getImages().getLarge();
                Log.d(TAG, "directorpicture: "+hotMoviePicture);
            }
            String hotMovieTitle=hotMovieItem.getSubjects().get(i).getTitle();
            if(hotMovieItem.getSubjects().get(i).getRating()!=null){
               hotMovieRating= hotMovieItem.getSubjects().get(i).getRating().getAverage();
            }else {
                hotMovieRating=0f;
            }
            String hotMovieId=hotMovieItem.getSubjects().get(i).getId();
            //星级评分
            double fitFilmRate = fitRating(hotMovieRating);
            Log.d("分数", "合理的分数: " + fitFilmRate);
            String hotMovieMessage= setPublicPraiseMesssage(hotMovieItem,i);
            hotMovieInfo=new HotMovieInfo(hotMoviePicture,hotMovieTitle,hotMovieRating,hotMovieId,fitFilmRate,hotMovieMessage);
            tempList.add(hotMovieInfo);
        }
        movieInfoList.addAll(tempList);
    }


    /**
     * 排行榜详细信息数据加载(做防空指针操作)
     */
    private String setPublicPraiseMesssage(HotMovieItem hotMovieItem,int i) {
        String genre1,genre2,cast1=null,cast2=null;
      //类型
        if(hotMovieItem.getSubjects().get(i).getGenres()==null){
            genre1="";genre2="";
        }else{
            if (hotMovieItem.getSubjects().get(i).getGenres().size() > 1) {
                genre1 = hotMovieItem.getSubjects().get(i).getGenres().get(0);
                genre2 =" " + hotMovieItem.getSubjects().get(i).getGenres().get(1)+"/";
            } else {
                genre1 = hotMovieItem.getSubjects().get(i).getGenres().get(0);
                genre2 = "/";
            }
        }
        //卡司
        if(hotMovieItem.getSubjects().get(i).getCasts()==null) {
            cast1 = "";cast2 = "";
        }else {
            if (hotMovieItem.getSubjects().get(i).getCasts().size() > 1) {
                cast1 = "";hotMovieItem.getSubjects().get(i).getCasts().get(0).getName();
                cast2 =" " + hotMovieItem.getSubjects().get(i).getCasts().get(1).getName() + "/";
            } else if(hotMovieItem.getSubjects().get(i).getCasts().size()==1){
                cast1 = "";hotMovieItem.getSubjects().get(i).getCasts().get(0).getName();
                cast2 = "/";
            }
        }
        return genre1+genre2+cast1+cast2;
    }


    /**
     * 显示标题
     */
    private void showTitle() {
        hotMovieTitle.setVisibility(View.VISIBLE);
        comingSoonTitle.setVisibility(View.VISIBLE);
        publicPraiseTitle.setVisibility(View.VISIBLE);
    }


    /**
     * 热门电影点击事件
     */
    private void HotMovieRecyclerViewOnClickItem() {
        hotMovieRecyclerViewAdapter.setOnHotMovieClickListener(new HotMovieRecyclerViewAdapter.OnHotMovieClickListener() {
            @Override
            public void onClick(String hotMovieId) {
                if(hotMovieId!=null){
                    Intent intent = new Intent(getActivity(),MovieDetailActivity.class);
                    intent.putExtra("id", hotMovieId);
                    startActivity(intent);
                }
            }
        });
    }
    /**
     * 即将上映的点击事件
     */
    private void ComingSoonRecyclerViewOnClickItem() {
        comingSoonRecyclerViewAdapter.setOnComingSoonClickListener(new ComingSoonRecyclerViewAdapter.OnComingSoonClickListener() {
            @Override
            public void onClick(String hotMovieId) {
                if(hotMovieId!=null){
                    Intent intent = new Intent(getActivity(),MovieDetailActivity.class);
                    intent.putExtra("id", hotMovieId);
                    startActivity(intent);
                }
            }
        });
    }
    /**
     * 口碑榜的点击事件
     */
    private void PublicPraiseRecyclerViewOnClickItem() {
        publicPraiseRecyclerViewAdapter.setOnPublicPraiseClickListener(new PublicPraiseRecyclerViewAdapter.OnpublicPraiseClickListener() {
            @Override
            public void onClick(String hotMovieId) {
                if(hotMovieId!=null){
                    Intent intent = new Intent(getActivity(),MovieDetailActivity.class);
                    intent.putExtra("id", hotMovieId);
                    startActivity(intent);
                }
            }
        });
    }



    /**
     * 转换合理的评分以星级显示
     */
    public double fitRating ( double rating) {
        double ratingIn5 = 0;
        if (rating >= 9.2f) {
            ratingIn5 = 5f;
        } else if (rating >= 2f) {
            BigDecimal tempRating = new BigDecimal(rating).setScale(0, BigDecimal.ROUND_HALF_UP);
            ratingIn5 = tempRating.doubleValue() / 2f;
        } else {
            if (rating == 0f) {
                ratingIn5 = 0f;
            } else {
                ratingIn5 = 1f;
            }

        }
        return ratingIn5;
    }

}
