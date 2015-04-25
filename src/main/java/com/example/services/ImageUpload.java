package com.example.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
      @FormDataParam("lon") String Lon,
      @FormDataParam("time") String time) {

    String uploadedFileLocation = "./Asset/"+Lat+"/"+Lon+"/" + fileDetail.getFileName();

    // save it
    Map uploadResult = writeToFile(uploadedInputStream, uploadedFileLocation);
      String url = (String) uploadResult.get("url");
    String output = "File uploaded to : " + url;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    java.util.Date parsedDate = null;
    Timestamp timestamp;
    try {
      parsedDate = dateFormat.parse(time);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    timestamp= new java.sql.Timestamp(parsedDate.getTime());
    return store_in_db(url, Float.parseFloat(Lat), Float.parseFloat(Lon), timestamp);
  }

  private Response store_in_db(String url, float lat, float lon, Timestamp time) {
    Connection connection = null;
    try {
      connection = getConnection();

      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS file_urls (filename_url text," +
          "lat float," +
          "lon float," +
          "time timestamp)");
      stmt.executeUpdate("INSERT INTO file_urls VALUES (\'" + url + "\'," +
          "\'" + lat + "\'," +
          "\'" + lon + "\'," +
          "\'" + time + "\'," +
          ")");
      String output = "File uploaded to : " + url;

      return Response.status(200).entity(output).build();
    } catch (Exception e) {
      return Response.status(200).entity("There was an error: " + e.getMessage()).build();
    } finally {
      if (connection != null) try{connection.close();} catch(SQLException e){}
    }
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
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS file_urls (filename_url text)");
      ResultSet rs = stmt.executeQuery("SELECT filename_url FROM file_urls");

      String out = "Hello!\n";
      while (rs.next()) {
        out += "Read from DB: " + rs.getString("filename_url") + "\n";
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