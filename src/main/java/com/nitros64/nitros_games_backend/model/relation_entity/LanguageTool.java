package com.nitros64.nitros_games_backend.model.relation_entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.nitros64.nitros_games_backend.model.entity.ProgrammingLanguage;
import com.nitros64.nitros_games_backend.model.entity.ProgrammingTool;

@Entity
@Table(name = "ToolLang")
@IdClass(LanguageToolId.class)
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class LanguageTool {

    @Id
    @ManyToOne(optional = false,
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "program_lang_id")
    private ProgrammingLanguage program_language;

    @Id
    @ManyToOne(optional = false,
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "program_tool_id")
    private ProgrammingTool program_tool;

}

//    public LanguageTool() {
//    }
//
//    public LanguageTool(ProgrammingLanguage program_language, ProgrammingTool program_tool) {
//        this.program_language = program_language;
//        this.program_tool = program_tool;
//    }
//
//    public ProgrammingLanguage getProgram_language() {
//        return program_language;
//    }
//
//    public void setProgram_language(ProgrammingLanguage program_language) {
//        this.program_language = program_language;
//    }
//
//    public ProgrammingTool getProgram_tool() {
//        return program_tool;
//    }
//
//    public void setProgram_tool(ProgrammingTool program_tool) {
//        this.program_tool = program_tool;
//    }