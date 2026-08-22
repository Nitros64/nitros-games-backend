package com.nitros64.nitros_games_backend.tooling.domain;

import com.nitros64.nitros_games_backend.catalog.domain.Processor;

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
@IdClass(ToolProcessorId.class)
@Table(name = "ToolProcessor")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ToolProcessor {
    
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
    @JoinColumn(name = "fk_idprocessor")
    private Processor processor;

//    public ToolProcessor() {
//    }

//    public ToolProcessor(ProgrammingTool lt, Processor processor) {
//        this.program_tool = lt;
//        this.processor = processor;
//    }

//    public ProgrammingTool getProgram_tool() {
//        return program_tool;
//    }
//
//    public void setProgram_tool(ProgrammingTool program_tool) {
//        this.program_tool = program_tool;
//    }
//
//    public Processor getProcessor() {
//        return processor;
//    }
//
//    public void setProcessor(Processor processor) {
//        this.processor = processor;
//    }
    
}
