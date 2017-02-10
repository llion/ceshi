package com.color.home.widgets.singleline_scroll;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/2/10.
 */
public class RSSParser {

    private final static boolean DBG = false;
    private final static String TAG = "RSSParser";
    private String ns = null;

    public RssProgram parse(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        parser.nextTag();
        return readRssProgram(parser);
    }

    private RssProgram readRssProgram(XmlPullParser parser) throws XmlPullParserException, IOException {
//        RssProgram channel;
//         String title;
//         String link;
//         String description;
//         String language;
         String pubDate = null;
         Image image = null;
         List<RssItem> rssItems = new ArrayList<RssItem>();

        parser.require(XmlPullParser.START_TAG, null, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readRssProgram. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("channel")) {
                parser.require(XmlPullParser.START_TAG, null, "channel");

                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String tagName2 = parser.getName();
                    if (DBG)
                        Log.i(TAG, "readRssProgram. [tagName2=" + tagName2);
                    if (tagName2.equalsIgnoreCase("image")) {

                        image = readImage(parser);
                    } else  if (tagName2.equalsIgnoreCase("item")) {

                        RssItem rssItem = readRssItem(parser);
                        if (DBG)
                            Log.d(TAG, "rssItem= " + rssItem);
                        rssItems.add(rssItem);

                    } else {
                        skip(parser);
                    }
                }


            } else {
                skip(parser);
            }
        }

        if (DBG)
             Log.d(TAG, "after parse, mRssItems= " + rssItems);
        return new RssProgram(pubDate, image, rssItems);
    }

    private RssItem readRssItem(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "item");

         String title = null;
         String description = null;
         String pubDate = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readRssItem. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("title")) {
                title = readText(parser);
            } else  if (tagName.equalsIgnoreCase("description")) {
                description = readText(parser);
            } else if (tagName.equalsIgnoreCase("pubDate")) {
                pubDate = readText(parser);
            } else {
                skip(parser);
            }
        }
        return new RssItem(title, description, pubDate);
    }

    private Image readImage(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "image");
        String url = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readImage. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("url")) {
                url = readText(parser);
            } else {
                skip(parser);
            }
        }
        return new Image(url);
    }

    public static class RssProgram {
        public String title;
        public String link;
        public String description;
        public String language;
        public String pubDate;
        public Image image;
        public List<RssItem> rssItems;

        public RssProgram(String title, String link, String description, String language, String pubDate, Image image) {
            this.title = title;
            this.link = link;
            this.description = description;
            this.language = language;
            this.pubDate = pubDate;
            this.image = image;
        }

        public RssProgram(String pubDate, Image image, List<RssItem> rssItems) {
            this.pubDate = pubDate;
            this.image = image;
            this.rssItems = rssItems;
        }

        @Override
        public String toString() {
            return "RssProgram{" +
                    "title='" + title + '\'' +
                    ", link='" + link + '\'' +
                    ", description='" + description + '\'' +
                    ", language='" + language + '\'' +
                    ", pubDate='" + pubDate + '\'' +
                    ", image=" + image +
                    ", mRssItems=" + rssItems +
                    '}';
        }
    }


    public static class Image {
        public String title;
        public String link;
        public String url;

        public Image(String url) {
            this.url = url;
        }

        public Image(String title, String link, String url) {
            this.title = title;
            this.link = link;
            this.url = url;
        }

        @Override
        public String toString() {
            return "Image{" +
                    "title='" + title + '\'' +
                    ", link='" + link + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    public static class RssItem {
        public String title;
        public String description;
        public String link;
        public String pubDate;

        public RssItem(String title, String description, String pubDate) {
            this.title = title;
            this.description = description;
            this.pubDate = pubDate;
        }

        public RssItem(String title, String description, String link, String pubDate) {
            this.title = title;
            this.description = description;
            this.link = link;
            this.pubDate = pubDate;
        }

        @Override
        public String toString() {
            return "RssItem{" +
                    "title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", link='" + link + '\'' +
                    ", pubDate='" + pubDate + '\'' +
                    '}';
        }
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText().trim();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }



}
