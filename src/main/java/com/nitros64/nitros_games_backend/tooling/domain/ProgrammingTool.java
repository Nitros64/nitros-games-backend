package com.nitros64.nitros_games_backend.tooling.domain;

import com.nitros64.nitros_games_backend.shared.domain.Base;

import java.net.URL;

import com.nitros64.nitros_games_backend.constrait.NoNumberString;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "program_tool")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
//@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.ANY, setterVisibility = Visibility.ANY)
public class ProgrammingTool extends Base {

    private static final long serialVersionUID = 1L;

    //@Pattern(regexp="^(0|[1-9][0-9]*)$", message = "El nombre no puede contener Numeros")
    @NoNumberString
    @NotEmpty(message = "no puede estar vacio")
    @Size(min = 4, max = 30, message = "el tamaño tiene que estar entre 4 y 30")
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull(message = "webPage cannot be null")
    @Column(nullable = false, unique = true)
    private URL webPage; //Debe ser la URL del sitio web de la herramienta usada

    @NotEmpty(message = "no puede estar vacio")
    @Size(min = 4, max = 30, message = "el tamaño tiene que estar entre 4 y 30")
    @Column(nullable = false, unique = true)
    private String imagefilePath; //Nota Hay que generar una etiqueta personalizada al recibir los archivos de imagen

    @NotNull(message = "ProgramToolType cannot be null")
    @ManyToOne(optional = false,
            cascade= CascadeType.ALL,
            fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_gametooltype")
    private ProgramToolType toolType;
}

/*
    CONTROLLER READY
    SERVICE READY
    REPOSITORY READY
 */


//    public ProgrammingTool() { }

//    public ProgrammingTool(String name) {
//        this.name = name;
//    }
//    
//    public ProgrammingTool(String name, ProgramToolType toolType) {
//        this.name = name;
//        this.toolType = toolType;
//    }
//    
//    public ProgrammingTool(String name, URL webPage, String imagefilePath, ProgramToolType toolType) {
//        this.name = name;
//        this.webPage = webPage;
//        this.imagefilePath = imagefilePath;
//        this.toolType = toolType;
//    }
//
//    public ProgrammingTool(Long id, String name) {
//        this.id = id;
//        this.name = name;
//    }    
//}
