package com.example.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/file")
public class ImageUpload {


  Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
      "cloud_name", "prkd2015",
      "api_key", "618491394938988",
      "api_secret", "rF6BHgQy13pU4qTxMOlooc8gjC0"));

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
    Map uploadResult = writeToFile(uploadedInputStream, uploadedFileLocation);
      String url = (String) uploadResult.get("url");
    String output = "File uploaded to : " + url;


//    File toUpload = new File("daisy.png");
//    Map uploadResult = cloudinary.uploader().upload(toUpload);

    return Response.status(200).entity(output).build();

  }


  private Connection getConnection() throws URISyntaxException, SQLException {
    URI dbUri = new URI(System.getenv("DATABASE_URL"));

    String username = dbUri.getUserInfo().split(":")[0];
    String password = dbUri.getUserInfo().split(":")[1];
    int port = dbUri.getPort();

    String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + dbUri.getPath() + "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

    return DriverManager.getConnection(dbUrl, username, password);
  }


  @GET
  @Path("/db")
  public Response showDatabase()
      throws ServletException, IOException {
    Connection connection = null;
    try {
      connection = getConnection();

      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      String out = "Hello!\n";
      while (rs.next()) {
        out += "Read from DB: " + rs.getTimestamp("tick") + "\n";
      }

      return Response.status(200).entity(out).build();
    } catch (Exception e) {
      return Response.status(200).entity("There was an error: " + e.getMessage()).build();
    } finally {
      if (connection != null) try{connection.close();} catch(SQLException e){}
    }
  }

  // save uploaded file to new location
  private Map writeToFile(InputStream uploadedInputStream,
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
      Map uploadResult = cloudinary.uploader().upload(file,  ObjectUtils.emptyMap());
      return uploadResult;
//      String publicId = (String) uploadResult.get("public_id");

    } catch (IOException e) {

      e.printStackTrace();
    }

    return null;
  }

}