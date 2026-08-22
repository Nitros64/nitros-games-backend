package com.nitros64.nitros_games_backend.model.relation_entity;

import java.io.Serializable;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.model.entity.ProgrammingTool;

import jakarta.persistence.CascadeType;
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
@Table(name = "ToolPlatform")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ToolPlatform implements Serializable {
    @Id
    @ManyToOne(optional = false, 
               cascade=CascadeType.ALL, 
               fetch=FetchType.EAGER)    
    @JoinColumn(name = "fk_idtool")    
    private ProgrammingTool program_tool;
    
    @Id
    @ManyToOne(optional = false, 
               cascade=CascadeType.ALL, 
               fetch=FetchType.EAGER)
    @JoinColumn(name = "fk_idplatform")
    private Platform platform;

//    public ToolPlatform() {
//    }
//
//    public ToolPlatform(ProgrammingTool program_tool, Platform platform) {
//        this.program_tool = program_tool;
//        this.platform = platform;
//    }
//
//    public ProgrammingTool getProgram_tool() {
//        return program_tool;
//    }
//
//    public void setProgram_tool(ProgrammingTool program_tool) {
//        this.program_tool = program_tool;
//    }
//
//    public Platform getPlatform() {
//        return platform;
//    }
//
//    public void setPlatform(Platform platform) {
//        this.platform = platform;
//    }
}
