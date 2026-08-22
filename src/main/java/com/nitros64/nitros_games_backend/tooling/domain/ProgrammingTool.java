package com.nitros64.nitros_games_backend.tooling.domain;

import com.nitros64.nitros_games_backend.shared.domain.Base;

import java.net.URL;

import jakarta.persistence.*;

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

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private URL webPage; //Debe ser la URL del sitio web de la herramienta usada

    @Column(nullable = false, unique = true)
    private String imagefilePath; //Nota Hay que generar una etiqueta personalizada al recibir los archivos de imagen

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
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
