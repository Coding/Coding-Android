package net.coding.program.model;

import android.text.Spannable;

import com.loopj.android.http.RequestParams;

import net.coding.program.common.CodingColor;
import net.coding.program.common.Global;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Vernon on 15/11/30.
 */
public class MergeObject implements Serializable {
    private static final String STYLE_ACCEPT = "ACCEPTED";
    private static final String STYLE_REFUSE = "REFUSED";
    private static final String STYLE_CANNEL = "CANCEL";
    private static final String STYLE_CANMERGE = "CANMERGE";
    private static final String STYLE_CANNOTMERGE = "CANNOTMERGE";
    public String merge_status = "";
    private int id;
    private String srcBranch = "";
    private String desBranch = "";
    private String title = "";
    private int author_id;
    private int iid;
    private int granted;
    private int granted_by_id;
    private String path = "";
    private long created_at;
    private UserObject author;
    private String body;
    private long action_at;
    private int commentCount;
    private String color;

    public MergeObject(JSONObject json) throws JSONException {
        id = json.optInt("id");
        srcBranch = json.optString("source_branch");
        desBranch = json.optString("target_branch");
        title = json.optString("title");
        iid = json.optInt("iid");
        author_id = json.optInt("author_id");
        JSONArray body = json.optJSONArray("body");
        if (body != null && body.length() > 0) {
            this.body = body.getString(0);
        } else {
            this.body = "";
        }
        merge_status = json.optString("merge_status");
        path = ProjectObject.translatePath(json.optString("path", ""));
        created_at = json.optLong("created_at");
        granted = json.optInt("granted");
        granted_by_id = json.optInt("granted_by_id");
        author = new UserObject(json.optJSONObject("author"));
        action_at = json.optLong("action_at");
        commentCount = json.optInt("comment_count", 0);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDesBranch() {
        return desBranch;
    }

    public void setDesBranch(String desBranch) {
        this.desBranch = desBranch;
    }

    public String getSrcBranch() {
        return srcBranch;
    }

    public void setSrcBranch(String srcBranch) {
        this.srcBranch = srcBranch;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    public boolean isMergeAccept() {
        return merge_status.equals(STYLE_ACCEPT);
    }

    public boolean isStyleCannotMerge() {
        return merge_status.equals(STYLE_CANNOTMERGE);
    }

    public boolean isStyleCanMerge() {
        return merge_status.equals(STYLE_CANMERGE);
    }

    public boolean isMergeRefuse() {
        return merge_status.equals(STYLE_REFUSE);
    }

    public boolean isMergeTreate() {
        return merge_status.equals(STYLE_ACCEPT) ||
                merge_status.equals(STYLE_REFUSE);
    }

    public boolean isMergeCannel() {
        return merge_status.equals(STYLE_CANNEL);
    }

    public int getGranted() {
        return granted;
    }

    public void setGranted(int granted) {
        this.granted = granted;
    }

    public int getGranted_by_id() {
        return granted_by_id;
    }

    public void setGranted_by_id(int granted_by_id) {
        this.granted_by_id = granted_by_id;
    }

    public int getAuthor_id() {
        return author_id;
    }

    public void setAuthor_id(int author_id) {
        this.author_id = author_id;
    }

    public boolean authorIsMe() {
        return author.isMe();
    }

    public UserObject getAuthor() {
        return author;
    }

    public void setAuthor(UserObject author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIid() {
        return iid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public String getColor() {
        return color;
    }

    public String getMergeStatus() {
        switch (merge_status) {
            case STYLE_ACCEPT:
                color = "#EB7A19";
                return "已合并";
            case STYLE_REFUSE:
                color = "#E84D60";
                return "已拒绝";
            case STYLE_CANMERGE:
                color = CodingColor.fontGreenString;
                return "可合并";
            case STYLE_CANNOTMERGE:
                color = "#B17EDD";
                return "不可自动合并";
            case STYLE_CANNEL:
                color = CodingColor.fon3String;
                return "已取消";
            default:
                color = CodingColor.fon3String;
                return "已取消";
        }
    }

    public long getCreatedAt() {
        return created_at;
    }

    public long getAction_at() {
        return action_at;
    }

    public void setAction_at(long action_at) {
        this.action_at = action_at;
    }

    private String getHostPublicHead(String end) {
        return Global.HOST_API + path + end;
    }

    public String getProjectPath() {
        int index = path.indexOf("/git/");
        if (index != -1) {
            return path.substring(0, index);
        }

        return path;
    }

    public String getHttpComments() {
        return getHostPublicHead("/comments");
    }

    public String getHttpDetail() {
        return getHostPublicHead("/base");
    }


    public String getHttpCommits() {
        return getHostPublicHead("/commits");
    }

    public RequestData getHttpMerge(String message, boolean delSource) {
        String url = getHostPublicHead("/merge");

        RequestParams params = new RequestParams();
        params.put("del_source_branch", delSource);
        params.put("message", message);

        return new RequestData(url, params);
    }

    private String getHttpHostComment() {
        String gitHead = getHostPublicHead("");
        int index = gitHead.indexOf("/git/");
        String head = gitHead.substring(0, index);
        return head + "/git/line_notes";
    }

    public Spannable getTitleSpannable() {
        return Global.changeHyperlinkColor(title);
    }


    public String getTitleIId() {
        return "# " + iid;
    }
}

