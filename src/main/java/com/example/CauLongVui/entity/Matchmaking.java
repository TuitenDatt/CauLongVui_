package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "matchmakings")
public class Matchmaking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // The creator of this matchmaking
    private String authorName; // Name of creator
    private String title; // E.g., Tìm 2 bạn nam nam đánh khá
    private String phoneContact; // SĐT Zalo để liên lạc
    private String courtName; // Sân nào

    private LocalDate playDate; // Ngày đánh
    private String startTime; // Giờ bắt đầu
    private String endTime; // Giờ kết thúc
    private String level; // Trình độ yêu cầu (TB, Khá, Giỏi...)
    private Double costPerPerson; // Chi phí dự kiến

    private Integer requiredPlayers; // Số người cần tuyển
    private Integer joinedPlayers; // Số người đã tham gia

    private String status; // OPEN, FULL, CANCELLED

    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "OPEN";
        if (joinedPlayers == null) joinedPlayers = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPhoneContact() { return phoneContact; }
    public void setPhoneContact(String phoneContact) { this.phoneContact = phoneContact; }
    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }
    public LocalDate getPlayDate() { return playDate; }
    public void setPlayDate(LocalDate playDate) { this.playDate = playDate; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Double getCostPerPerson() { return costPerPerson; }
    public void setCostPerPerson(Double costPerPerson) { this.costPerPerson = costPerPerson; }
    public Integer getRequiredPlayers() { return requiredPlayers; }
    public void setRequiredPlayers(Integer requiredPlayers) { this.requiredPlayers = requiredPlayers; }
    public Integer getJoinedPlayers() { return joinedPlayers; }
    public void setJoinedPlayers(Integer joinedPlayers) { this.joinedPlayers = joinedPlayers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
