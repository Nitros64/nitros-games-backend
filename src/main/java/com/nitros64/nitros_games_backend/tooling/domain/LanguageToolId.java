package com.nitros64.nitros_games_backend.tooling.domain;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;

public class LanguageToolId implements Serializable{
    private static final long serialVersionUID = -2834827403836993112L;
    
    //@Column(name = "program_lang_id")
    private Long program_language;
    
    //@Column(name = "program_tool_id")
    private Long program_tool;

    public LanguageToolId() {
    }

    public LanguageToolId(Long program_language, Long program_tool) {
        this.program_language = program_language;
        this.program_tool = program_tool;
    }

    public Long getProgram_language() {
        return program_language;
    }

    public void setProgram_language(Long program_language) {
        this.program_language = program_language;
    }

    public Long getProgram_tool() {
        return program_tool;
    }

    public void setProgram_tool(Long program_tool) {
        this.program_tool = program_tool;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.program_language);
        hash = 59 * hash + Objects.hashCode(this.program_tool);
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
        final LanguageToolId other = (LanguageToolId) obj;
        if (!Objects.equals(this.program_language, other.program_language)) {
            return false;
        }
        if (!Objects.equals(this.program_tool, other.program_tool)) {
            return false;
        }
        return true;
    }
}
