/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

package org.mule.twitter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.UnhandledException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.api.context.MuleContextAware;
import org.mule.twitter.UserEvent.EventType;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.internal.http.alternative.HttpClientHiddenConstructionArgument;
import twitter4j.internal.http.alternative.MuleHttpClient;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Twitter is an online social networking service and microblogging service that enables its users to send and read
 * text-based posts of up to 140 characters, known as "tweets".
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "twitter", schemaVersion = "2.4", description = "Twitter Integration", friendlyName = "Twitter")
public class TwitterConnector implements MuleContextAware {

    private static final String STREAM_BASE_URL = "https://stream.twitter.com/1/";
    private static final String SITE_STREAM_BASE_URL = "https://sitestream.twitter.com/2b/";

    protected transient Log logger = LogFactory.getLog(getClass());

    private Twitter twitter;

    private TwitterStream stream;

    /**
     * The consumer key used by this application
     */
    @Configurable
    private String consumerKey;

    /**
     * The consumer key secret by this application
     */
    @Configurable
    private String consumerSecret;

    /**
     * The access key provided by Twitter
     */
    @Optional
    @Configurable
    private String accessKey;

    /**
     * The access secret provided by Twitter
     */
    @Optional
    @Configurable
    private String accessSecret;

    /**
     * Whether to use SSL in API calls to Twitter
     */
    @Optional
    @Configurable
    @Default("true")
    @FriendlyName("Use SSL")
    private boolean useSSL;

    /**
     * Proxy host
     */
    @Configurable
    @Optional
    @Placement(group = "Proxy settings", tab = "Proxy")
    private String proxyHost;

    /**
     * Proxy port
     */
    @Configurable
    @Optional
    @Default("-1")
    @Placement(group = "Proxy settings", tab = "Proxy")
    private int proxyPort;

    /**
     *
     * Proxy username
     */
    @Configurable
    @Optional
    @Placement(group = "Proxy settings", tab = "Proxy")
    private String proxyUsername;

    /**
     * Proxy password
     */
    @Configurable
    @Optional
    @Placement(group = "Proxy settings", tab = "Proxy")
    @Password
    private String proxyPassword;

    @PostConstruct
    public void init() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setUseSSL(useSSL);
        cb.setHttpProxyHost(proxyHost);
        cb.setHttpProxyPort(proxyPort);
        cb.setHttpProxyUser(proxyUsername);
        cb.setHttpProxyPassword(proxyPassword);

        HttpClientHiddenConstructionArgument.setUseMule(true);
        twitter = new TwitterFactory(cb.build()).getInstance();

        twitter.setOAuthConsumer(consumerKey, consumerSecret);
        if (accessKey != null) {
            twitter.setOAuthAccessToken(new AccessToken(accessKey, accessSecret));
        }
    }

    /**
     * Returns tweets that match a specified query.
     * <p/>
     * This method calls http://search.twitter.com/search.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:search}
     *
     * @param query The search query.
     * @param lang Restricts tweets to the given language, given by an <a href="http://en.wikipedia.org/wiki/ISO_639-1">ISO 639-1 code</a>
     * @param locale Specify the language of the query you are sending (only ja is currently effective). This is intended for language-specific clients and the default should work in the majority of cases.
     * @param maxId If specified, returns tweets with status ids less than the given id
     * @param rpp Sets the number of tweets to return per page, up to a max of 100
     * @param page Sets the page number (starting at 1) to return, up to a max of roughly 1500 results
     * @param since If specified, returns tweets since the given date. Date should be formatted as YYYY-MM-DD
     * @param sinceId Returns tweets with status ids greater than the given id.
     * @param geocode A {@link String} containing the latitude and longitude separated by ','. Used to get the tweets by users located within a given radius of the given latitude/longitude, where the user's location is taken from their Twitter profile
     * @param radius The radius to be used in the geocode -ONLY VALID IF A GEOCODE IS GIVEN-
     * @param unit The unit of measurement of the given radius. Can be 'mi' or 'km'. Miles by default.
     * @param until If specified, returns tweets with generated before the given date. Date should be formatted as YYYY-MM-DD
     * @param resultType If specified, returns tweets included popular or real time or both in the responce. Both by default. Can be 'mixed', 'popular' or 'recent'.
     * @return the {@link QueryResult}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public QueryResult search(String query,
                              @Optional String lang,
                              @Optional String locale,
                              @Optional Long maxId,
                              @Optional Integer rpp,
                              @Optional Integer page,
                              @Optional String since,
                              @Optional Long sinceId,
                              @Optional String geocode,
                              @Optional String radius,
                              @Default (value = Query.MILES) @Optional String unit,
                              @Optional String until,
                              @Optional String resultType) throws TwitterException {
        final Query q = new Query(query);
        
        if (lang != null)
        {
            q.setLang(lang);
        }
        if (locale != null)
        {
            q.setLocale(locale);
        }
        if (maxId != null && maxId.longValue() != 0 )
        {
            q.setMaxId(maxId.longValue());
        }
        if (rpp != null && rpp.intValue() != 0 )
        {
            q.setRpp(rpp.intValue());
        }
        if (page != null && page.intValue() != 0)
        {
            q.setPage(page.intValue());
        }
        if (since != null)
        {
            q.setSince(since);
        }
        if (sinceId != null && sinceId.longValue() != 0)
        {
            q.setSinceId(sinceId.longValue());
        }
        if (geocode != null)
        {
            final String[] geocodeSplit = StringUtils.split(geocode, ',');
            final double latitude = Double.parseDouble(StringUtils.replace(geocodeSplit[0], " ", ""));
            final double longitude = Double.parseDouble(StringUtils.replace(geocodeSplit[1], " ", ""));
            q.setGeoCode(new GeoLocation(latitude, longitude), Double.parseDouble(radius), unit);
        }
        if (until != null)
        {
            q.setUntil(until);
        }
        if (resultType != null)
        {
            q.setResultType(resultType);
        }
        return twitter.search(q);
    }

    /**
     * Returns the 20 most recent statuses, including retweets, posted by the
     * authenticating user and that user's friends. This is the equivalent of
     * /timeline/home on the Web.<br>
     * Usage note: This home_timeline call is identical to statuses/friends_timeline,
     * except that home_timeline also contains retweets, while
     * statuses/friends_timeline does not for backwards compatibility reasons. In a
     * future version of the API, statuses/friends_timeline will be deprected and
     * replaced by home_timeline. <br>
     * This method calls http://api.twitter.com/1/statuses/home_timeline
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getHomeTimeline}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of records to retrieve. Must be less than or equal to 200.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID.
     *                There are limits to the number of Tweets which can be accessed through the API. If the
     *                limit of Tweets has occured since the since_id, the since_id will be forced to the
     *                oldest ID available.
     * @return list of {@link Status} of the home Timeline
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/home_timeline">GET
     *      statuses/home_timeline | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getHomeTimeline(@Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getHomeTimeline(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent statuses posted from the authenticating user. It's
     * also possible to request another user's timeline via the id parameter.<br>
     * This is the equivalent of the Web / page for your own user, or the profile
     * page for a third party.<br>
     * For backwards compatibility reasons, retweets are stripped out of the
     * user_timeline when calling in XML or JSON (they appear with 'RT' in RSS and
     * Atom). If you'd like them included, you can merge them in from statuses
     * retweeted_by_me.<br>
     * <br>
     * This method calls http://api.twitter.com/1/statuses/user_timeline.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getUserTimelineByScreenName}
     *
     * @param screenName The screen name of the user for whom to return results for
     * @param page       Specifies the page of results to retrieve.
     * @param count      Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                   best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                   after the count has been applied.
     * @param sinceId    Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                   limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                   the since_id, the since_id will be forced to the oldest ID available.
     * @return list of {@link Status} of the user Timeline
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/user_timeline">GET
     *      statuses/user_timeline | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getUserTimelineByScreenName(String screenName,
                                                            @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                            @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                            @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getUserTimeline(screenName, getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent statuses posted from the authenticating user. It's
     * also possible to request another user's timeline via the id parameter.<br>
     * This is the equivalent of the Web / page for your own user, or the profile
     * page for a third party.<br>
     * For backwards compatibility reasons, retweets are stripped out of the
     * user_timeline when calling in XML or JSON (they appear with 'RT' in RSS and
     * Atom). If you'd like them included, you can merge them in from statuses
     * retweeted_by_me.<br>
     * <br>
     * This method calls http://api.twitter.com/1/statuses/user_timeline.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getUserTimelineByUserId}
     *
     * @param userId  specifies the ID of the user for whom to return the user_timeline
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return list of {@link Status} of the user Timeline
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/user_timeline">GET
     *      statuses/user_timeline | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getUserTimelineByUserId(long userId,
                                                        @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                        @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                        @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getUserTimeline(userId, getPaging(page, count, sinceId));
    }

    protected Paging getPaging(int page, int count, long sinceId) {
        Paging paging = new Paging(page, count);
        if (sinceId > 0) {
            paging.setSinceId(sinceId);
        }
        return paging;
    }

    /**
     * Returns the 20 most recent statuses posted from the authenticating user. It's
     * also possible to request another user's timeline via the id parameter.<br>
     * This is the equivalent of the Web / page for your own user, or the profile
     * page for a third party.<br>
     * For backwards compatibility reasons, retweets are stripped out of the
     * user_timeline when calling in XML or JSON (they appear with 'RT' in RSS and
     * Atom). If you'd like them included, you can merge them in from statuses
     * retweeted_by_me.<br>
     * <br>
     * This method calls http://api.twitter.com/1/statuses/user_timeline.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getUserTimeline}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return list of {@link Status} the user Timeline
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/user_timeline">GET
     *      statuses/user_timeline | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getUserTimeline(@Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getUserTimeline(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent mentions (status containing @username) for the
     * authenticating user. <br>
     * This method calls http://api.twitter.com/1/statuses/mentions
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getMentions}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent mentions ({@link Status} containing @username) for the authenticating user.
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/mentions">GET
     *      statuses/mentions | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getMentions(@Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                            @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                            @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getMentions(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent retweets posted by the authenticating user. <br>
     * This method calls http://api.twitter.com/1/statuses/retweeted_by_me
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedByMe}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent retweets ({@link Status}) posted by the authenticating user
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/retweeted_by_me">GET
     *      statuses/retweeted_by_me | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getRetweetedByMe(@Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                 @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                 @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetedByMe(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent retweets posted by the authenticating user's
     * friends. <br>
     * This method calls http://api.twitter.com/1/statuses/retweeted_to_me
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedToMe}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent retweets ({@link Status}) posted by the authenticating user's
     *         friends.
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/retweeted_to_me">GET
     *      statuses/retweeted_to_me | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getRetweetedToMe(@Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                 @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                 @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetedToMe(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent tweets of the authenticated user that have been
     * retweeted by others. <br>
     * This method calls http://api.twitter.com/1/statuses/retweets_of_me
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetsOfMe}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent tweets ({@link Status})of the authenticated user that have been retweeted by others.
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/retweets_of_me">GET
     *      statuses/retweets_of_me | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getRetweetsOfMe(@Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetsOfMe(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent retweets posted by users the specified user
     * follows. This method is identical to statuses/retweeted_to_me except you can
     * choose the user to view. <br>
     * This method has not been finalized and the interface is subject to change in
     * incompatible ways. <br>
     * This method calls http://api.twitter.com/1/statuses/retweeted_to_user
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedToUserByScreenName}
     *
     * @param screenName the user to view
     * @param page       Specifies the page of results to retrieve.
     * @param count      Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                   best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                   after the count has been applied.
     * @param sinceId    Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                   limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                   the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent retweets ({@link Status}) posted by the authenticating user's friends.
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a
     *      href="http://groups.google.com/group/twitter-api-announce/msg/34909da7c399169e">#newtwitter
     *      and the API - Twitter API Announcements | Google Group</a>
     */
    @Processor
    public ResponseList<Status> getRetweetedToUserByScreenName(String screenName,
                                                               @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                               @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                               @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetedToUser(screenName, getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent retweets posted by users the specified user
     * follows. This method is identical to statuses/retweeted_to_me except you can
     * choose the user to view. <br>
     * This method has not been finalized and the interface is subject to change in
     * incompatible ways. <br>
     * This method calls http://api.twitter.com/1/statuses/retweeted_to_user
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedToUserByUserId}
     *
     * @param userId  the user to view
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent retweets ({@link Status}) posted by the authenticating user's friends.
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a
     *      href="http://groups.google.com/group/twitter-api-announce/msg/34909da7c399169e">#newtwitter
     *      and the API - Twitter API Announcements | Google Group</a>
     */
    @Processor
    public ResponseList<Status> getRetweetedToUserByUserId(long userId,
                                                           @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                           @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                           @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetedToUser(userId, getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent retweets posted by the specified user. This method
     * is identical to statuses/retweeted_by_me except you can choose the user to
     * view. <br>
     * This method has not been finalized and the interface is subject to change in
     * incompatible ways. <br>
     * This method calls http://api.twitter.com/1/statuses/retweeted_by_user
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedByUserByScreenName}
     *
     * @param screenName the user to view
     * @param page       Specifies the page of results to retrieve.
     * @param count      Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                   best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                   after the count has been applied.
     * @param sinceId    Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                   limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                   the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent retweets ({@link Status}) posted by the authenticating user
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a
     *      href="http://groups.google.com/group/twitter-api-announce/msg/34909da7c399169e">#newtwitter
     *      and the API - Twitter API Announcements | Google Group</a>
     */
    @Processor
    public ResponseList<Status> getRetweetedByUserByScreenName(String screenName,
                                                               @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                               @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                               @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetedByUser(screenName, getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent retweets posted by the specified user. This method
     * is identical to statuses/retweeted_by_me except you can choose the user to
     * view. <br>
     * This method has not been finalized and the interface is subject to change in
     * incompatible ways. <br>
     * This method calls http://api.twitter.com/1/statuses/retweeted_by_user
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedByUserByUserId}
     *
     * @param userId  the user to view
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent retweets ({@link Status}) posted by the authenticating user
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a
     *      href="http://groups.google.com/group/twitter-api-announce/msg/34909da7c399169e">#newtwitter
     *      and the API - Twitter API Announcements | Google Group</a>
     */
    @Processor
    public ResponseList<Status> getRetweetedByUserByUserId(long userId,
                                                           @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                                           @Placement(group = "Pagination") @Default(value = "20") @Optional int count,
                                                           @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetedByUser(userId, getPaging(page, count, sinceId));
    }

    /**
     * Returns a single status, specified by the id parameter below. The status's
     * author will be returned inline. <br>
     * This method calls http://api.twitter.com/1/statuses/show
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:showStatus}
     *
     * @param id the numerical ID of the status you're trying to retrieve
     * @return a single {@link Status}
     * @throws twitter4j.TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/show/:id">GET
     *      statuses/show/:id | dev.twitter.com</a>
     */
    @Processor
    public Status showStatus(long id) throws TwitterException {
        return twitter.showStatus(id);
    }

    /**
     * Answers user information for the authenticated user
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:showUser}
     *
     * @return a {@link User} object
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public User showUser() throws TwitterException {
        return twitter.showUser(twitter.getId());
    }

    /**
     * Updates the authenticating user's status. A status update with text identical
     * to the authenticating user's text identical to the authenticating user's
     * current status will be ignored to prevent duplicates. <br>
     * This method calls http://api.twitter.com/1/statuses/update
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:updateStatus}
     *
     * @param status    the text of your status update
     * @param inReplyTo The ID of an existing status that the update is in reply to.
     * @param latitude  The latitude of the location this tweet refers to. This parameter will be ignored unless it is
     *                  inside the range -90.0 to +90.0 (North is positive) inclusive.
     * @param longitude he longitude of the location this tweet refers to. The valid ranges for longitude is -180.0 to
     *                  +180.0 (East is positive) inclusive. This parameter will be ignored if outside that range or if there not a
     *                  corresponding lat parameter.
     * @return the latest {@link Status}
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/statuses/update">POST
     *      statuses/update | dev.twitter.com</a>
     */
    @Processor
    public Status updateStatus(String status,
                               @Default(value = "-1") @Optional long inReplyTo,
                               @Placement(group = "Coordinates") @Optional Double latitude,
                               @Placement(group = "Coordinates") @Optional Double longitude) throws TwitterException {
        StatusUpdate update = new StatusUpdate(status);
        if (inReplyTo > 0) {
            update.setInReplyToStatusId(inReplyTo);
        }
        if (latitude != null && longitude != null) {
            update.setLocation(new GeoLocation(latitude, longitude));
        }
        Status response = twitter.updateStatus(update);
        
        //Twitter4j doesn't throw exception when json reponse has 'error: Could not authenticate with OAuth'
        if (response.getId() == -1)
        {
            throw new TwitterException("Could not authenticate with OAuth\n");
        }

        return response;
    }

    /**
     * Destroys the status specified by the required ID parameter.<br>
     * Usage note: The authenticating user must be the author of the specified
     * status. <br>
     * This method calls http://api.twitter.com/1/statuses/destroy
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:destroyStatus}
     *
     * @param statusId The ID of the status to destroy.
     * @return the deleted {@link Status}
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/statuses/destroy/:id">POST
     *      statuses/destroy/:id | dev.twitter.com</a>
     */
    @Processor
    public Status destroyStatus(long statusId) throws TwitterException {
        return twitter.destroyStatus(statusId);
    }

    /**
     * Retweets a tweet. Returns the original tweet with retweet details embedded. <br>
     * This method calls http://api.twitter.com/1/statuses/retweet
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:retweetStatus}
     *
     * @param statusId The ID of the status to retweet.
     * @return the retweeted {@link Status}
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/statuses/retweet/:id">POST
     *      statuses/retweet/:id | dev.twitter.com</a>
     */
    @Processor
    public Status retweetStatus(long statusId) throws TwitterException {
        return twitter.retweetStatus(statusId);
    }

    /**
     * Returns up to 100 of the first retweets of a given tweet. <br>
     * This method calls http://api.twitter.com/1/statuses/retweets
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweets}
     *
     * @param statusId The numerical ID of the tweet you want the retweets of.
     * @return the retweets ({@link Status}) of a given tweet
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/retweets/:id">Tweets
     *      Resources > statuses/retweets/:id</a>
     * @since Twitter4J 2.0.10
     */
    @Processor
    public ResponseList<Status> getRetweets(long statusId) throws TwitterException {
        return twitter.getRetweets(statusId);
    }

    /**
     * Show user objects of up to 100 members who retweeted the status. <br>
     * This method calls http://api.twitter.com/1/statuses/:id/retweeted_by
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedBy}
     *
     * @param statusId The ID of the status you want to get retweeters of
     * @param page     Specifies the page of results to retrieve.
     * @param count    Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                 best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                 after the count has been applied.
     * @param sinceId  Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                 limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                 the since_id, the since_id will be forced to the oldest ID available.
     * @return the list of {@link User} who retweeted your status
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/:id/retweeted_by">GET
     *      statuses/:id/retweeted_by | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<User> getRetweetedBy(long statusId,
                                             @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                             @Placement(group = "Pagination") @Default(value = "100") @Optional int count,
                                             @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId)
            throws TwitterException {
        return twitter.getRetweetedBy(statusId, getPaging(page, count, sinceId));
    }

    /**
     * Show user ids of up to 100 users who retweeted the status represented by id <br />
     * This method calls
     * http://api.twitter.com/1/statuses/:id/retweeted_by/ids.format
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetedByIds}
     *
     * @param statusId The ID of the status you want to get retweeters of
     * @param page     Specifies the page of results to retrieve.
     * @param count    Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                 best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                 after the count has been applied.
     * @param sinceId  Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                 limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                 the since_id, the since_id will be forced to the oldest ID available.
     * @return {@link IDs} of users who retweeted the stats
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a
     *      href="http://dev.twitter.com/doc/get/statuses/:id/retweeted_by/ids">GET
     *      statuses/:id/retweeted_by/ids | dev.twitter.com</a>
     */
    @Processor(friendlyName = "Get retweeted by IDs")
    public IDs getRetweetedByIds(long statusId,
                                 @Placement(group = "Pagination") @Default(value = "1") @Optional int page,
                                 @Placement(group = "Pagination") @Default(value = "100") @Optional int count,
                                 @Placement(group = "Pagination") @Default(value = "-1") @Optional long sinceId) throws TwitterException {
        return twitter.getRetweetedByIDs(statusId, getPaging(page, count, sinceId));
    }

    /**
     * Set the OAuth verifier after it has been retrieved via requestAuthorization.
     * The resulting access tokens will be logged to the INFO level so the user can
     * reuse them as part of the configuration in the future if desired.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:setOauthVerifier}
     *
     * @param oauthVerifier The OAuth verifier code from Twitter.
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public void setOauthVerifier(String oauthVerifier) throws TwitterException {
        AccessToken accessToken = twitter.getOAuthAccessToken(oauthVerifier);
        logger.info("Got OAuth access tokens. Access token:" + accessToken.getToken()
                + " Access token secret:" + accessToken.getTokenSecret());
    }

    /**
     * Start the OAuth request authorization process.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:requestAuthorization}
     *
     * @param callbackUrl the url to be requested when the user authorizes this app
     * @return The user authorization URL.
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public String requestAuthorization(@Optional String callbackUrl) throws TwitterException {
        RequestToken token = twitter.getOAuthRequestToken(callbackUrl);
        return token.getAuthorizationURL();
    }

    /**
     * Search for places (cities and neighborhoods) that can be attached to a
     * statuses/update. Given a latitude and a longitude, return a list of all the
     * valid places that can be used as a place_id when updating a status.
     * Conceptually, a query can be made from the user's location, retrieve a list of
     * places, have the user validate the location he or she is at, and then send the
     * ID of this location up with a call to statuses/update.<br>
     * There are multiple granularities of places that can be returned --
     * "neighborhoods", "cities", etc. At this time, only United States data is
     * available through this method.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:reverseGeoCode}
     *
     * @param latitude  latitude coordinate. Mandatory if ip is not specified
     * @param longitude longitude coordinate.
     * @return a {@link ResponseList} of {@link Place}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public ResponseList<Place> reverseGeoCode(@Placement(group = "Coordinates") @Optional Double latitude,
                                              @Placement(group = "Coordinates") @Optional Double longitude)
            throws TwitterException {
        return twitter.reverseGeoCode(createQuery(latitude, longitude, null));
    }

    /**
     * Search for places that can be attached to a statuses/update. Given a latitude
     * and a longitude pair, or and IP address, this request will return a list of
     * all the valid places that can be used as the place_id when updating a status.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:searchPlaces}
     *
     * @param latitude  latitude coordinate. Mandatory if ip is not specified
     * @param longitude longitude coordinate.
     * @param ip        the ip. Mandatory if coordinates are not specified
     * @return a {@link ResponseList} of {@link Place}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public ResponseList<Place> searchPlaces(@Placement(group = "Coordinates") @Optional Double latitude,
                                            @Placement(group = "Coordinates") @Optional Double longitude,
                                            @Optional String ip) throws TwitterException {
        return twitter.searchPlaces(createQuery(latitude, longitude, ip));
    }

    private GeoQuery createQuery(Double latitude, Double longitude, String ip) {
        if (ip == null) {
            return new GeoQuery(new GeoLocation(latitude, longitude));
        }
        return new GeoQuery(ip);
    }

    /**
     * Find out more details of a place that was returned from the reverseGeoCode
     * operation.
     * <p/>
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getGeoDetails}
     *
     * @param id The ID of the location to query about.
     * @return a {@link Place}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public Place getGeoDetails(String id) throws TwitterException {
        return twitter.getGeoDetails(id);
    }

    /**
     * Creates a new place at the given latitude and longitude.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:createPlace}
     *
     * @param placeName            The placeName a place is known as.
     * @param containedWithin The place_id within which the new place can be found.
     *                        Try and be as close as possible with the containing place. For
     *                        example, for a room in a building, set the contained_within as the
     *                        building place_id.
     * @param token           The token found in the response from geo/similar_places.
     * @param latitude        The latitude the place is located at.
     * @param longitude       The longitude the place is located at.
     * @param streetAddress   optional: This parameter searches for places which have
     *                        this given street address. There are other well-known, and
     *                        application specific attributes available. Custom attributes are
     *                        also permitted. Learn more about Place Attributes.
     * @return a new {@link Place}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public Place createPlace(String placeName,
                             String containedWithin,
                             String token,
                             @Placement(group = "Coordinates") Double latitude,
                             @Placement(group = "Coordinates") Double longitude,
                             @Optional String streetAddress) throws TwitterException {
        return twitter.createPlace(placeName, containedWithin, token, new GeoLocation(latitude, longitude),
                streetAddress);
    }

    /**
     * Locates places near the given coordinates which are similar in name.
     * Conceptually you would use this method to get a list of known places to choose from first.
     * Then, if the desired place doesn't exist, make a request to POST geo/place to create a new one.
     * The token contained in the response is the token needed to be able to create a new place.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getSimilarPlaces}
     *
     *
     * @param latitude The latitude to search around. This parameter will be ignored unless it is inside the range
     *                 -90.0 to +90.0 (North is positive) inclusive. It will also be ignored if there
     *                 isn't a corresponding long parameter.
     * @param longitude The longitude to search around. The valid ranges for longitude is -180.0 to +180.0
     *                  (East is positive) inclusive. This parameter will be ignored if outside that range,
     *                  if it is not a number, if geo_enabled is disabled, or if there not
     *                  a corresponding lat parameter.
     * @param placeName      The name a place is known as.
     * @param containedWithin This is the place_id which you would like to restrict the search results to.
     *                        Setting this value means only places within the given place_id will be found.
     * @param streetAddress This parameter searches for places which have this given street address.
     *                      There are other well-known, and application specific attributes available.
     *                      Custom attributes are also permitted.
     * @return places
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public SimilarPlaces getSimilarPlaces(@Placement(group = "Coordinates") Double latitude,
                                          @Placement(group = "Coordinates") Double longitude,
                                          String placeName, @Optional String containedWithin,
                                          @Optional String streetAddress) throws TwitterException {
        return twitter.getSimilarPlaces(new GeoLocation(latitude, longitude),
                placeName, containedWithin, streetAddress);
    }

    /**
     * Returns the sorted locations that Twitter has trending topic information for. 
     * The response is an array of &quot;locations&quot; that encode the location's WOEID 
     * (a <a href="http://developer.yahoo.com/geo/geoplanet/">Yahoo! Where On Earth ID</a>) 
     * and some other human-readable information such as a canonical name and country the 
     * location belongs in.
     * <br>The available trend locations will be sorted by distance to the lat 
     * and long passed in. The sort is nearest to furthest.
     *
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getAvailableTrends}
     * 
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the {@link Location}s
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public ResponseList<Location> getAvailableTrends(@Optional Double latitude, 
                                                     @Optional Double longitude) 
            throws TwitterException {
        
        if(latitude != null && longitude != null) {
            return twitter.getAvailableTrends(new GeoLocation(latitude, longitude));
        }
        
        return twitter.getAvailableTrends();
    }

    /**
     * Returns the top 10 trending topics for a specific location Twitter has trending 
     * topic information for. The response is an array of "trend" objects that encode 
     * the name of the trending topic, the query parameter that can be used to search 
     * for the topic on Search, and the direct URL that can be issued against Search. 
     * This information is cached for five minutes, and therefore users are discouraged 
     * from querying these endpoints faster than once every five minutes.  
     * Global trends information is also available from this API by using a WOEID of 1.
     *
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getLocationTrends}
     * 
     * @param woeid The WOEID of the location to be querying for
     * @return trends
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public Trends getLocationTrends(@Optional @Default(value = "1") int woeid) 
            throws TwitterException {
        return twitter.getLocationTrends(woeid);
    }
    /**
     * Returns the top 20 trending topics for each hour in a given day.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getDailyTrends}
     *
     * @param date            starting date of daily trends. If no date is specified, current
     *                        date is used
     * @param excludeHashTags whether hashtags should be excluded
     * @return a list of {@link Trends} objects
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public List<Trends> getDailyTrends(@Optional Date date,
                                       @Optional @Default("false") boolean excludeHashTags)
            throws TwitterException {
        return twitter.getDailyTrends(date, excludeHashTags);
    }

    /**
     * Returns the top 30 trending topics for each day in a given week.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getWeeklyTrends}
     *
     * @param date            starting date of daily trends. If no date is specified, current
     *                        date is used
     * @param excludeHashTags if all hashtags should be removed from the trends list.
     * @return a list of {@link Trends} objects
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public List<Trends> getWeeklyTrends(@Optional Date date,
                                        @Optional @Default("false") boolean excludeHashTags)
            throws TwitterException {
        return twitter.getWeeklyTrends(date, excludeHashTags);
    }


    /**
     * Asynchronously retrieves public statuses that match one or more filter predicates.
     * <p/>
     * At least a keyword or userId must be specified. Multiple parameters may be
     * specified.
     * <p/>
     * Placing long parameters in the URL may cause the request to be rejected for excessive URL length.
     * <p/>
     * The default access level allows up to 200 track keywords and 400 follow userids.
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:filteredStream}
     *
     * @param count    the number of previous statuses to stream before transitioning to the live stream.
     * @param userIds  the user ids to follow
     * @param keywords the keywords to track
     * @param callback the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void filteredStream(@Optional @Default("0") int count,
                               @Placement(group = "User Ids to Follow") @Optional List<Long> userIds,
                               @Placement(group = "Keywords to Track") @Optional List<String> keywords,
                               final SourceCallback callback) {
        listenToStatues(callback).filter(new FilterQuery(count, toLongArray(userIds), toStringArray(keywords)));
    }

    /**
     * Asynchronously retrieves a random sample of all public statuses. The sample
     * size and quality varies depending on the account permissions
     * <p/>
     * The default access level provides a small proportion of the Firehose. The "Gardenhose"
     * access level provides a proportion more suitable for data mining
     * and research applications that desire a larger proportion to be
     * statistically significant sample.
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:sampleStream}
     *
     * @param callback the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void sampleStream(final SourceCallback callback) {
        listenToStatues(callback).sample();
    }

    /**
     * Asynchronously retrieves all public statuses. This stream is not generally
     * available - it requires special permissions and its usage is discouraged by
     * Twitter
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:firehoseStream}
     *
     * @param count    Indicates the number of previous statuses to consider for delivery before transitioning to live
     *                 stream delivery.
     * @param callback the {@link SourceCallback} used to dispatch messageswhen a response is received
     */
    @Source
    public void firehoseStream(int count, final SourceCallback callback) {
        listenToStatues(callback).firehose(count);
    }

    /**
     * Asynchronously retrieves all statuses containing 'http:' and 'https:'. Like
     * Firehorse, its is not a generally available stream
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:linkStream}
     *
     * @param count    Indicates the number of previous statuses to consider for delivery before transitioning to live
     *                 stream delivery.
     * @param callback the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void linkStream(int count, final SourceCallback callback) {
        listenToStatues(callback).links(count);
    }

    /**
     * Retrieves the following user updates notifications:<br/>
     * - New Statuses <br/>
     * - Block/Unblock events <br/>
     * - Follow events <br/>
     * - User profile updates <br/>
     * - Retweets <br/>
     * - List creation/deletion <br/>
     * - List member addition/remotion <br/>
     * - List subscription/unsubscription <br/>
     * - List updates <br/>
     * - Profile updates <br/>
     * <p/>
     * Such notifications are represented as org.mule.twitter.UserEvent objects
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:userStream}
     *
     * @param keywords  the keywords to track for new statuses
     * @param callback_ the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void userStream(@Placement(group = "Keywords to Track") List<String> keywords, final SourceCallback callback_) {
        initStream();
        final SoftCallback callback = new SoftCallback(callback_);
        stream.addListener(new UserStreamAdapter() {
            @Override
            public void onException(Exception ex) {
                logger.warn("An exception occured while processing user stream", ex);
            }

            @Override
            public void onStatus(Status status) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.NEW_STATUS, status.getUser(), status));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onBlock(User source, User blockedUser) {
                try {
                    callback.process(UserEvent.fromTarget(EventType.BLOCK, source, blockedUser));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onFollow(User source, User followedUser) {
                try {
                    callback.process(UserEvent.fromTarget(EventType.FOLLOW, source, followedUser));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onRetweet(User source, User target, Status retweetedStatus) {
                try {
                    callback.process(UserEvent.from(EventType.RETWEET, source, target, retweetedStatus));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUnblock(User source, User unblockedUser) {
                try {
                    callback.process(UserEvent.fromTarget(EventType.UNBLOCK, source, unblockedUser));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListCreation(User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.LIST_CREATION, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListDeletion(User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.LIST_DELETION, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListMemberAddition(User addedMember, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_MEMBER_ADDITION, addedMember, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListMemberDeletion(User deletedMember, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_MEMBER_DELETION, deletedMember, listOwner,
                            list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListSubscription(User subscriber, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_SUBSCRIPTION, subscriber, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListUnsubscription(User subscriber, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_UNSUBSCRIPTION, subscriber, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListUpdate(User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.LIST_UPDATE, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserProfileUpdate(User updatedUser) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.PROFILE_UPDATE, updatedUser, null));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        stream.user(toStringArray(keywords));
    }


    /**
     * Asynchronously retrieves statutes for a set of supplied user's ids.
     * Site Streams are a beta service, so refer always to latest twitter documentation about them.
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:siteStream}
     *
     * @param userIds        ids of users to include in the stream
     * @param withFollowings withFollowings whether to receive status updates from people following
     * @param callback_      the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void siteStream(@Placement(group = "User Ids to Follow") List<Long> userIds,
                           @Optional @Default("false") boolean withFollowings,
                           final SourceCallback callback_) {
        initStream();
        final SoftCallback callback = new SoftCallback(callback_);
        stream.addListener(new SiteStreamsAdapter() {

            @Override
            public void onException(Exception ex) {
                logger.warn("An exception occured while processing site stream", ex);
            }

            @Override
            public void onStatus(long forUser, Status status) {
                try {
                    callback.process(status);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

        });
        stream.site(withFollowings, toLongArray(userIds));
    }

    /**
     * Sends a new direct message to the specified user from the authenticating user.
     * Requires both the user and text parameters below. The text will be trimmed if
     * the length of the text is exceeding 140 characters.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:sendDirectMessageByScreenName}
     *
     * @param screenName The screen name of the user to whom send the direct message
     * @param message    The text of your direct message
     * @return the {@link DirectMessage}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public DirectMessage sendDirectMessageByScreenName(String screenName, String message) throws TwitterException {
        return twitter.sendDirectMessage(screenName, message);
    }

    /**
     * Sends a new direct message to the specified user from the authenticating user.
     * Requires both the user and text parameters below. The text will be trimmed if
     * the length of the text is exceeding 140 characters.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:sendDirectMessageByUserId}
     *
     * @param userId  The user ID of the user to whom send the direct message
     * @param message The text of your direct message
     * @return the {@link DirectMessage}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public DirectMessage sendDirectMessageByUserId(long userId, String message) throws TwitterException {
        return twitter.sendDirectMessage(userId, message);
    }

    private void initStream() {
        if (stream != null) {
            throw new IllegalStateException("Only one stream can be consumed per twitter account");
        }
        this.stream = newStream();
    }

    private String[] toStringArray(List<String> list) {
        if (list == null) {
            return null;
        }
        return list.toArray(new String[list.size()]);
    }

    private TwitterStream listenToStatues(final SourceCallback callback_) {
        initStream();
        final SoftCallback callback = new SoftCallback(callback_);
        stream.addListener(new StatusAdapter() {
            @Override
            public void onException(Exception ex) {
                logger.warn("An exception occured while processing status stream", ex);
            }

            @Override
            public void onStatus(Status status) {
                try {
                    callback.process(status);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        return stream;
    }

    private TwitterStream newStream() {
        ConfigurationBuilder cb = new ConfigurationBuilder()
                .setUseSSL(useSSL)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setStreamBaseURL(STREAM_BASE_URL)
                .setSiteStreamBaseURL(SITE_STREAM_BASE_URL)
                .setHttpProxyHost(proxyHost)
                .setHttpProxyPort(proxyPort)
                .setHttpProxyUser(proxyUsername)
                .setHttpProxyPassword(proxyPassword);

        if (accessKey != null) {
            cb.setOAuthAccessToken(accessKey).setOAuthAccessTokenSecret(accessSecret);
        }

        HttpClientHiddenConstructionArgument.setUseMule(false);
        return new TwitterStreamFactory(cb.build()).getInstance();
    }

    private long[] toLongArray(List<Long> longList) {
        if (longList == null) {
            return null;
        }
        long[] ls = new long[longList.size()];
        for (int i = 0; i < longList.size(); i++) {
            ls[i] = longList.get(i);
        }
        return ls;
    }

    public Twitter getTwitterClient() {
        return twitter;
    }

    public boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public void setAccessKey(String accessToken) {
        this.accessKey = accessToken;
    }

    public void setAccessSecret(String accessTokenSecret) {
        this.accessSecret = accessTokenSecret;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    @Override
    public void setMuleContext(MuleContext context) {
        MuleHttpClient.setMuleContext(context);
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getAccessSecret() {
        return accessSecret;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    static final class SoftCallback implements SourceCallback {
        private final SourceCallback callback;

        public SoftCallback(SourceCallback callback) {
            this.callback = callback;
        }

        @Override
        public Object process() throws Exception {
            try {
                return callback.process();
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }

        @Override
        public Object process(Object payload) {
            try {
                return callback.process(payload);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }

        @Override
        public Object process(Object payload, Map<String, Object> properties) throws Exception {
            try {
                return callback.process(payload);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }

        @Override
        public MuleEvent processEvent(MuleEvent event) throws MuleException
        {
            try {
                return callback.processEvent(event);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }
    }
}