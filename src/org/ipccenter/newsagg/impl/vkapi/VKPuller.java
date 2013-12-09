package org.ipccenter.newsagg.impl.vkapi;

import com.google.gson.Gson;
import org.ipccenter.newsagg.Puller;
import org.ipccenter.newsagg.entity.News;
import org.ipccenter.newsagg.gson.Feed;
import org.ipccenter.newsagg.gson.FeedItem;
import org.ipccenter.newsagg.gson.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.ipccenter.newsagg.gson.SearchFeed;
import org.ipccenter.newsagg.gson.SearchFeedItem;

/**
 * Created with IntelliJ IDEA. User: darya Date: 28.10.13 Time: 22:30 To change
 * this template use File | Settings | File Templates.
 */
public class VKPuller implements Puller {


    private VKAuth auth;

    private static final Logger LOG = LoggerFactory.getLogger(VKPuller.class);

    private List<News> postsList = new ArrayList<News>();
    private int offset = 0;

    public VKPuller(VKAuth auth) {
        this.auth = auth;
    }

    public List<News> getPostsList() {
        return postsList;
    }

    public int getOffset() {
        return offset;
    }

    public void getFriends() throws IOException, NoSuchAlgorithmException {
        VKMethod getFriends = new VKMethod("newsfeed.get", auth);
//        getFriends.addParam("uid", auth.getUserID());
        String friends = getFriends.execute();
        LOG.info("Friends: {}", friends);
    }

    public void checkFeed() throws IOException {
        VKMethod getFeed = new VKMethod("newsfeed.get", auth);
        String filters = "post,note";
        //String startTime = String.valueOf(getLastUpdateTime() - 12 * 60 * 60 * 100);
        getFeed.addParam("filters", filters)
                .addParam("return_banned", "0")
                .addParam("count", "10")
                .addParam("offset", String.valueOf(offset));
        LOG.info("Search method params: {}", getFeed.getParams());
        String rawFeed = getFeed.execute();
        LOG.info("Response in plain text: {}", rawFeed);
        Gson gson = new Gson();
        Response feedResponse = gson.fromJson(rawFeed, Response.class);
        LOG.info("Responce: {}", feedResponse);
        if (feedResponse.getError() != null) {
            LOG.warn("Cannot get response. Error message is: {}", feedResponse.getError().getErrorMessage());
            throw new IllegalArgumentException(feedResponse.getError().getErrorMessage());
        }
        Feed feedList = feedResponse.getResponse();
        offset = feedList.getNewOffset();
        LOG.info("Feedlist: {}", feedList);
        FeedItem[] feedItem = gson.fromJson(gson.toJson(feedList), Feed.class).getItems();
        LOG.info("Feeditems: {}", feedItem);
        for (FeedItem item : feedItem) {
            parsePost(item, null);
        }

    }

    public void findPosts() throws IOException {
        List<String> requests = new ArrayList<String>();
        requests.add("ФРТК");
        requests.add("МФТИ");
        requests.add("Физтех");
        requests.add("РТ");
        String count = "10";
        for (String request : requests) {          
            VKMethod searchFeed = new VKMethod("newsfeed.search", auth);
            String filters = "post,note";
            searchFeed.addParam("filters", filters).addParam("q", request).addParam("count", count).addParam("offset", String.valueOf(offset));
            String feed = searchFeed.execute();
            LOG.info("Response in plain text: {}", feed);
            Gson gson = new Gson();
            Response searchFeedResponse = gson.fromJson(feed, Response.class);
            if (searchFeedResponse.getError() != null) {
                throw new IllegalArgumentException(searchFeedResponse.getError().getErrorMessage());
            }
            SearchFeed feedList = searchFeedResponse.getSearchResponse();
            SearchFeedItem[] feedItem = gson.fromJson(gson.toJson(feedList), SearchFeed.class).getItems();
            for (SearchFeedItem item : feedItem) {
                parsePost(null, item);
            }
        }

    }

    public void parsePost(FeedItem feedItem, SearchFeedItem searchFeedItem) {
        News post = new News();
        post.setSource("vk.com");
        if (searchFeedItem == null){
            post.setContent(feedItem.getText());
            post.setDate(feedItem.getDate());
            if (feedItem.isCopy()) {
                post.setUrl("http://vk.com/" + feedItem.getCopyPostAddress());
            } else {
                post.setUrl("http://vk.com/" + feedItem.getPostAddress());
            }
        }
        else{
            post.setContent(searchFeedItem.getText());
            post.setDate(searchFeedItem.getDate());
        }
        post.setStatus(0);
        //post.setAuthor("");
        postsList.add(post);
    }

}
