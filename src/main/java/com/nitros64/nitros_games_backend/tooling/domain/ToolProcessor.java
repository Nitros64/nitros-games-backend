package com.nitros64.nitros_games_backend.tooling.domain;

import java.io.Serializable;

import com.nitros64.nitros_games_backend.catalog.domain.Processor;

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

@Entity
@IdClass(ToolProcessorId.class)
@Table(name = "tool_processor")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ToolProcessor implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_idtool")
    private ProgrammingTool programmingTool;
    
    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_idprocessor")
    private Processor processor;
}
