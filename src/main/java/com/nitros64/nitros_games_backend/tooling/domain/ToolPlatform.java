package com.nitros64.nitros_games_backend.tooling.domain;

import java.io.Serializable;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@IdClass(ToolPlatformId.class)
@Table(name = "tool_platform")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ToolPlatform implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_idtool")
    private ProgrammingTool programmingTool;
    
    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_idplatform")
    private Platform platform;
}
