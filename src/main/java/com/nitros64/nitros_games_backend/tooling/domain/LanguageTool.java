package com.nitros64.nitros_games_backend.tooling.domain;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


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
