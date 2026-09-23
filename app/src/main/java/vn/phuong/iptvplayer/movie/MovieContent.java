package vn.phuong.iptvplayer.movie;
import java.util.ArrayList; import java.util.List;
public final class MovieContent {
 private MovieContent(){}
 public static final class Item { public String id="",title="",poster="",backdrop="",overview="",year="",type="movie",rating=""; public final List<String> genres=new ArrayList<>(); }
 public static final class Section { public String title=""; public final List<Item> items=new ArrayList<>(); }
 public static final class Detail { public Item item; public String director=""; public final List<Season> seasons=new ArrayList<>(); }
 public static final class Season { public String id="",name=""; public int number; public final List<Episode> episodes=new ArrayList<>(); }
 public static final class Episode { public String id="",name=""; public int number; }
 public static final class Source { public String url="",mime="",quality="",referer="",origin="https://novahd.cc"; public final List<Subtitle> subtitles=new ArrayList<>(); }
 public static final class Subtitle { public String url="",language="",label=""; }
}