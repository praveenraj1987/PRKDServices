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

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

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
    }
    catch (ParseException e) {
      java.util.Date d = new java.util.Date();
      try {
        parsedDate = dateFormat.parse(new Timestamp(d.getTime()).toString());
      } catch (ParseException e1) {
        e1.printStackTrace();
      }
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
          "\'" + time + "\'" +
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


  public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
    double earthRadius = 6371000; //meters
    double dLat = Math.toRadians(lat2-lat1);
    double dLng = Math.toRadians(lng2-lng1);
    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng/2) * Math.sin(dLng/2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    float dist = (float) (earthRadius * c);

    return dist;
  }


  @POST
  @Path("/nearby")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response showNearBy(
      @FormDataParam("lat") String userLat,
      @FormDataParam("lon") String userLon
  ) throws IOException{
//    Connection connection = null;
//    try {
//      connection = getConnection();
//
//      Statement stmt = connection.createStatement();
//      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS file_urls (filename_url text)");
//      ResultSet rs = stmt.executeQuery("SELECT * FROM file_urls");
//
//      String out = "";
//      while (rs.next()) {
//        float lat = rs.getFloat("lat");
//        float lon = rs.getFloat("lon");
//        if(distFrom(Float.parseFloat(userLat), Float.parseFloat(userLon), lat, lon) < 1000) {
//          out += "Start of Record -------: <br>" + "File URL:->" + rs.getString("filename_url") + "<br>" +
//              "File Latitude:->" + lat + "<br>" +
//              "File Longitude:->" + lon + "<br>" +
//              "File TimeStamp:->" + rs.getTimestamp("time") + "<br><br>";
//        }
//      }
//
//      return Response.status(200).entity(out).build();
//    } catch (Exception e) {
    return Response.status(200).entity("There was an error: " + userLat + userLon).build();
//    } finally {
//      if (connection != null) try{connection.close();} catch(SQLException e){}
//    }
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
      ResultSet rs = stmt.executeQuery("SELECT * FROM file_urls");

      String out = "";
      while (rs.next()) {
        out += "Start of Record -------: <br>" + "File URL:->" + rs.getString("filename_url") + "<br>" +
            "File Latitude:->" + rs.getFloat("lat") + "<br>" +
            "File Longitude:->" + rs.getFloat("lon") + "<br>"+
            "File TimeStamp:->" + rs.getTimestamp("time") + "<br><br>";
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