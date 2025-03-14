package com.nitros64.nitros_games_backend.model.relation_entity;

import java.io.Serializable;
import java.util.Objects;

public class ToolPlatformId implements Serializable{
    private static final long serialVersionUID = -2834827499936993112L;
    
    private Long program_tool;
    private Long platform;

    public ToolPlatformId(Long program_tool, Long platform) {
        this.program_tool = program_tool;
        this.platform = platform;
    }

    public ToolPlatformId() {
    }

    public Long getProgram_tool() {
        return program_tool;
    }

    public void setProgram_tool(Long program_tool) {
        this.program_tool = program_tool;
    }

    public Long getPlatform() {
        return platform;
    }

    public void setPlatform(Long platform) {
        this.platform = platform;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.program_tool);
        hash = 29 * hash + Objects.hashCode(this.platform);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ToolPlatformId other = (ToolPlatformId) obj;
        if (!Objects.equals(this.program_tool, other.program_tool)) {
            return false;
        }
        if (!Objects.equals(this.platform, other.platform)) {
            return false;
        }
        return true;
    }
}
