package com.nitros64.nitros_games_backend.tooling.domain;

import java.io.Serializable;

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
@Table(name = "tool_lang")
@IdClass(LanguageToolId.class)
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class LanguageTool implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "program_lang_id")
    private ProgrammingLanguage programmingLanguage;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "program_tool_id")
    private ProgrammingTool programmingTool;
}
