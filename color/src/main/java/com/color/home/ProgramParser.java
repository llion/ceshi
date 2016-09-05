package com.color.home;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class parses XML from program.xml. Given an InputStream representation of a programs desc, it returns a List of programs, where each
 * list element represents a single program in the XML.
 */
public class ProgramParser {
    private final static boolean DBG = false;
    private final static String TAG = "ProgramParser";
    private File mVsnFile;

    public ProgramParser(File mVsnFile) {
        if (DBG) {
            Log.d(TAG, "Program Parse constructor to parse=" + mVsnFile);
        }
        this.mVsnFile = mVsnFile;
    }

    public File getmVsnFile() {
        return mVsnFile;
    }

    public static class DigitalClock {
        public String type;
        public String flags;
        public String name;
        public String isStrikeOut;
        public String weight;
        public String ftSize;
        public String ftColor;
        public String bItalic;
        public String bUnderline;
        public String bBold;
        public String charSet;

        public DigitalClock(String type, String flags, String name, String isStrikeOut, String weight, String ftSize, String ftColor, String bItalic,
                 String bUnderline, String bBold, String charSet) {
            this.type = type;
            this.flags = flags;
            this.name = name;
            this.isStrikeOut = isStrikeOut;
            this.weight = weight;
            this.ftSize = ftSize;
            this.ftColor = ftColor;
            this.bItalic = bItalic;
            this.bUnderline = bUnderline;
            this.bBold = bBold;
            this.charSet = charSet;
        }

    }

    public static class MultiPicInfo implements ResourceCollectable {
        public String source;
        public String picCount;
        public String onePicDuration;
        public FileSource filePath;

        public MultiPicInfo(String source, String picCount, String onePicDuration, FileSource filePath) {
            super();
            this.source = source;
            this.picCount = picCount;
            this.onePicDuration = onePicDuration;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            return "MultiPicInfo [source=" + source + ", picCount=" + picCount + ", onePicDuration=" + onePicDuration + ", filePath="
                    + filePath + "]";
        }

        @Override
        public void collectFile(Set<String> files) {
            if (filePath != null) {
                filePath.collectFile(files);
            }
        }

    }

    public static class ScrollPicInfo implements ResourceCollectable {
        public String source;
        public String picCount;
        public String onePicDuration;
        public FileSource filePath;

        public ScrollPicInfo(String source, String picCount, String onePicDuration, FileSource filePath) {
            super();
            this.source = source;
            this.picCount = picCount;
            this.onePicDuration = onePicDuration;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            return "ScrollPicInfo [source=" + source + ", picCount=" + picCount + ", onePicDuration=" + onePicDuration + ", filePath="
                    + filePath + "]";
        }

        @Override
        public void collectFile(Set<String> files) {
            if (filePath != null) {
                filePath.collectFile(files);
            }
        }

    }

    public static class LogFont {
        public String lfHeight;
        public String lfWidth;
        public String lfEscapement;
        public String lfOrientation;
        public String lfWeight;
        public String lfItalic;
        public String lfUnderline;
        public String lfStrikeOut;
        public String lfCharSet;
        public String lfOutPrecision;
        public String lfQuality;
        public String lfPitchAndFamily;
        public String lfFaceName;

        public LogFont(String lfHeight, String lfWidth, String lfEscapement, String lfOrientation, String lfWeight, String lfItalic,
                String lfUnderline, String lfStrikeOut, String lfCharSet, String lfOutPrecision, String lfQuality, String lfPitchAndFamily,
                String lfFaceName) {
            super();
            this.lfHeight = lfHeight;
            this.lfWidth = lfWidth;
            this.lfEscapement = lfEscapement;
            this.lfOrientation = lfOrientation;
            this.lfWeight = lfWeight;
            this.lfItalic = lfItalic;
            this.lfUnderline = lfUnderline;
            this.lfStrikeOut = lfStrikeOut;
            this.lfCharSet = lfCharSet;
            this.lfOutPrecision = lfOutPrecision;
            this.lfQuality = lfQuality;
            this.lfPitchAndFamily = lfPitchAndFamily;
            this.lfFaceName = lfFaceName;
        }

        @Override
        public String toString() {
            return "LogFont [lfHeight=" + lfHeight + ", lfWidth=" + lfWidth + ", lfEscapement=" + lfEscapement + ", lfOrientation="
                    + lfOrientation + ", lfWeight=" + lfWeight + ", lfItalic=" + lfItalic + ", lfUnderline=" + lfUnderline
                    + ", lfStrikeOut=" + lfStrikeOut + ", lfCharSet=" + lfCharSet + ", lfOutPrecision=" + lfOutPrecision + ", lfQuality="
                    + lfQuality + ", lfPitchAndFamily=" + lfPitchAndFamily + ", lfFaceName=" + lfFaceName + "]";
        }

        public String getIdString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append(lfHeight).append(lfWidth).append(lfEscapement).append(lfOrientation).append(lfWeight)
                    .append(lfItalic).append(lfUnderline).append(lfStrikeOut).append(lfCharSet).append(lfOutPrecision).append(lfQuality)
                    .append(lfPitchAndFamily).append(lfFaceName);
            if (DBG)
                Log.d(TAG, "lfHeight = " + lfHeight + ", lfWidth = " + lfWidth + ", lfEscapement = " + lfEscapement + ", lfOrientation = " + lfOrientation
                 + ", lfWeight = " + lfWeight + ", lfItalic = " + lfItalic + ", lfUnderline = " + lfUnderline + ", lfStrikeOut = " + lfStrikeOut
                 + ", lfCharSet = " + lfCharSet + ", lfOutPrecision = " + lfOutPrecision + ", lfQuality = " + lfQuality + ", lfPitchAndFamily = " + lfPitchAndFamily + ", lfFaceName = " + lfFaceName);
            return sb.toString();
        }

    }

    public static class Effect {
        public String Type;
        public String Time;
        public String repeatX;
        public String repeatY;
        public String IsTran;

        public Effect(String type, String time, String repeatX, String repeatY, String isTran) {
            Type = type;
            Time = time;
            this.repeatX = repeatX;
            this.repeatY = repeatY;
            IsTran = isTran;
        }

        @Override
        public String toString() {
            return "Effect [Type=" + Type + ", Time=" + Time + ", repeatX=" + repeatX + ", repeatY=" + repeatY + ", IsTran=" + IsTran + "]";
        }

    }

    public static class EffectType {
        public String isstatic;
        public String staytype;

        public EffectType(String isstatic, String staytype) {
            super();
            this.isstatic = isstatic;
            this.staytype = staytype;
        }

        @Override
        public String toString() {
            return "EffectType [isstatic=" + isstatic + ", staytype=" + staytype + "]";
        }

    }

    public static class DisplayRect {
        public String x;
        public String y;
        public String width;
        public String height;
        public String borderwidth;
        public String bordercolor;

        public DisplayRect(String x, String y, String width, String height, String borderwidth, String bordercolor) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.borderwidth = borderwidth;
            this.bordercolor = bordercolor;
        }

        @Override
        public String toString() {
            return "DisplayRect [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", borderwidth=" + borderwidth
                    + ", bordercolor=" + bordercolor + "]";
        }

    }

    public static class ItemRect {
        public String x;
        public String y;
        public String width;
        public String height;

        public ItemRect(String x, String y, String width, String height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return "ItemRect [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
        }

    }

    public static class Item implements ResourceCollectable {
        private Texts mTexts;
        public String id;
        public String name;
        public String type;
        public String version;
        public String backcolor;
        public String alhpa;
        public String duration;
        public String beglaring;
        public EffectType effect;
        public Effect ineffect;
        public Effect outeffect;
        public MultiPicInfo multipicinfo;
        public ScrollPicInfo scrollpicinfo;
        public String beToEndTime;
        public String style;
        public String isMultiLine;
        public String prefix;
        public String endDateTime;
        public String showFormat;
        public LogFont logfont;
        public String text;
        public String textColor;
        public String isShowDayCount;
        public String dayCountColor;
        public String isShowHourCount;
        public String hourCountColor;
        public String isShowMinuteCount;
        public String minuteCountColor;
        public String isShowSecondCount;
        public String secondCountColor;
        public String width;
        public String height;
        public FileSource filesource;
        public String reserveAS;
        public String isfromfile;
        public String isscroll;
        public String speed;
        public String isheadconnecttail;
        public String wordspacing;
        public String repeatcount;
        public String isscrollbytime;
        public String movedir;
        public String length;
        public String videoWidth;
        public String videoHeight;
        public String inOffset;
        public String playLength;
        public String volume;
        public String showx;
        public String showy;
        public String loop;
        public String showwidth;
        public String showheight;
        public String issetshowregion;
        public String issetplaylen;
        public String ifspeedbyframe;
        public String speedbyframe;
        public String url;
        public String centeralalign;
        public String regionname;
        public String isshowweather;
        public String temperatureprefix;
        public String isshowtemperature;
        public String windprefix;
        public String isshowwind;
        public String airprefix;
        public String isshowair;
        public String ultraviolet;
        public String isshowultraviolet;
        public String movementindex;
        public String isshowmovementindex;
        public String coldindex;
        public String isshowcoldindex;
        public String humidity;
        public String serverType;
        public String regioncode;
        public String isshowhumidity;
        public String longitud;
        public String latitude;
        public String timezone;
        public String zoneDescripId;
        public String language;
        public String useproxy;
        public String proxyserver;
        public String proxyport;
        public String proxyuser;
        public String proxypsw;
        public String isshowpic;
        public String showstyle;
        public String isAnalog;
        public DigitalClock digitalClock;
        public AnologClock anologClock;
        public HhourScale hhourScale;
        public MinuteScale minuteScale;
        public String invertClr;
        public ProgramParser mPp;
        public ItemRect itemRect;

        public Item(String id, String name, String type, String version, String backcolor, String alhpa, String duration,
                    String beglaring, EffectType effect, Effect ineffect, Effect outeffect, MultiPicInfo multipicinfo,
                    String beToEndTime, String style, String isMultiLine, String prefix, String endDateTime, String showFormat,
                    LogFont logfont, String text, String textColor, String isShowDayCount, String dayCountColor, String isShowHourCount,
                    String hourCountColor, String isShowMinuteCount, String minuteCountColor, String isShowSecondCount,
                    String secondCountColor, String width, String height, FileSource filesource, String reserveAS, String isfromfile,
                    String isscroll, String speed, String isheadconnecttail, String wordspacing, String repeatcount, String isscrollbytime,
                    String movedir, String length, String videoWidth, String videoHeight, String inOffset, String playLength, String volume,
                    String showx, String showy, String loop, String showwidth, String showheight, String issetshowregion, String issetplaylen,
                    String ifspeedbyframe, String speedbyframe, String url, String centeralalign, String regionname, String isshowweather,
                    String temperatureprefix, String isshowtemperature, String windprefix, String isshowwind, String airprefix, String isshowair,
                    String ultraviolet, String isshowultraviolet, String movementindex, String isshowmovementindex, String coldindex,
                    String isshowcoldindex, String humidity, String serverType, String regioncode, String isshowhumidity, String longitud,
                    String latitude, String timezone, String zoneDescripId, String language, String useproxy, String proxyserver, String proxyport,
                    String proxyuser, String proxypsw, String isshowpic, String showstyle, String isAnalog, DigitalClock digitalClock,
                    AnologClock anologClock, HhourScale hhourScale, MinuteScale minuteScale, ScrollPicInfo scrollpicinfo, String invertClr, ProgramParser pp, ItemRect itemRect) {

            super();
            this.mPp = pp;

            this.id = id;
            this.name = name;
            this.type = type;
            this.version = version;
            this.backcolor = backcolor;
            this.alhpa = alhpa;
            this.duration = duration;
            this.beglaring = beglaring;
            this.effect = effect;
            this.ineffect = ineffect;
            this.outeffect = outeffect;
            this.multipicinfo = multipicinfo;
            this.beToEndTime = beToEndTime;
            this.style = style;
            this.isMultiLine = isMultiLine;
            this.prefix = prefix;
            this.endDateTime = endDateTime;
            this.showFormat = showFormat;
            this.logfont = logfont;
            this.text = text;
            this.textColor = textColor;
            this.isShowDayCount = isShowDayCount;
            this.dayCountColor = dayCountColor;
            this.isShowHourCount = isShowHourCount;
            this.hourCountColor = hourCountColor;
            this.isShowMinuteCount = isShowMinuteCount;
            this.minuteCountColor = minuteCountColor;
            this.isShowSecondCount = isShowSecondCount;
            this.secondCountColor = secondCountColor;
            this.width = width;
            this.height = height;
            this.filesource = filesource;
            this.reserveAS = reserveAS;
            this.isfromfile = isfromfile;
            this.isscroll = isscroll;
            this.speed = speed;
            this.isheadconnecttail = isheadconnecttail;
            this.wordspacing = wordspacing;
            this.repeatcount = repeatcount;
            this.isscrollbytime = isscrollbytime;
            this.movedir = movedir;
            this.length = length;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.inOffset = inOffset;
            this.playLength = playLength;
            this.volume = volume;
            this.showx = showx;
            this.showy = showy;
            this.loop = loop;
            this.showwidth = showwidth;
            this.showheight = showheight;
            this.issetshowregion = issetshowregion;
            this.issetplaylen = issetplaylen;
            this.ifspeedbyframe = ifspeedbyframe;
            this.speedbyframe = speedbyframe;
            this.url = url;
            this.centeralalign = centeralalign;
            this.regionname = regionname;
            this.isshowweather = isshowweather;
            this.temperatureprefix = temperatureprefix;
            this.isshowtemperature = isshowtemperature;
            this.windprefix = windprefix;
            this.isshowwind = isshowwind;
            this.airprefix = airprefix;
            this.isshowair = isshowair;
            this.ultraviolet = ultraviolet;
            this.isshowultraviolet = isshowultraviolet;
            this.movementindex = movementindex;
            this.isshowmovementindex = isshowmovementindex;
            this.coldindex = coldindex;
            this.isshowcoldindex = isshowcoldindex;
            this.humidity = humidity;
            this.serverType = serverType;
            this.regioncode = regioncode;
            this.isshowhumidity = isshowhumidity;
            this.longitud = longitud;
            this.latitude = latitude;
            this.timezone = timezone;
            this.zoneDescripId = zoneDescripId;
            this.language = language;
            this.useproxy = useproxy;
            this.proxyserver = proxyserver;
            this.proxyport = proxyport;
            this.proxyuser = proxyuser;
            this.proxypsw = proxypsw;
            this.isshowpic = isshowpic;
            this.showstyle = showstyle;
            this.isAnalog = isAnalog;
            this.digitalClock = digitalClock;
            this.anologClock = anologClock;
            this.hhourScale = hhourScale;
            this.minuteScale = minuteScale;
            this.scrollpicinfo = scrollpicinfo;
            this.invertClr = invertClr;
            this.itemRect = itemRect;

        }

        public String getAbsFilePath() {
            String absFilePath = mPp.getmVsnFile().getParentFile().getAbsolutePath() + filesource.filepath;
            if (DBG) {
                Log.d(TAG, "getAbsFilePath absFilePath=" + absFilePath);
            }
            return absFilePath;
        }

        @Override
        public void collectFile(Set<String> files) {
            if (multipicinfo != null) {
                multipicinfo.collectFile(files);
            }

            if (scrollpicinfo != null) {
                scrollpicinfo.collectFile(files);
            }

            if (filesource != null) {
                filesource.collectFile(files);
            }

        }

        private HashCode mHash;

        public HashCode getTextBitmapHash() {
            if (mHash != null)
                return mHash;

            // Single line text.
            if ("4".equals(type)) {
                final String idString = logfont.getIdString();
                // 16 is the string length of textColor and beglaring.
                // textColor.length() + beglaring.length()
                String texts = text;
                if ("1".equals(isfromfile))
                    texts = getTexts().mText;
                int length = 0;
                if (texts != null)
                    length = texts.length();

                final StringBuilder sb = new StringBuilder(length + idString.length() + 16);
                sb.append(texts).append(textColor).append(beglaring).append(idString);
                mHash = Hashing.sha1().hashString(sb.toString(), Charset.forName("UTF-16"));
                // mHash = Hashing.sha1().hashString(sb.toString(), Charset.forName("iso-8859-1"));
                if (DBG) {
                    // Log.d(TAG, "Error:" + Hex.encodeHex(new byte[] { 0x2f, 0x34}));
                    Log.d(TAG, "getTextBitmapHash. [sb=" + sb.toString() + ", mHash=" + mHash
                            + ", textColor = " + textColor + ", beglaring = " + beglaring);
                }

                return mHash;
            } else {
                return null;
            }

        }

        private Object mMeta;

        public Object getMeta() {
            return mMeta;
        }

        public void setMeta(Object meta) {
            mMeta = meta;
        }

        public Texts getTexts() {
            if (mTexts == null) {
                mTexts = new Texts(this);
            }

            return mTexts;
        }

        public void setTexts(Texts texts) {
            mTexts = texts;
        }

    }

    public static class VideoItem extends Item {

        private final boolean isSeekable;

        public VideoItem(String id, String name, String type, String version, String backcolor, String alhpa, String duration, String beglaring, EffectType effect, Effect ineffect, Effect outeffect, MultiPicInfo multipicinfo, String beToEndTime, String style, String isMultiLine, String prefix, String endDateTime, String showFormat, LogFont logfont, String text, String textColor, String isShowDayCount, String dayCountColor, String isShowHourCount, String hourCountColor,
                         String isShowMinuteCount, String minuteCountColor, String isShowSecondCount, String secondCountColor, String width, String height, FileSource filesource, String reserveAS, String isfromfile, String isscroll, String speed, String isheadconnecttail, String wordspacing, String repeatcount, String isscrollbytime, String movedir, String length, String videoWidth, String videoHeight, String inOffset, String playLength, String volume, String showx, String showy, String loop, String showwidth, String showheight, String issetshowregion, String issetplaylen, String ifspeedbyframe, String speedbyframe, String url, String centeralalign, String regionname, String isshowweather, String temperatureprefix, String isshowtemperature, String windprefix, String isshowwind, String airprefix, String isshowair, String ultraviolet, String isshowultraviolet, String movementindex, String isshowmovementindex, String coldindex, String isshowcoldindex, String humidity, String serverType, String regioncode, String isshowhumidity, String longitud, String latitude, String timezone, String zoneDescripId, String language, String useproxy, String proxyserver, String proxyport, String proxyuser, String proxypsw, String isshowpic, String showstyle, String isAnalog, DigitalClock digitalClock, AnologClock anologClock, HhourScale hhourScale, MinuteScale minuteScale, ScrollPicInfo scrollpicinfo, String invertClr, ProgramParser pp, ItemRect itemRect) {
            super(id, name, type, version, backcolor, alhpa, duration, beglaring, effect, ineffect, outeffect, multipicinfo, beToEndTime, style,
                    isMultiLine, prefix, endDateTime, showFormat, logfont, text, textColor, isShowDayCount, dayCountColor, isShowHourCount, hourCountColor,
                    isShowMinuteCount, minuteCountColor, isShowSecondCount, secondCountColor, width, height, filesource, reserveAS, isfromfile, isscroll, speed, isheadconnecttail, wordspacing, repeatcount, isscrollbytime, movedir, length, videoWidth, videoHeight, inOffset, playLength, volume, showx, showy, loop, showwidth, showheight, issetshowregion, issetplaylen, ifspeedbyframe, speedbyframe, url, centeralalign, regionname, isshowweather, temperatureprefix, isshowtemperature, windprefix, isshowwind, airprefix, isshowair, ultraviolet, isshowultraviolet, movementindex, isshowmovementindex, coldindex, isshowcoldindex, humidity, serverType, regioncode, isshowhumidity, longitud, latitude, timezone, zoneDescripId, language, useproxy, proxyserver, proxyport, proxyuser, proxypsw, isshowpic, showstyle, isAnalog, digitalClock, anologClock, hhourScale, minuteScale, scrollpicinfo, invertClr, pp, itemRect);

            isSeekable = checkSeekable();
        }

        private boolean checkSeekable() {
            if (DBG)
                Log.d(TAG, "checkSeekable, isFileExists()=" + isFileExists());

            // Mpeg2 file is not seekable.
            //  || absFilePath.endsWith(".mpg") ".mpeg" || absFilePath.endsWith(".wmv")
            final String absFilePath = getAbsFilePath();
            // Only make the .mp4 seekable. All the others, in doubt, and make'em unseekable.
            if (absFilePath.endsWith(".mp4") || absFilePath.endsWith(".mov")) {
                return true;
            }

            return false;
        }

        private boolean isFileExists() {
            return "1".equals(filesource.isrelative)
                    && (new File(getAbsFilePath()).exists());
        }

        public boolean isSeekable() {
            return isSeekable;
        }
    }

    public static class Region implements Comparable<Region>, ResourceCollectable {
        public String id;
        public String name;
        public String type;
        public String show;
        public String layer;
        public DisplayRect rect;
        public ArrayList<Item> items;

        public Region(String id, String name, String type, String show, String layer, DisplayRect rect, ArrayList<Item> items) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.show = show;
            this.layer = layer;
            this.rect = rect;
            this.items = items;
        }

        @Override
        public String toString() {
            return "Region [id=" + id + ", name=" + name + ", type=" + type + ", show=" + show + ", layer=" + layer + ", rect=" + rect
                    + ", items=" + dumpItems() + "]";
        }

        public String dumpItems() {
            StringBuffer sb = new StringBuffer();
            int index = 0;
            for (Item item : items) {
                sb.append("Item index=" + index + "\n");
                sb.append(item);
                sb.append("\n");
                index++;
            }
            return sb.toString();
        }

        @Override
        public int compareTo(Region another) {
            int anotherLayer = Integer.parseInt(another.layer);
            int thisLayer = Integer.parseInt(this.layer);

            return anotherLayer - thisLayer;
        }

        @Override
        public void collectFile(Set<String> files) {
            if (items != null) {
                for (Item item : items) {
                    item.collectFile(files);
                }
            }

        }
    }

    public static class BgAudio implements ResourceCollectable {
        // never public, so that another class won't be messed up.
        private final static String TAG = "ProgramParser.BgAudio";
        public FileSource filesource;
        public String length;
        public String inoffset;
        public String playlenth;
        public String volume;
        public String isloop;

        public BgAudio(FileSource filesource, String length, String inoffset, String playlenth, String volume, String isloop) {
            super();
            this.filesource = filesource;
            this.length = length;
            this.inoffset = inoffset;
            this.playlenth = playlenth;
            this.volume = volume;
            this.isloop = isloop;
        }

        @Override
        public String toString() {
            return "BgAudio [filesource=" + filesource + ", length=" + length + ", inoffset=" + inoffset + ", playlenth=" + playlenth
                    + ", volume=" + volume + ", isloop=" + isloop + "]";
        }

        @Override
        public void collectFile(Set<String> files) {
            if (DBG)
                Log.i(TAG, "collectFile. ");

            if (filesource != null) {
                filesource.collectFile(files);
            }
        }

    }

    public static class BgFile extends FileSource {

        public BgFile(String isrelative, String filepath, String MD5) {
            super(isrelative, filepath, MD5);
        }
    }

    public static class FileSource implements ResourceCollectable {
        public String isrelative;
        public String filepath;
        public String MD5;

        public FileSource(String isrelative, String filepath, String MD5) {
            this.isrelative = isrelative;
            this.MD5 = MD5;
            // Replace windows path to linux.
            this.filepath = filepath.replace("\\", "/").replace("./", "/");
        }

        @Override
        public String toString() {
            return "FileSource [isrelative=" + isrelative + ", filepath=" + filepath + "]";
        }

        public boolean isValidFileSource() {
            return !TextUtils.isEmpty(filepath);

        }

        @Override
        public void collectFile(Set<String> files) {
            if ("1".equals(isrelative) && !TextUtils.isEmpty(filepath)) {
                if (DBG)
                    Log.i(TAG, "collectFile. Add file=" + filepath);
                files.add(filepath);
            }
        }

    }

    public static class Page implements ResourceCollectable {
        public String id;
        public String name;
        public String visible;
        public String appointduration;
        public Integer bgColor;
        public String looptype;
        public Information info;
        public BgFile bgfile;
        public List<BgAudio> bgaudios;
        public List<Region> regions;

        public Page(String id, String name, String visible, String appointduration, String looptype, Integer bgColor, Information info,
                BgFile bgfile, List<BgAudio> bgaudios, List<Region> regions) {
            this.id = id;
            this.name = name;
            this.visible = visible;
            this.appointduration = appointduration;
            this.looptype = looptype;
            this.bgColor = bgColor;
            this.info = info;
            this.bgfile = bgfile;
            this.bgaudios = bgaudios;

            if (DBG)
                Log.i(TAG, "Page dumpRegion before sort. regions layers=" + dumpLayer(regions));

            if (regions != null) {
                Collections.sort(regions);
                this.regions = regions;
            }

            if (DBG)
                Log.i(TAG, "Page dumpRegion after sort. regions layers=" + dumpLayer(this.regions));
        }

        private String dumpLayer(List<Region> regions) {
            if (regions == null)
                return "No regions";

            StringBuffer sb = new StringBuffer();
            for (Region region : regions) {
                sb.append("Region layer=" + region.layer + "\n");
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            return "Page [id=" + id + ", name=" + name + ", visible=" + visible + ", appointduration=" + appointduration + ", bgColor="
                    + bgColor + ", looptype=" + looptype + ", info=" + info + ", bgfile=" + bgfile + ", bgaudios=" + bgaudios
                    + ", regions="
                    + regions + "]";
        }

        public String dumpRegion() {
            if (regions == null)
                return "No regions";

            int index = 0;
            StringBuffer sb = new StringBuffer();
            for (Region region : regions) {
                sb.append("Region index=" + index + "\n");
                sb.append(region);
                sb.append("\n");
                index++;
            }
            return sb.toString();
        }

        @Override
        public void collectFile(Set<String> files) {
            if (bgfile != null) {
                bgfile.collectFile(files);
            }
            if (bgaudios != null) {
                for (BgAudio ba : bgaudios) {
                    ba.collectFile(files);
                }
            }
            if (regions != null) {
                for (Region region : regions) {
                    region.collectFile(files);
                }
            }
        }

    }

    public static class Information {
        public String width;
        public String height;
        public String duration;
        public String description;
        public String creator;
        public String createtime;
        public String lastmodifytime;

        public Information(String width, String height, String duration, String description, String creator, String createtime,
                String lastmodifytime) {
            this.width = width;
            this.height = height;
            this.duration = duration;
            this.description = description;
            this.creator = creator;
            this.createtime = createtime;
            this.lastmodifytime = lastmodifytime;
        }

        @Override
        public String toString() {
            return "Information. [width=" + width + ", height=" + height + ", duration=" + duration + ", description=" + description
                    + ", creator=" + creator + ", createtime=" + createtime + ", lastmodifytime=" + lastmodifytime;
        }

    }

    private static final String ns = null;

    // We don't use namespaces

    public List<Program> parse(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        parser.nextTag();
        return readPrograms(parser);
    }

    private List<Program> readPrograms(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Program> programs = new ArrayList<Program>();

        parser.require(XmlPullParser.START_TAG, ns, "Programs");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readPrograms. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("program")) {
                programs.add(readProgram(parser));
            } else {
                skip(parser);
            }
        }
        return programs;
    }

    public static class Program implements ResourceCollectable {
        public final String id;
        public final String name;
        public final String version;
        public final Information info;
        public final ArrayList<Page> pages;

        public Program(String id, String name, String version, Information info, ArrayList<Page> pages) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.info = info;
            this.pages = pages;
        }

        public static Set<String> collectFiles(List<Program> programs) {
            if (programs == null) {
                if (DBG)
                    Log.d(TAG, "collectFiles. [no program.");
                return null;
            }
            if (DBG)
                Log.i(TAG, "collectFiles. ");

            Set<String> resources = new HashSet<String>(10);
            for (ResourceCollectable prog : programs) {
                if (DBG)
                    Log.i(TAG, "collectFiles. [program=" + prog);
                prog.collectFile(resources);
                if (DBG) {
                    for (String filepath : resources) {
                        Log.i(TAG, "collectFiles. dump filepath=" + filepath);
                    }
                }
            }

            return resources;
        }

        @Override
        public String toString() {
            return "Program. [id=" + id + ", name=" + name + ", version=" + version + ", info=" + info + ", Pages=\n" + dumpPages();
        }

        @Override
        public void collectFile(Set<String> files) {
            for (Page page : pages) {
                page.collectFile(files);
            }
        }

        private String dumpPages() {
            StringBuffer sb = new StringBuffer();
            int index = 0;
            for (Page page : pages) {
                sb.append("Page index=" + index + "\n");
                sb.append(page);
                sb.append("\n");
                index++;
            }
            return sb.toString();
        }
    }

    private Program readProgram(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Program");

        String id = null;
        String programName = null;
        String version = null;
        Information info = null;
        ArrayList<Page> pages = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readProgram. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("id")) {
                id = readId(parser);
            } else if (tagName.equalsIgnoreCase("name")) {
                programName = readName(parser);
            } else if (tagName.equalsIgnoreCase("version")) {
                version = readVersion(parser);
            } else if (tagName.equalsIgnoreCase("Information")) {
                info = readInformation(parser);
            } else if (tagName.equalsIgnoreCase("Pages")) {
                pages = readPages(parser);
            } else {
                skip(parser);
            }
        }
        return new Program(id, programName, version, info, pages);
    }

    private ArrayList<Page> readPages(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<Page> pages = new ArrayList<Page>();

        parser.require(XmlPullParser.START_TAG, ns, "Pages");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readPages. [tagName=" + tagName);
            // Starts by looking for the tag
            if (tagName.equalsIgnoreCase("Page")) {
                pages.add(readPage(parser));
            } else {
                skip(parser);
            }
        }
        return pages;
    }

    private Page readPage(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Page");

        String id = null;
        String name = null;
        String visible = null;
        String appointduration = null;
        String looptype = null;
        Integer bgColor = null;
        Information info = null;
        BgFile bgfile = null;
        List<BgAudio> bgaudios = null;
        List<Region> regions = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readPage. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("id")) {
                id = readId(parser);
            } else if (tagName.equalsIgnoreCase("name")) {
                name = readName(parser);
            } else if (tagName.equalsIgnoreCase("Visibale")) {
                visible = readText(parser);
            } else if (tagName.equalsIgnoreCase("AppointDuration")) {
                appointduration = readText(parser);
            } else if (tagName.equalsIgnoreCase("LoopType")) {
                looptype = readText(parser);
            } else if (tagName.equalsIgnoreCase("BgColor")) {
                try {
                    bgColor = Color.parseColor(readText(parser).replace("0x", "#"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (tagName.equalsIgnoreCase("BgFile")) {
                bgfile = readBgFile(parser);
            } else if (tagName.equalsIgnoreCase("BgAudios")) {
                bgaudios = readBgAudios(parser);
            } else if (tagName.equalsIgnoreCase("Regions")) {
                regions = readRegions(parser);
            } else {
                skip(parser);
            }
        }
        return new Page(id, name, visible, appointduration, looptype, bgColor, info, bgfile, bgaudios, regions);
    }

    private List<BgAudio> readBgAudios(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<BgAudio> bgaudios = new ArrayList<BgAudio>();
        parser.require(XmlPullParser.START_TAG, ns, "BgAudios");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readBgAudios. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("BgAudio")) {
                bgaudios.add(readBgAudio(parser));
            } else {
                skip(parser);
            }
        }
        return bgaudios;
    }

    private BgAudio readBgAudio(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "BgAudio");

        FileSource filesource = null;
        String length = null;
        String inoffset = null;
        String playlenth = null;
        String volume = null;
        String isloop = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readBgAudio. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("FileSource")) {
                filesource = readFileSource(parser);
            } else if (tagName.equalsIgnoreCase("Lenth")) {
                length = readText(parser);
            } else if (tagName.equalsIgnoreCase("InOffset")) {
                inoffset = readText(parser);
            } else if (tagName.equalsIgnoreCase("PlayLenth")) {
                playlenth = readText(parser);
            } else if (tagName.equalsIgnoreCase("Volume")) {
                volume = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsLoop")) {
                isloop = readText(parser);
            } else {
                if (DBG)
                    Log.w(TAG, "readBgAudio. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new BgAudio(filesource, length, inoffset, playlenth, volume, isloop);
    }

    private ArrayList<Region> readRegions(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<Region> regions = new ArrayList<Region>();

        parser.require(XmlPullParser.START_TAG, ns, "Regions");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readRegions. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("Region")) {
                regions.add(readRegion(parser));
            } else {
                skip(parser);
            }
        }
        return regions;
    }

    private Region readRegion(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Region");

        String id = null;
        String name = null;
        String type = null;
        String show = null;
        String layer = null;
        DisplayRect rect = null;
        ArrayList<Item> items = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readRegion. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Id")) {
                id = readId(parser);
            } else if (tagName.equalsIgnoreCase("Name")) {
                name = readName(parser);
            } else if (tagName.equalsIgnoreCase("type")) {
                type = readText(parser);
            } else if (tagName.equalsIgnoreCase("Show")) {
                show = readText(parser);
            } else if (tagName.equalsIgnoreCase("Layer")) {
                layer = readText(parser);
            } else if (tagName.equalsIgnoreCase("Rect")) {
                rect = readDisplayRect(parser);
            } else if (tagName.equalsIgnoreCase("Items")) {
                items = readItems(parser);
            } else {
                Log.w(TAG, "readRegion. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new Region(id, name, type, show, layer, rect, items);
    }

    private ArrayList<Item> readItems(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<Item> items = new ArrayList<Item>();

        parser.require(XmlPullParser.START_TAG, ns, "Items");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readItems. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("Item")) {
                items.add(readItem(parser));
            } else {
                skip(parser);
            }
        }
        return items;
    }

    private Item readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Item");

        String id = null;
        String name = null;
        String type = null;
        String version = null;
        String backcolor = null;
        String alhpa = null;
        String duration = null;
        String beglaring = null;
        EffectType effect = null;
        Effect ineffect = null;
        Effect outeffect = null;
        MultiPicInfo multipicinfo = null;
        ScrollPicInfo scrollpicinfo = null;
        String beToEndTime = null;
        String style = null;
        String isMultiLine = null;
        String prefix = null;
        String endDateTime = null;
        String showFormat = null;
        LogFont logfont = null;
        String text = null;
        String textColor = null;
        String isShowDayCount = null;
        String dayCountColor = null;
        String isShowHourCount = null;
        String hourCountColor = null;
        String isShowMinuteCount = null;
        String minuteCountColor = null;
        String isShowSecondCount = null;
        String secondCountColor = null;
        String width = null;
        String height = null;
        FileSource filesource = null;
        String reserveAS = null;
        String isfromfile = null;
        String isscroll = null;
        String speed = null;
        String isheadconnecttail = null;
        String wordspacing = null;
        String repeatcount = null;
        String isscrollbytime = null;
        String playLength = null;
        String movedir = null;
        String length = null;
        String videoWidth = null;
        String videoHeight = null;
        String inOffset = null;
        String volume = null;
        String showx = null;
        String showy = null;
        String showwidth = null;
        String showheight = null;
        String issetshowregion = null;
        String issetplaylen = null;
        String loop = null;
        String ifspeedbyframe = null;
        String speedbyframe = null;
        String centeralalign = null;
        String regionname = null;
        String isshowweather = null;
        String temperatureprefix = null;
        String isshowtemperature = null;
        String windprefix = null;
        String isshowwind = null;
        String airprefix = null;
        String isshowair = null;
        String ultraviolet = null;
        String isshowultraviolet = null;
        String movementindex = null;
        String isshowmovementindex = null;
        String coldindex = null;
        String isshowcoldindex = null;
        String humidity = null;
        String serverType = null;
        String regioncode = null;
        String isshowhumidity = null;
        String longitud = null;
        String latitude = null;
        String timezone = null;
        String zoneDescripId = null;
        String language = null;
        String useproxy = null;
        String proxyserver = null;
        String proxyport = null;
        String proxyuser = null;
        String proxypsw = null;
        String isshowpic = null;
        String showstyle = null;
        String url = null;
        String isAnalog = null;
        DigitalClock digitalClock = null;
        AnologClock anologClock = null;
        HhourScale hhourScale = null;
        MinuteScale minuteScale = null;
        String invertClr = null;
        ItemRect itemRect = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readItem. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Id")) {
                id = readId(parser);
            } else if (tagName.equalsIgnoreCase("Name")) {
                name = readName(parser);
            } else if (tagName.equalsIgnoreCase("Type")) {
                type = readText(parser);
            } else if (tagName.equalsIgnoreCase("Version")) {
                version = readVersion(parser);
            } else if (tagName.equalsIgnoreCase("BackColor")) {
                backcolor = readText(parser);
            } else if (tagName.equalsIgnoreCase("Alhpa")) {
                alhpa = readText(parser);
            } else if (tagName.equalsIgnoreCase("Duration")) {
                duration = readText(parser);
            } else if (tagName.equalsIgnoreCase("BeGlaring")) {
                beglaring = readText(parser);
            } else if (tagName.equalsIgnoreCase("effect")) {
                effect = readEffectType(parser);
            } else if (tagName.equalsIgnoreCase("inEffect")) {
                ineffect = readEffect(parser);
            } else if (tagName.equalsIgnoreCase("outEffect")) {
                outeffect = readEffect(parser);
            } else if (tagName.equalsIgnoreCase("MultiPicInfo")) {
                multipicinfo = readMultiPicInfo(parser);
            } else if (tagName.equalsIgnoreCase("ScrollPicInfo")) {
                scrollpicinfo = readScrollPicInfo(parser);
            } else if (tagName.equalsIgnoreCase("BeToEndTime")) {
                beToEndTime = readText(parser);
            } else if (tagName.equalsIgnoreCase("Style")) {
                style = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsMultiLine")) {
                isMultiLine = readText(parser);
            } else if (tagName.equalsIgnoreCase("Prefix")) {
                prefix = readText(parser);
            } else if (tagName.equalsIgnoreCase("EndDateTime")) {
                endDateTime = readText(parser);
            } else if (tagName.equalsIgnoreCase("ShowFormat")) {
                showFormat = readText(parser);
            } else if (tagName.equalsIgnoreCase("LogFont")) {
                logfont = readLogFont(parser);
            } else if (tagName.equalsIgnoreCase("Text")) {
                text = readText(parser);
            } else if (tagName.equalsIgnoreCase("TextColor")) {
                textColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowDayCount")) {
                isShowDayCount = readText(parser);
            } else if (tagName.equalsIgnoreCase("DayCountColor")) {
                dayCountColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowHourCount")) {
                isShowHourCount = readText(parser);
            } else if (tagName.equalsIgnoreCase("HourCountColor")) {
                hourCountColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowMinuteCount")) {
                isShowMinuteCount = readText(parser);
            } else if (tagName.equalsIgnoreCase("MinuteCountColor")) {
                minuteCountColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowSecondCount")) {
                isShowSecondCount = readText(parser);
            } else if (tagName.equalsIgnoreCase("secondCountColor")) {
                secondCountColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("Width")) {
                width = readWidth(parser);
            } else if (tagName.equalsIgnoreCase("Height")) {
                height = readHeight(parser);
            } else if (tagName.equalsIgnoreCase("FileSource")) {
                filesource = readFileSource(parser);
            } else if (tagName.equalsIgnoreCase("ReserveAS")) {
                reserveAS = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsFromFile")) {
                isfromfile = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsScroll")) {
                isscroll = readText(parser);
            } else if (tagName.equalsIgnoreCase("Speed")) {
                speed = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsHeadConnectTail")) {
                isheadconnecttail = readText(parser);
            } else if (tagName.equalsIgnoreCase("WordSpacing")) {
                wordspacing = readText(parser);
            } else if (tagName.equalsIgnoreCase("invertClr")) {
                invertClr = readText(parser);
            } else if (tagName.equalsIgnoreCase("RepeatCount")) {
                repeatcount = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsScrollByTime")) {
                isscrollbytime = readText(parser);
            } else if (tagName.equalsIgnoreCase("PlayLenth")) {
                playLength = readText(parser);
            } else if (tagName.equalsIgnoreCase("MoveDir")) {
                movedir = readText(parser);
            } else if (tagName.equalsIgnoreCase("Length")) {
                length = readText(parser);
            } else if (tagName.equalsIgnoreCase("VideoWidth")) {
                videoWidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("VideoHeight")) {
                videoHeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("InOffset")) {
                inOffset = readText(parser);
            } else if (tagName.equalsIgnoreCase("PlayLength") || tagName.equalsIgnoreCase("PlayLenth")) {
                playLength = readText(parser);
            } else if (tagName.equalsIgnoreCase("Volume")) {
                volume = readText(parser);
            } else if (tagName.equalsIgnoreCase("ShowX")) {
                showx = readText(parser);
            } else if (tagName.equalsIgnoreCase("ShowY")) {
                showy = readText(parser);
            } else if (tagName.equalsIgnoreCase("Loop")) {
                loop = readText(parser);
            } else if (tagName.equalsIgnoreCase("showWidth")) {
                showwidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("showHeight")) {
                showheight = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsSetShowRegion")) {
                issetshowregion = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsSetPlayLen")) {
                issetplaylen = readText(parser);
            } else if (tagName.equalsIgnoreCase("IfSpeedByFrame")) {
                ifspeedbyframe = readText(parser);
            } else if (tagName.equalsIgnoreCase("SpeedByFrame")) {
                speedbyframe = readText(parser);
            } else if (tagName.equalsIgnoreCase("centeralAlign")) {
                centeralalign = readText(parser);
            } else if (tagName.equalsIgnoreCase("RegionName")) {
                regionname = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowWeather")) {
                isshowweather = readText(parser);
            } else if (tagName.equalsIgnoreCase("WeatherPrefix")) {
                windprefix = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowTemperature")) {
                isshowtemperature = readText(parser);
            } else if (tagName.equalsIgnoreCase("TemperaturePrefix")) {
                temperatureprefix = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowWind")) {
                isshowwind = readText(parser);
            } else if (tagName.equalsIgnoreCase("WindPrefix")) {
                windprefix = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowAir")) {
                isshowair = readText(parser);
            } else if (tagName.equalsIgnoreCase("AirPrefix")) {
                airprefix = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowUltraviolet")) {
                isshowultraviolet = readText(parser);
            } else if (tagName.equalsIgnoreCase("Ultraviolet")) {
                ultraviolet = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowMovementIndex")) {
                isshowmovementindex = readText(parser);
            } else if (tagName.equalsIgnoreCase("MovementIndex")) {
                movementindex = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowColdIndex")) {
                isshowcoldindex = readText(parser);
            } else if (tagName.equalsIgnoreCase("ColdIndex")) {
                coldindex = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowHumidity")) {
                isshowhumidity = readText(parser);
            } else if (tagName.equalsIgnoreCase("Humidity")) {
                humidity = readText(parser);
            } else if (tagName.equalsIgnoreCase("serverType")) {
                serverType = readText(parser);
            } else if (tagName.equalsIgnoreCase("regionCode")) {
                regioncode = readText(parser);
            } else if (tagName.equalsIgnoreCase("longitud")) {
                longitud = readText(parser);
            } else if (tagName.equalsIgnoreCase("latitude")) {
                latitude = readText(parser);
            } else if (tagName.equalsIgnoreCase("timezone")) {
                timezone = readText(parser);
            } else if (tagName.equalsIgnoreCase("ZoneDescripID")) {
                zoneDescripId = readText(parser);
            } else if (tagName.equalsIgnoreCase("Language")) {
                language = readText(parser);
            } else if (tagName.equalsIgnoreCase("useproxy")) {
                useproxy = readText(parser);
            } else if (tagName.equalsIgnoreCase("proxyServer")) {
                proxyserver = readText(parser);
            } else if (tagName.equalsIgnoreCase("proxyPort")) {
                proxyport = readText(parser);
            } else if (tagName.equalsIgnoreCase("proxyUser")) {
                proxyuser = readText(parser);
            } else if (tagName.equalsIgnoreCase("proxyPsw")) {
                proxypsw = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsShowPic")) {
                isshowpic = readText(parser);
            } else if (tagName.equalsIgnoreCase("ShowStyle")) {
                showstyle = readText(parser);
            } else if (tagName.equalsIgnoreCase("Url")) {
                url = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsAnolog")) {
                isAnalog = readText(parser);
            } else if (tagName.equalsIgnoreCase("DigtalClock")) {
                digitalClock = readDigtalClock(parser);
            } else if (tagName.equalsIgnoreCase("AnologClock")) {
                anologClock = readAnologClock(parser);
            } else if (tagName.equalsIgnoreCase("HhourScale")) {
                hhourScale = readHhourScale(parser);
            } else if (tagName.equalsIgnoreCase("MinuteScale")) {
                minuteScale = readMinuteScale(parser);
            } else if (tagName.equalsIgnoreCase("Rect")) {
                itemRect = readItemRect(parser);
            } else {
                if (DBG)
                    Log.w(TAG, "readItem. [Skipping tag=" + tagName);
                skip(parser);
            }
        }

        // Video has it's own class.
        if ("3".equals(type)) {
            return new VideoItem(id, name, type, version, backcolor, alhpa, duration, beglaring, effect, ineffect, outeffect, multipicinfo, beToEndTime, style,
                    isMultiLine, prefix, endDateTime, showFormat, logfont, text, textColor, isShowDayCount, dayCountColor, isShowHourCount, hourCountColor,
                    isShowMinuteCount, minuteCountColor, isShowSecondCount, secondCountColor, width, height, filesource, reserveAS, isfromfile, isscroll, speed,
                    isheadconnecttail, wordspacing, repeatcount, isscrollbytime, movedir, length, videoWidth, videoHeight, inOffset, playLength, volume, showx, showy, loop,
                    showwidth, showheight, issetshowregion, issetplaylen, ifspeedbyframe, speedbyframe, url, centeralalign, regionname, isshowweather, temperatureprefix,
                    isshowtemperature, windprefix, isshowwind, airprefix, isshowair, ultraviolet, isshowultraviolet, movementindex, isshowmovementindex,
                    coldindex, isshowcoldindex, humidity, serverType, regioncode, isshowhumidity, longitud, latitude, timezone, zoneDescripId,
                    language, useproxy, proxyserver, proxyport, proxyuser, proxypsw, isshowpic, showstyle, isAnalog, digitalClock, anologClock, hhourScale, minuteScale,
                    scrollpicinfo, invertClr, this, itemRect);
        } else {
            return new Item(id, name, type, version, backcolor, alhpa, duration, beglaring, effect, ineffect, outeffect, multipicinfo, beToEndTime, style,
                    isMultiLine, prefix, endDateTime, showFormat, logfont, text, textColor, isShowDayCount, dayCountColor, isShowHourCount, hourCountColor,
                    isShowMinuteCount, minuteCountColor, isShowSecondCount, secondCountColor, width, height, filesource, reserveAS, isfromfile, isscroll, speed,
                    isheadconnecttail, wordspacing, repeatcount, isscrollbytime, movedir, length, videoWidth, videoHeight, inOffset, playLength, volume, showx, showy, loop,
                    showwidth, showheight, issetshowregion, issetplaylen, ifspeedbyframe, speedbyframe, url, centeralalign, regionname, isshowweather, temperatureprefix,
                    isshowtemperature, windprefix, isshowwind, airprefix, isshowair, ultraviolet, isshowultraviolet, movementindex, isshowmovementindex,
                    coldindex, isshowcoldindex, humidity, serverType, regioncode, isshowhumidity, longitud, latitude, timezone, zoneDescripId,
                    language, useproxy, proxyserver, proxyport, proxyuser, proxypsw, isshowpic, showstyle, isAnalog, digitalClock, anologClock, hhourScale, minuteScale,
                    scrollpicinfo, invertClr, this, itemRect);
        }

    }

    private DigitalClock readDigtalClock(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "DigtalClock");

        String type = null;
        String flags = null;
        String name = null;
        String isStrikeOut = null;
        String weight = null;
        String ftSize = null;
        String ftColor = null;
        String bItalic = null;
        String bUnderline = null;
        String bBold = null;
        String charSet = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readDigtalClock. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("DigtalClock")) {
                type = readText(parser);
            } else if (tagName.equalsIgnoreCase("Flags")) {
                flags = readText(parser);
            } else if (tagName.equalsIgnoreCase("Name")) {
                name = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsStrikeOut")) {
                isStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("Weight")) {
                weight = readText(parser);
            } else if (tagName.equalsIgnoreCase("ftSize")) {
                ftSize = readText(parser);
            } else if (tagName.equalsIgnoreCase("ftColor")) {
                ftColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("bItalic")) {
                bItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("bUnderline")) {
                bUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("bBold")) {
                bBold = readText(parser);
            } else if (tagName.equalsIgnoreCase("CharSet")) {
                charSet = readText(parser);
            } else {
                Log.w(TAG, "readDigtalClock. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new DigitalClock(type, flags, name, isStrikeOut, weight, ftSize, ftColor, bItalic, bUnderline, bBold, charSet);
    }

    private MultiPicInfo readMultiPicInfo(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "MultiPicInfo");

        String source = null;
        String picCount = null;
        String onePicDuration = null;
        FileSource filePath = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readMultiPicInfo. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Source")) {
                source = readText(parser);
            } else if (tagName.equalsIgnoreCase("PicCount")) {
                picCount = readText(parser);
            } else if (tagName.equalsIgnoreCase("OnePicDuration")) {
                onePicDuration = readText(parser);
            } else if (tagName.equalsIgnoreCase("FilePath")) {
                filePath = readFileSource(parser);
            } else {
                Log.w(TAG, "readMultiPicInfo. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new MultiPicInfo(source, picCount, onePicDuration, filePath);
    }

    private ScrollPicInfo readScrollPicInfo(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "ScrollPicInfo");

        String source = null;
        String picCount = null;
        String onePicDuration = null;
        FileSource filePath = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readScrollPicInfo. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Source")) {
                source = readText(parser);
            } else if (tagName.equalsIgnoreCase("PicCount")) {
                picCount = readText(parser);
            } else if (tagName.equalsIgnoreCase("OnePicDuration")) {
                onePicDuration = readText(parser);
            } else if (tagName.equalsIgnoreCase("FilePath")) {
                filePath = readFileSource(parser);
            } else {
                Log.w(TAG, "readScrollPicInfo. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new ScrollPicInfo(source, picCount, onePicDuration, filePath);
    }

    private LogFont readLogFont(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "LogFont");

        String lfHeight = null;
        String lfWidth = null;
        String lfEscapement = null;
        String lfOrientation = null;
        String lfWeight = null;
        String lfItalic = null;
        String lfUnderline = null;
        String lfStrikeOut = null;
        String lfCharSet = null;
        String lfOutPrecision = null;
        String lfQuality = null;
        String lfPitchAndFamily = null;
        String lfFaceName = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readLogFont. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("lfHeight")) {
                lfHeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWidth")) {
                lfWidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfEscapement")) {
                lfEscapement = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOrientation")) {
                lfOrientation = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWeight")) {
                lfWeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfItalic")) {
                lfItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfUnderline")) {
                lfUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfStrikeOut")) {
                lfStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfCharSet")) {
                lfCharSet = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOutPrecision")) {
                lfOutPrecision = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfQuality")) {
                lfQuality = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfPitchAndFamily")) {
                lfPitchAndFamily = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfFaceName")) {
                lfFaceName = readText(parser);
            } else {
                Log.w(TAG, "readLogFont. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new LogFont(lfHeight, lfWidth, lfEscapement, lfOrientation, lfWeight, lfItalic, lfUnderline, lfStrikeOut, lfCharSet,
                lfOutPrecision, lfQuality, lfPitchAndFamily, lfFaceName);
    }

    private Effect readEffect(XmlPullParser parser) throws XmlPullParserException, IOException {
        // parser.require(XmlPullParser.START_TAG, ns, "inEffect");
        // parser.require(XmlPullParser.START_TAG, ns, "outEffect");

        String type = null;
        String time = null;
        String repeatx = null;
        String repeaty = null;
        String istran = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readEffect. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Type")) {
                type = readText(parser);
            } else if (tagName.equalsIgnoreCase("Time")) {
                time = readText(parser);
            } else if (tagName.equalsIgnoreCase("repeatX")) {
                repeatx = readText(parser);
            } else if (tagName.equalsIgnoreCase("repeatY")) {
                repeaty = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsTran")) {
                istran = readText(parser);
            } else {
                Log.w(TAG, "readEffect. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new Effect(type, time, repeatx, repeaty, istran);
    }

    private EffectType readEffectType(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "effect");

        String isstatic = null;
        String staytype = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readEffectType. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("IsStatic")) {
                isstatic = readText(parser);
            } else if (tagName.equalsIgnoreCase("StayType")) {
                staytype = readText(parser);
            } else {
                Log.w(TAG, "readEffectType. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new EffectType(isstatic, staytype);
    }

    private DisplayRect readDisplayRect(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Rect");

        String x = null;
        String y = null;
        String width = null;
        String height = null;
        String borderwidth = null;
        String bordercolor = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readDisplayRect. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("X")) {
                x = readText(parser);
            } else if (tagName.equalsIgnoreCase("Y")) {
                y = readText(parser);
            } else if (tagName.equalsIgnoreCase("Width")) {
                width = readWidth(parser);
            } else if (tagName.equalsIgnoreCase("Height")) {
                height = readHeight(parser);
            } else if (tagName.equalsIgnoreCase("BorderWidth")) {
                borderwidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("BorderColor")) {
                bordercolor = readText(parser);
            } else {
                Log.w(TAG, "readDisplayRect. [Skipping tag=" + tagName);
                skip(parser);
            }
        }

        if (DBG)
            Log.d(TAG, "readDisplayRect. [width=" + width + ", height=" + height);
        return new DisplayRect(x, y, width, height, borderwidth, bordercolor);
    }

    private ItemRect readItemRect(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Rect");

        String x = null;
        String y = null;
        String width = null;
        String height = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readItemRect. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("X")) {
                x = readText(parser);
            } else if (tagName.equalsIgnoreCase("Y")) {
                y = readText(parser);
            } else if (tagName.equalsIgnoreCase("Width")) {
                width = readWidth(parser);
            } else if (tagName.equalsIgnoreCase("Height")) {
                height = readHeight(parser);
            } else {
                Log.w(TAG, "readItemRect. [Skipping tag=" + tagName);
                skip(parser);
            }
        }

        if (DBG)
            Log.d(TAG, "readItemRect. [width=" + width + ", height=" + height);
        return new ItemRect(x, y, width, height);
    }

    private FileSource readFileSource(XmlPullParser parser) throws XmlPullParserException, IOException {
        // parser.require(XmlPullParser.START_TAG, ns, "FileSource");
        // parser.require(XmlPullParser.START_TAG, ns, "FilePath"); // Under
        // MultiPicInfo

        String isrelative = null;
        String filepath = null;
        String MD5 = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readFileSource. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("IsRelative")) {
                isrelative = readText(parser);
            } else if (tagName.equalsIgnoreCase("FilePath")) {
                filepath = readText(parser);
            } else if (tagName.equalsIgnoreCase("MD5")) {
                MD5 = readText(parser);
            } else {
                Log.w(TAG, "readFileSource. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new FileSource(isrelative, filepath, MD5);
    }

    private BgFile readBgFile(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "BgFile");

        String isrelative = null;
        String filepath = null;
        String MD5 = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readBgFile. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("IsRelative")) {
                isrelative = readText(parser);
            } else if (tagName.equalsIgnoreCase("FilePath")) {
                filepath = readText(parser);
            } else if (tagName.equalsIgnoreCase("MD5")) {
                MD5 = readText(parser);
            } else {
                Log.w(TAG, "readBgFile. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new BgFile(isrelative, filepath, MD5);
    }

    private Information readInformation(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Information");

        String width = null;
        String height = null;
        String duration = null;
        String description = null;
        String creator = null;
        String createtime = null;
        String lastmodifytime = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readInformation. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Width")) {
                width = readWidth(parser);
            } else if (tagName.equalsIgnoreCase("Height")) {
                height = readHeight(parser);

            } else if (tagName.equalsIgnoreCase("Duration")) {
                duration = readText(parser);
                // Read text from here.
            } else if (tagName.equalsIgnoreCase("Description")) {
                description = readText(parser);
            } else if (tagName.equalsIgnoreCase("Creator")) {
                creator = readText(parser);
            } else if (tagName.equalsIgnoreCase("CreateTime")) {
                createtime = readText(parser);
            } else if (tagName.equalsIgnoreCase("LastModifyTime")) {
                lastmodifytime = readText(parser);
            } else {
                Log.w(TAG, "readInformation. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new Information(width, height, duration, description, creator, createtime, lastmodifytime);
    }

    private String readHeight(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Height");
        String height = readText(parser);

        if (DBG)
            Log.i(TAG, "readHeight. [Height" + height);

        parser.require(XmlPullParser.END_TAG, ns, "Height");
        return height;
    }

    private String readWidth(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Width");
        String width = readText(parser);

        if (DBG)
            Log.i(TAG, "readWidth. [Width" + width);

        parser.require(XmlPullParser.END_TAG, ns, "Width");
        return width;
    }

    private String readId(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Id");
        String id = readText(parser);

        if (DBG)
            Log.i(TAG, "readId. [id" + id);

        parser.require(XmlPullParser.END_TAG, ns, "Id");
        return id;
    }

    // Processes link tags in the feed.
    private String readVersion(XmlPullParser parser) throws IOException, XmlPullParserException {
        String version = "";
        parser.require(XmlPullParser.START_TAG, ns, "Version");
        String tagName = parser.getName();

        if (DBG)
            Log.i(TAG, "readVersion. [tagName=" + tagName);

        // String relType = parser.getAttributeValue(null, "rel");
        // if (tag.equals("Version")) {
        // if (relType.equals("alternate")) {
        // link = parser.getAttributeValue(null, "href");
        // parser.nextTag();
        // }
        // }

        version = readText(parser);

        parser.require(XmlPullParser.END_TAG, ns, "Version");
        return version;
    }

    // Processes summary tags in the feed.
    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Name");
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Name");
        return name;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText().trim();
            parser.nextTag();
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested
    // tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps
    // going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being
    // 0).
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

    public static class AnologClock {
        public String type;
        public String shape;
        public String flags;
        public String name;
        public String isStrikeOut;
        public String weight;
        public String ftSize;
        public String ftColor;
        public String bItalic;
        public String bUnderline;
        public String bBold;
        public String charSet;
        public String hourPinClr;
        public String minutePinClr;
        public String secondPinClr;
        public String textOffset;
        public String dateOffset;
        public String lunarOffset;
        public String weekOffset;
        public ClockFont clockFont;

        public AnologClock(String type, String shape, String flags, String name, String isStrikeOut, String weight, String ftColor, String ftSize, String bItalic, String bUnderline, String bBold, String charSet, String hourPinClr, String minutePinClr, String secondPinClr, String textOffset, String dateOffset, String lunarOffset, String weekOffset, ClockFont clockFont) {
            this.type = type;
            this.shape = shape;
            this.flags = flags;
            this.name = name;
            this.isStrikeOut = isStrikeOut;
            this.weight = weight;
            this.ftColor = ftColor;
            this.ftSize = ftSize;
            this.bItalic = bItalic;
            this.bUnderline = bUnderline;
            this.bBold = bBold;
            this.charSet = charSet;
            this.hourPinClr = hourPinClr;
            this.minutePinClr = minutePinClr;
            this.secondPinClr = secondPinClr;
            this.textOffset = textOffset;
            this.dateOffset = dateOffset;
            this.lunarOffset = lunarOffset;
            this.weekOffset = weekOffset;
            this.clockFont = clockFont;
        }
    }

    public static class ClockFont {
        public HourFont hourFont;
        public FixedDate fixedDate;
        public FixedText fixedText;
        public FixedWeek fixedWeek;
        public NongLi nongLi;
        public String fixedTextBold;
        public String fixedTextColor;
        public String weekBold;
        public String weekColor;
        public String dateBold;
        public String dateColor;

        public ClockFont(HourFont hourFont, FixedDate fixedDate, FixedText fixedText, FixedWeek fixedWeek, NongLi nongLi, String fixedTextBold, String fixedTextColor, String weekBold, String weekColor, String dateBold, String dateColor) {
            this.hourFont = hourFont;
            this.fixedDate = fixedDate;
            this.fixedText = fixedText;
            this.fixedWeek = fixedWeek;
            this.nongLi = nongLi;
            this.fixedTextBold = fixedTextBold;
            this.fixedTextColor = fixedTextColor;
            this.weekBold = weekBold;
            this.weekColor = weekColor;
            this.dateBold = dateBold;
            this.dateColor = dateColor;
        }
    }

    public static class HhourScale {
        public String clr;
        public String shape;
        public String width;
        public String height;

        public HhourScale(String clr, String shape, String width, String height) {
            this.clr = clr;
            this.shape = shape;
            this.width = width;
            this.height = height;
        }
    }

    public static class MinuteScale {
        public String clr;
        public String shape;
        public String width;
        public String height;

        public MinuteScale(String clr, String shape, String width, String height) {
            this.clr = clr;
            this.shape = shape;
            this.width = width;
            this.height = height;
        }
    }

    public static class HourFont {
        public String lfHeight;
        public String lfWidth;
        public String lfEscapement;
        public String lfOrientation;
        public String lfWeight;
        public String lfItalic;
        public String lfUnderline;
        public String lfStrikeOut;
        public String lfCharSet;
        public String lfOutPrecision;
        public String lfQuality;
        public String lfPitchAndFamily;
        public String lfFaceName;

        public HourFont(String lfHeight, String lfWidth, String lfEscapement, String lfOrientation, String lfWeight, String lfItalic, String lfUnderline, String lfStrikeOut, String lfCharSet, String lfOutPrecision, String lfQuality, String lfPitchAndFamily, String lfFaceName) {
            this.lfHeight = lfHeight;
            this.lfWidth = lfWidth;
            this.lfEscapement = lfEscapement;
            this.lfOrientation = lfOrientation;
            this.lfWeight = lfWeight;
            this.lfItalic = lfItalic;
            this.lfUnderline = lfUnderline;
            this.lfStrikeOut = lfStrikeOut;
            this.lfCharSet = lfCharSet;
            this.lfOutPrecision = lfOutPrecision;
            this.lfQuality = lfQuality;
            this.lfPitchAndFamily = lfPitchAndFamily;
            this.lfFaceName = lfFaceName;
        }
    }

    public static class FixedText {
        public String lfHeight;
        public String lfWidth;
        public String lfEscapement;
        public String lfOrientation;
        public String lfWeight;
        public String lfItalic;
        public String lfUnderline;
        public String lfStrikeOut;
        public String lfCharSet;
        public String lfOutPrecision;
        public String lfQuality;
        public String lfPitchAndFamily;
        public String lfFaceName;

        public FixedText(String lfHeight, String lfWidth, String lfEscapement, String lfOrientation, String lfWeight, String lfItalic, String lfUnderline, String lfCharSet, String lfStrikeOut, String lfOutPrecision, String lfQuality, String lfPitchAndFamily, String lfFaceName) {
            this.lfHeight = lfHeight;
            this.lfWidth = lfWidth;
            this.lfEscapement = lfEscapement;
            this.lfOrientation = lfOrientation;
            this.lfWeight = lfWeight;
            this.lfItalic = lfItalic;
            this.lfUnderline = lfUnderline;
            this.lfCharSet = lfCharSet;
            this.lfStrikeOut = lfStrikeOut;
            this.lfOutPrecision = lfOutPrecision;
            this.lfQuality = lfQuality;
            this.lfPitchAndFamily = lfPitchAndFamily;
            this.lfFaceName = lfFaceName;
        }
    }

    public static class NongLi {
        public String lfHeight;
        public String lfWidth;
        public String lfEscapement;
        public String lfOrientation;
        public String lfWeight;
        public String lfItalic;
        public String lfUnderline;
        public String lfStrikeOut;
        public String lfCharSet;
        public String lfOutPrecision;
        public String lfQuality;
        public String lfPitchAndFamily;
        public String lfFaceName;

        public NongLi(String lfHeight, String lfWidth, String lfEscapement, String lfOrientation, String lfWeight, String lfItalic, String lfUnderline, String lfCharSet, String lfStrikeOut, String lfOutPrecision, String lfQuality, String lfPitchAndFamily, String lfFaceName) {
            this.lfHeight = lfHeight;
            this.lfWidth = lfWidth;
            this.lfEscapement = lfEscapement;
            this.lfOrientation = lfOrientation;
            this.lfWeight = lfWeight;
            this.lfItalic = lfItalic;
            this.lfUnderline = lfUnderline;
            this.lfCharSet = lfCharSet;
            this.lfStrikeOut = lfStrikeOut;
            this.lfOutPrecision = lfOutPrecision;
            this.lfQuality = lfQuality;
            this.lfPitchAndFamily = lfPitchAndFamily;
            this.lfFaceName = lfFaceName;
        }
    }

    public static class FixedDate {
        public String lfHeight;
        public String lfWidth;
        public String lfEscapement;
        public String lfOrientation;
        public String lfWeight;
        public String lfItalic;
        public String lfUnderline;
        public String lfStrikeOut;
        public String lfCharSet;
        public String lfOutPrecision;
        public String lfQuality;
        public String lfPitchAndFamily;
        public String lfFaceName;

        public FixedDate(String lfHeight, String lfWidth, String lfEscapement, String lfOrientation, String lfWeight, String lfItalic, String lfUnderline, String lfCharSet, String lfStrikeOut, String lfOutPrecision, String lfQuality, String lfPitchAndFamily, String lfFaceName) {
            this.lfHeight = lfHeight;
            this.lfWidth = lfWidth;
            this.lfEscapement = lfEscapement;
            this.lfOrientation = lfOrientation;
            this.lfWeight = lfWeight;
            this.lfItalic = lfItalic;
            this.lfUnderline = lfUnderline;
            this.lfCharSet = lfCharSet;
            this.lfStrikeOut = lfStrikeOut;
            this.lfOutPrecision = lfOutPrecision;
            this.lfQuality = lfQuality;
            this.lfPitchAndFamily = lfPitchAndFamily;
            this.lfFaceName = lfFaceName;
        }
    }

    public static class FixedWeek {
        public String lfHeight;
        public String lfWidth;
        public String lfEscapement;
        public String lfOrientation;
        public String lfWeight;
        public String lfItalic;
        public String lfUnderline;
        public String lfStrikeOut;
        public String lfCharSet;
        public String lfOutPrecision;
        public String lfQuality;
        public String lfPitchAndFamily;
        public String lfFaceName;

        public FixedWeek(String lfHeight, String lfWidth, String lfEscapement, String lfOrientation, String lfWeight, String lfItalic, String lfUnderline, String lfCharSet, String lfStrikeOut, String lfOutPrecision, String lfQuality, String lfPitchAndFamily, String lfFaceName) {
            this.lfHeight = lfHeight;
            this.lfWidth = lfWidth;
            this.lfEscapement = lfEscapement;
            this.lfOrientation = lfOrientation;
            this.lfWeight = lfWeight;
            this.lfItalic = lfItalic;
            this.lfUnderline = lfUnderline;
            this.lfCharSet = lfCharSet;
            this.lfStrikeOut = lfStrikeOut;
            this.lfOutPrecision = lfOutPrecision;
            this.lfQuality = lfQuality;
            this.lfPitchAndFamily = lfPitchAndFamily;
            this.lfFaceName = lfFaceName;
        }
    }

    private AnologClock readAnologClock(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "AnologClock");

        String type = null;
        String shape = null;
        String flags = null;
        String name = null;
        String isStrikeOut = null;
        String weight = null;
        String ftSize = null;
        String ftColor = null;
        String bItalic = null;
        String bUnderline = null;
        String bBold = null;
        String charSet = null;
        String hourPinClr = null;
        String minutePinClr = null;
        String secondPinClr = null;
        String textOffset = null;
        String dateOffset = null;
        String lunarOffset = null;
        String weekOffset = null;
        ClockFont clockFont = null;


        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readAnologClock. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("AnologClock")) {
                type = readText(parser);
            } else if (tagName.equalsIgnoreCase("Shape")) {
                shape = readText(parser);
            } else if (tagName.equalsIgnoreCase("Flags")) {
                flags = readText(parser);
            } else if (tagName.equalsIgnoreCase("Name")) {
                name = readText(parser);
            } else if (tagName.equalsIgnoreCase("IsStrikeOut")) {
                isStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("Weight")) {
                weight = readText(parser);
            } else if (tagName.equalsIgnoreCase("ftSize")) {
                ftSize = readText(parser);
            } else if (tagName.equalsIgnoreCase("ftColor")) {
                ftColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("bItalic")) {
                bItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("bUnderline")) {
                bUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("bBold")) {
                bBold = readText(parser);
            } else if (tagName.equalsIgnoreCase("CharSet")) {
                charSet = readText(parser);
            } else if (tagName.equalsIgnoreCase("HourPinClr")) {
                hourPinClr = readText(parser);
            } else if (tagName.equalsIgnoreCase("MinutePinClr")) {
                minutePinClr = readText(parser);
            } else if (tagName.equalsIgnoreCase("SecondPinClr")) {
                secondPinClr = readText(parser);
            } else if (tagName.equalsIgnoreCase("TextOffset")) {
                textOffset = readText(parser);
            } else if (tagName.equalsIgnoreCase("DateOffset")) {
                dateOffset = readText(parser);
            } else if (tagName.equalsIgnoreCase("LunarOffset")) {
                lunarOffset = readText(parser);
            } else if (tagName.equalsIgnoreCase("WeekOffset")) {
                weekOffset = readText(parser);
            } else if (tagName.equalsIgnoreCase("ClockFont")) {
                clockFont = readClockFont(parser);
            } else {
                Log.w(TAG, "readAnologClock. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new AnologClock(type, shape, flags, name, isStrikeOut, weight, ftColor, ftSize, bItalic, bUnderline, bBold, charSet,
                hourPinClr, minutePinClr, secondPinClr, textOffset, dateOffset, lunarOffset, weekOffset, clockFont);

    }

    private ClockFont readClockFont(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "ClockFont");

        HourFont hourFont = null;
        FixedText fixedText = null;
        FixedWeek fixedWeek = null;
        FixedDate fixedDate = null;
        NongLi nongLi = null;
        String fixedTextBold = null;
        String fixedTextColor = null;
        String weekBold = null;
        String weekColor = null;
        String dateBold = null;
        String dateColor = null;
        int i = 0;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readClockFont. [tagName=" + tagName);
            if (tagName.equalsIgnoreCase("Time")) {
                hourFont = readHourFont(parser);
            } else if (tagName.equalsIgnoreCase("FixedText")) {
                fixedText = readFixedText(parser);
            } else if (tagName.equalsIgnoreCase("Week")) {
                fixedWeek = readFixedWeek(parser);
            } else if (tagName.equalsIgnoreCase("Date")) {
                fixedDate = readFixedDate(parser);
            } else if (tagName.equalsIgnoreCase("NongLi")) {
                nongLi = readNongLi(parser);
            } else if (tagName.equalsIgnoreCase("FixedTextBold")) {
                fixedTextBold = readText(parser);
            } else if (tagName.equalsIgnoreCase("FixedTextColor")) {
                fixedTextColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("WeekBold")) {
                weekBold = readText(parser);
            } else if (tagName.equalsIgnoreCase("WeekColor")) {
                weekColor = readText(parser);
            } else if (tagName.equalsIgnoreCase("DateBold")) {
                dateBold = readText(parser);
            } else if (tagName.equalsIgnoreCase("DateColor") && (i == 0)) {
                dateColor = readText(parser);
                i++;
            } else {
                Log.w(TAG, "readClockFont. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new ClockFont(hourFont, fixedDate, fixedText, fixedWeek, nongLi, fixedTextBold, fixedTextColor, weekBold, weekColor, dateBold, dateColor);
    }

    private HhourScale readHhourScale(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "HhourScale");

        String clr = null;
        String shape = null;
        String width = null;
        String height = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readHhourScale. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Clr")) {
                clr = readText(parser);
            } else if (tagName.equalsIgnoreCase("Shape")) {
                shape = readText(parser);
            } else if (tagName.equalsIgnoreCase("Width")) {
                width = readText(parser);
            } else if (tagName.equalsIgnoreCase("Height")) {
                height = readText(parser);
            } else {
                Log.w(TAG, "readHhourScale. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new HhourScale(clr, shape, width, height);
    }

    private MinuteScale readMinuteScale(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "MinuteScale");

        String clr = null;
        String shape = null;
        String width = null;
        String height = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readMinuteScale. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("Clr")) {
                clr = readText(parser);
            } else if (tagName.equalsIgnoreCase("Shape")) {
                shape = readText(parser);
            } else if (tagName.equalsIgnoreCase("Width")) {
                width = readText(parser);
            } else if (tagName.equalsIgnoreCase("Height")) {
                height = readText(parser);
            } else {
                Log.w(TAG, "readMinuteScale. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new MinuteScale(clr, shape, width, height);
    }

    private HourFont readHourFont(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Time");

        String lfHeight = null;
        String lfWidth = null;
        String lfEscapement = null;
        String lfOrientation = null;
        String lfWeight = null;
        String lfItalic = null;
        String lfUnderline = null;
        String lfStrikeOut = null;
        String lfCharSet = null;
        String lfOutPrecision = null;
        String lfQuality = null;
        String lfPitchAndFamily = null;
        String lfFaceName = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readHourFont. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("lfHeight")) {
                lfHeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWidth")) {
                lfWidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfEscapement")) {
                lfEscapement = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOrientation")) {
                lfOrientation = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWeight")) {
                lfWeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfItalic")) {
                lfItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfUnderline")) {
                lfUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfStrikeOut")) {
                lfStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfCharSet")) {
                lfCharSet = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOutPrecision")) {
                lfOutPrecision = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfQuality")) {
                lfQuality = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfPitchAndFamily")) {
                lfPitchAndFamily = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfFaceName")) {
                lfFaceName = readText(parser);
            } else {
                Log.w(TAG, "readHourFont. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new HourFont(lfHeight, lfWidth, lfEscapement, lfOrientation, lfWeight, lfItalic, lfUnderline, lfStrikeOut, lfCharSet, lfOutPrecision, lfQuality, lfPitchAndFamily, lfFaceName);
    }

    private FixedText readFixedText(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "FixedText");

        String lfHeight = null;
        String lfWidth = null;
        String lfEscapement = null;
        String lfOrientation = null;
        String lfWeight = null;
        String lfItalic = null;
        String lfUnderline = null;
        String lfStrikeOut = null;
        String lfCharSet = null;
        String lfOutPrecision = null;
        String lfQuality = null;
        String lfPitchAndFamily = null;
        String lfFaceName = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readFixedText. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("lfHeight")) {
                lfHeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWidth")) {
                lfWidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfEscapement")) {
                lfEscapement = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOrientation")) {
                lfOrientation = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWeight")) {
                lfWeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfItalic")) {
                lfItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfUnderline")) {
                lfUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfStrikeOut")) {
                lfStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfCharSet")) {
                lfCharSet = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOutPrecision")) {
                lfOutPrecision = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfQuality")) {
                lfQuality = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfPitchAndFamily")) {
                lfPitchAndFamily = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfFaceName")) {
                lfFaceName = readText(parser);
            } else {
                Log.w(TAG, "readFixedText. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new FixedText(lfHeight, lfWidth, lfEscapement, lfOrientation, lfWeight, lfItalic, lfUnderline, lfStrikeOut, lfCharSet, lfOutPrecision, lfQuality, lfPitchAndFamily, lfFaceName);
    }

    private NongLi readNongLi(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "NongLi");

        String lfHeight = null;
        String lfWidth = null;
        String lfEscapement = null;
        String lfOrientation = null;
        String lfWeight = null;
        String lfItalic = null;
        String lfUnderline = null;
        String lfStrikeOut = null;
        String lfCharSet = null;
        String lfOutPrecision = null;
        String lfQuality = null;
        String lfPitchAndFamily = null;
        String lfFaceName = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readNongLi. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("lfHeight")) {
                lfHeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWidth")) {
                lfWidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfEscapement")) {
                lfEscapement = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOrientation")) {
                lfOrientation = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWeight")) {
                lfWeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfItalic")) {
                lfItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfUnderline")) {
                lfUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfStrikeOut")) {
                lfStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfCharSet")) {
                lfCharSet = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOutPrecision")) {
                lfOutPrecision = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfQuality")) {
                lfQuality = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfPitchAndFamily")) {
                lfPitchAndFamily = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfFaceName")) {
                lfFaceName = readText(parser);
            } else {
                Log.w(TAG, "readNongLi. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new NongLi(lfHeight, lfWidth, lfEscapement, lfOrientation, lfWeight, lfItalic, lfUnderline, lfStrikeOut, lfCharSet, lfOutPrecision, lfQuality, lfPitchAndFamily, lfFaceName);
    }

    private FixedDate readFixedDate(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Date");

        String lfHeight = null;
        String lfWidth = null;
        String lfEscapement = null;
        String lfOrientation = null;
        String lfWeight = null;
        String lfItalic = null;
        String lfUnderline = null;
        String lfStrikeOut = null;
        String lfCharSet = null;
        String lfOutPrecision = null;
        String lfQuality = null;
        String lfPitchAndFamily = null;
        String lfFaceName = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readFixedDate. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("lfHeight")) {
                lfHeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWidth")) {
                lfWidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfEscapement")) {
                lfEscapement = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOrientation")) {
                lfOrientation = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWeight")) {
                lfWeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfItalic")) {
                lfItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfUnderline")) {
                lfUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfStrikeOut")) {
                lfStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfCharSet")) {
                lfCharSet = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOutPrecision")) {
                lfOutPrecision = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfQuality")) {
                lfQuality = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfPitchAndFamily")) {
                lfPitchAndFamily = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfFaceName")) {
                lfFaceName = readText(parser);
            } else {
                Log.w(TAG, "readFixedDate. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new FixedDate(lfHeight, lfWidth, lfEscapement, lfOrientation, lfWeight, lfItalic, lfUnderline, lfStrikeOut, lfCharSet, lfOutPrecision, lfQuality, lfPitchAndFamily, lfFaceName);
    }

    private FixedWeek readFixedWeek(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Week");

        String lfHeight = null;
        String lfWidth = null;
        String lfEscapement = null;
        String lfOrientation = null;
        String lfWeight = null;
        String lfItalic = null;
        String lfUnderline = null;
        String lfStrikeOut = null;
        String lfCharSet = null;
        String lfOutPrecision = null;
        String lfQuality = null;
        String lfPitchAndFamily = null;
        String lfFaceName = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (DBG)
                Log.i(TAG, "readFixedWeek. [tagName=" + tagName);

            if (tagName.equalsIgnoreCase("lfHeight")) {
                lfHeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWidth")) {
                lfWidth = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfEscapement")) {
                lfEscapement = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOrientation")) {
                lfOrientation = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfWeight")) {
                lfWeight = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfItalic")) {
                lfItalic = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfUnderline")) {
                lfUnderline = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfStrikeOut")) {
                lfStrikeOut = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfCharSet")) {
                lfCharSet = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfOutPrecision")) {
                lfOutPrecision = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfQuality")) {
                lfQuality = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfPitchAndFamily")) {
                lfPitchAndFamily = readText(parser);
            } else if (tagName.equalsIgnoreCase("lfFaceName")) {
                lfFaceName = readText(parser);
            } else {
                Log.w(TAG, "readFixedWeek. [Skipping tag=" + tagName);
                skip(parser);
            }
        }
        return new FixedWeek(lfHeight, lfWidth, lfEscapement, lfOrientation, lfWeight, lfItalic, lfUnderline, lfCharSet, lfStrikeOut, lfOutPrecision, lfQuality, lfPitchAndFamily, lfFaceName);
    }

}