package com.nitros64.nitros_games_backend.storage.api;

import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;
import com.nitros64.nitros_games_backend.shared.api.error.ApiProblem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.constrait.NoNumberString;
import com.nitros64.nitros_games_backend.storage.application.FileHostImageHandler;
import com.nitros64.nitros_games_backend.storage.application.ServerHostImageService;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*********************************************************************
 *                      SERVERHOST IMAGE CONTROLLER                  *
 ********************************************************************/

@Validated
@RestController
@RequestMapping(path = "api/v1/serverhostimage")
public class ServerHostImageController extends BaseControllerImpl<ServerHostImage, ServerHostImageService>{

     @Autowired
     private FileHostImageHandler filehosthandler;

     @Override
     @PostMapping("add") //@Valid
     public ResponseEntity<?> save(@RequestBody ServerHostImage entity) {
        var problem = ApiProblem.create(
                HttpStatus.FORBIDDEN,
                "Operation not allowed",
                "operation_not_allowed",
                "Host images must be created through the upload endpoint",
                "/api/v1/serverhostimage/add");
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
     }

     //@Secured({"ROLE_ADMIN","ROLE_USER"})//SE DEBE PONER EL PREFIJO "ROLE_"
     @PostMapping("upload_image")
     public ResponseEntity<?> upload(@RequestParam("fileHostImage") MultipartFile file, @NoNumberString @NotBlank
                                     @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
                                     @RequestParam("name") String host_image_name) {

         if (servicio.findByName(host_image_name).isPresent()) {
             var problem = ApiProblem.create(
                     HttpStatus.CONFLICT,
                     "Data conflict",
                     "data_conflict",
                     "The request conflicts with existing data",
                     "/api/v1/serverhostimage/upload_image");
             return ResponseEntity.status(problem.getStatus())
                     .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                     .body(problem);
         }

         String newfileName = filehosthandler.manage(file);
         ServerHostImage response = servicio.save(new ServerHostImage(host_image_name,newfileName));
         return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

     @PutMapping(path = "upload_image/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE }, produces = {MediaType.APPLICATION_JSON_VALUE })
     public ResponseEntity<?> updateImage (@RequestParam(name = "fileHostImage", required = false) MultipartFile file, @PathVariable Long id,
                                           @NoNumberString(message = "Lalo") @NotBlank @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
                                           @RequestParam("name") String host_image_name) {
         var problem = ApiProblem.create(
                 HttpStatus.NOT_ACCEPTABLE,
                 "Update rejected",
                 "update_rejected",
                 "The host image cannot be updated through this operation",
                 "/api/v1/serverhostimage/upload_image/" + id);
         return ResponseEntity.status(problem.getStatus())
                 .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                 .body(problem);
     }

     @PutMapping(path = "update_name/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }, produces = {MediaType.APPLICATION_JSON_VALUE })
     public ResponseEntity<?> updateName (@PathVariable Long id, @NoNumberString @NotBlank
                                          @Size(min = 4, max = 30, message="el tamaño tiene que estar entre 4 y 30")
                                          @RequestParam("name") String host_image_name) {

         var entity = this.servicio.findById(id);

         if(entity.getName().equals(host_image_name)){
             entity.setName(host_image_name);
             return ResponseEntity.status(HttpStatus.ACCEPTED).body(servicio.save(entity));
         }
         var problem = ApiProblem.create(
                 HttpStatus.IM_USED,
                 "Update rejected",
                 "update_rejected",
                 "The host image name could not be updated",
                 "/api/v1/serverhostimage/update_name/" + id);
         return ResponseEntity.status(problem.getStatus())
                 .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                 .body(problem);
     }
}
