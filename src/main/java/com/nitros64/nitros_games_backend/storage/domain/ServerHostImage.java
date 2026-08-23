package com.nitros64.nitros_games_backend.storage.domain;

import com.nitros64.nitros_games_backend.shared.domain.Base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_hostimage")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ServerHostImage extends Base{
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, unique = true)
    private String name;   //MEGA POR EJEMPLO

    @Column(nullable = false, unique = true)
    private String imagepath;

    public ServerHostImage(String name){
        this.name = name;
    }
}

/*
    CONTROLLER READY
    SERVICE READY
    REPOSITORY READY
 */

/*
ImagePath es la imagen del servidor usado para el link asociado
    ****Todo link debe tener un host privado donde se almacena el ejecutable del juego,
    
    ****Los server privados como MEGA, MediaFire o Dropbox tienen su propio logotipo 
        este logotipo se usa para diferenciar y seleccionar entre los diferentes Links de descarga

    ****Cada Juego tiene diferentes version y cada version tiene diferentes opciones para descargar,
        cada opcion es un servidor privado diferente, como los mensinados anteriormente
*/
