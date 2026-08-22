package com.nitros64.nitros_games_backend.controllers;

import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;

import jdk.jfr.ContentType;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.constrait.NoNumberString;
import com.nitros64.nitros_games_backend.filehandling.FileHostImageHandler;
import com.nitros64.nitros_games_backend.model.entity.ServerHostImage;
import com.nitros64.nitros_games_backend.service.impl.ServerHostImageService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.SQLException;
import java.util.Arrays;

/*********************************************************************
 *                      SERVERHOST IMAGE CONTROLLER                  *
 ********************************************************************/

@Validated
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/serverhostimage")
public class ServerHostImageController extends BaseControllerImpl<ServerHostImage, ServerHostImageService>{

     @Autowired
     private FileHostImageHandler filehosthandler;

     @Override
     @PostMapping("add") //@Valid
     public ResponseEntity<?> save(@RequestBody ServerHostImage entity) throws Exception {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
     }

     //@Secured({"ROLE_ADMIN","ROLE_USER"})//SE DEBE PONER EL PREFIJO "ROLE_"
     @PostMapping("upload_image")
     public ResponseEntity<?> upload(@RequestParam("fileHostImage") MultipartFile file, @NoNumberString @NotBlank
                                     @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
                                     @RequestParam("name") String host_image_name) throws Exception{

         if( servicio.findByName(host_image_name).isPresent() )
             throw new ConstraintViolationException("An error appear!!", new SQLException("Duplicate value " + host_image_name + " in Data Base"), null);

         String newfileName = filehosthandler.manage(file);
         ServerHostImage response = servicio.save(new ServerHostImage(host_image_name,newfileName));
         return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

     @PutMapping(path = "upload_image/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE }, produces = {MediaType.APPLICATION_JSON_VALUE })
     public ResponseEntity<?> updateImage (@RequestParam(name = "fileHostImage", required = false) MultipartFile file, @PathVariable Long id,
                                           @NoNumberString(message = "Lalo") @NotBlank @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
                                           @RequestParam("name") String host_image_name) throws Exception { //

         System.out.println("\n" + file.getContentType());
         System.out.println(MediaType.IMAGE_PNG_VALUE);

         if(!Arrays.asList(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE).contains(file.getContentType())) {
             throw new IllegalStateException("File must be an Image");
         }

//         boolean change = false;
//         var entity = this.servicio.findById(id);
//         if(file != null && !file.isEmpty()){
//             String old_imagepath = entity.getImagepath();
//             if(filehosthandler.delete(old_imagepath)){
//                 entity.setImagepath( filehosthandler.manage(file) );
//                 change = true;
//             }
//         }
//
//         if(!host_image_name.equals(entity.getName())){
//             entity.setName(host_image_name);
//             change = true;
//         }
//
//         if(change){
//             try {
//                 return ResponseEntity.status(HttpStatus.ACCEPTED).body(servicio.update(id,entity));
//             } catch (Exception e) {
//                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\": \"Error. Por favor intente mas tarde.\"}");
//             }
//         }
         return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("{ \"error\": \"Error. Cannot Update.\"}");
     }

     @PutMapping(path = "update_name/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }, produces = {MediaType.APPLICATION_JSON_VALUE })
     public ResponseEntity<?> updateName (@PathVariable Long id, @NoNumberString @NotBlank
                                          @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
                                          @RequestParam("name") String host_image_name) throws Exception{

         var entity = this.servicio.findById(id);

         if(entity.getName().equals(host_image_name)){
             entity.setName(host_image_name);
             return ResponseEntity.status(HttpStatus.ACCEPTED).body(servicio.save(entity));
         }
         return ResponseEntity.status(HttpStatus.IM_USED).body("{ \"error\": \"Error. Por favor intente mas tarde.\"}");
     }
}
