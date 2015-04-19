package com.example.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/file")
public class ImageUpload {

  @POST
  @Path("/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadFile(
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("lat") String Lat,
      @FormDataParam("lon") String Lon) {

    String uploadedFileLocation = "./Asset/"+Lat+"/"+Lon+"/" + fileDetail.getFileName();

    // save it
    writeToFile(uploadedInputStream, uploadedFileLocation);

    String output = "File uploaded to : " + uploadedFileLocation;

    return Response.status(200).entity(output).build();

  }

  // save uploaded file to new location
  private void writeToFile(InputStream uploadedInputStream,
                           String uploadedFileLocation) {

    try {

//      OutputStream out = new FileOutputStream(new File(
//          uploadedFileLocation));
      int read = 0;
      byte[] bytes = new byte[1024];


      File file = new File(uploadedFileLocation);
      file.getParentFile().mkdirs();
      file.createNewFile();
      OutputStream out = new FileOutputStream(file);
      while ((read = uploadedInputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }
      out.flush();
      out.close();
    } catch (IOException e) {

      e.printStackTrace();
    }

  }

}