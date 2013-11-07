package org.ipccenter.newsagg.impl.vkapi;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.*;

import org.ipccenter.newsagg.News;
import org.ipccenter.newsagg.Puller;
import org.ipccenter.newsagg.gson.FeedItem;
import org.ipccenter.newsagg.gson.Response;


/**
 * Created with IntelliJ IDEA.
 * User: darya
 * Date: 28.10.13
 * Time: 22:30
 * To change this template use File | Settings | File Templates.
 */
public class VKPuller implements Puller {

    private List<News> postsList = new ArrayList<News>();
    private Date lastUpdate;

    public long getLastUpdateTime(){ return lastUpdate.getTime();}

    public void checkFeed() throws IOException {
        VKAuth auth = new VKAuth("215332856", "", "");
        VKMethod getFeed = new VKMethod("newsfeed.get", auth);
        String filters = "post,note";
        String startTime = String.valueOf(getLastUpdateTime());
        String feed = getFeed.addParam("filters", filters).addParam("start_time", startTime).execute();
        Gson gson = new Gson();
        Response feedResponse =  gson.fromJson(feed, Response.class);
        if (feedResponse.getError() != null)
            throw new IllegalArgumentException(feedResponse.getError().getErrorMessage());
        FeedItem[] feedList = feedResponse.getResponse();
        for(FeedItem feedItem : feedList)
            parsePost(feedItem);
    }

    public void findPosts() throws IOException {
        VKAuth auth = new VKAuth("215332856", "", "");
        VKMethod searchFeed = new VKMethod("newsfeed.search", auth);
        List<String> requests = Arrays.asList(KEYWORDS.values().toString());
        String count = "100";
        String startTime = String.valueOf(getLastUpdateTime());
        for (String request: requests){
            String feed = searchFeed.addParam("q", request).addParam("count", count).addParam("start_time", startTime).execute();
            Gson gson = new Gson();
            Response searchFeedResponse = gson.fromJson(feed, Response.class);
            if (searchFeedResponse.getError() != null)
                throw new IllegalArgumentException(searchFeedResponse.getError().getErrorMessage());
            FeedItem[] feedList = searchFeedResponse.getResponse();
            for (FeedItem feedItem : feedList)
                parsePost(feedItem);
        }

    }

    public void parsePost(FeedItem feedItem){
        News post = new News();
        post.source = News.Sources.VKONTAKTE;
        post.content = feedItem.getText();
        post.date = feedItem.getDate();
        if (feedItem.isCopy())
            post.url = "vk.com/" + feedItem.getCopyPostAddress();
        else
            post.url = "vk.com/" + feedItem.getPostAddress();
        post.status = 0;
        postsList.add(post);
    }


}
