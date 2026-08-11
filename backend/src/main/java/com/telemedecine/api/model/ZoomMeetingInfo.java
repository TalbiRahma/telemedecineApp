package com.telemedecine.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "zoom_meeting")
public class ZoomMeetingInfo {
    @Id
    private String id;
    private String joinUrl;
    private String startUrl;
}
