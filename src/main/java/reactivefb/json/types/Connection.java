package reactivefb.json.types;

import com.restfb.util.UrlUtils;

import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Represents a <a href="http://developers.facebook.com/docs/api">Graph API Connection type</a>.
 *
 * @param <T> The Facebook type
 * @author Sergii Karpenko
 */
public class Connection<T>  {

  public static final String HTTP_PREFIX = "http://";
  public static final String HTTPS_PREFIX = "https://";
  public static final String AFTER_PARAMETER = "after";
  public static final String BEFORE_PARAMETER = "before";
  private List<T> data;

  private Paging paging;

  private transient String url;

  /**
   * Data for this connection.
   * 
   * @return Data for this connection.
   */
  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
  }

  public Paging getPaging() {
    return paging;
  }

  public void setPaging(Paging paging) {
    this.paging = paging;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getNextPageUrl(){
    String pagingNext = paging.getNext();
    if(pagingNext != null){
      return pagingNext;
    }

    String afterCursor = ofNullable(paging.getCursors()).map(Cursors::getAfter).orElse(null);
    if(afterCursor != null){
      return UrlUtils.replaceOrAddQueryParameter(url, AFTER_PARAMETER, afterCursor);
    }

    return null;
  }

  public String getPreviousPageUrl(){
    String pagingPrevious = paging.getPrevious();
    if(pagingPrevious != null){
      return pagingPrevious;
    }

    String beforeCursor = ofNullable(paging.getCursors()).map(Cursors::getBefore).orElse(null);
    if(beforeCursor != null){
      return UrlUtils.replaceOrAddQueryParameter(url, BEFORE_PARAMETER, beforeCursor);
    }

    return null;
  }

  public static class Paging {

    private Cursors cursors;
    private String previous;
    private String next;

    public String getPrevious() {
      return fixProtocol(previous);
    }

    public void setPrevious(String previous) {
      this.previous = previous;
    }

    public String getNext() {
      return fixProtocol(next);
    }

    public void setNext(String next) {
      this.next = next;
    }

    public Cursors getCursors() {
      return cursors;
    }

    public void setCursors(Cursors cursors) {
      this.cursors = cursors;
    }

    private static String fixProtocol(String pageUrl) {
      if (null != pageUrl && pageUrl.startsWith(HTTP_PREFIX)) {
        return pageUrl.replaceFirst(HTTP_PREFIX, HTTPS_PREFIX);
      } else {
        return pageUrl;
      }
    }
  }

  public static class Cursors {
    private String before;
    private String after;

    public String getBefore() {
      return before;
    }

    public void setBefore(String before) {
      this.before = before;
    }

    public String getAfter() {
      return after;
    }

    public void setAfter(String after) {
      this.after = after;
    }

  }

}