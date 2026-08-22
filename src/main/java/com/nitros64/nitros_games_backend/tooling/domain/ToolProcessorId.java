package com.nitros64.nitros_games_backend.tooling.domain;

import java.io.Serializable;
import java.util.Objects;

public class ToolProcessorId implements Serializable{
    
    private static final long serialVersionUID = -2834827499936993112L;
    
    private Long program_tool;
    private Long processor;

    public ToolProcessorId() {
    }

    public ToolProcessorId(Long program_tool, Long processor) {
        this.program_tool = program_tool;
        this.processor = processor;
    }

    public Long getProgram_tool() {
        return program_tool;
    }

    public void setProgram_tool(Long program_tool) {
        this.program_tool = program_tool;
    }

    public Long getProcessor() {
        return processor;
    }

    public void setProcessor(Long processor) {
        this.processor = processor;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.program_tool);
        hash = 79 * hash + Objects.hashCode(this.processor);
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
        final ToolProcessorId other = (ToolProcessorId) obj;
        if (!Objects.equals(this.program_tool, other.program_tool)) {
            return false;
        }
        if (!Objects.equals(this.processor, other.processor)) {
            return false;
        }
        return true;
    }
    
    
}
