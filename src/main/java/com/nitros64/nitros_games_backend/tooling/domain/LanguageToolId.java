package com.nitros64.nitros_games_backend.tooling.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class LanguageToolId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long programmingLanguage;
    private Long programmingTool;
}
