package com.bidding.model;

public class SystemNotification {
    private String time;
    private String type;
    private String content;

    public SystemNotification(String time, String type, String content) {
        this.time = time;
        this.type = type;
        this.content = content;
    }

    // Các hàm Getter bắt buộc phải có để PropertyValueFactory hoạt động
    public String getTime() { return time; }
    public String getType() { return type; }
    public String getContent() { return content; }

    public void setTime(String time) { this.time = time; }
    public void setType(String type) { this.type = type; }
    public void setContent(String content) { this.content = content; }
}