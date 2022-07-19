package de.webis.webarchive.MotionVectors;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MotionVectorParser {
  ProcessBuilder ffmpeg_extract_mvs;
  boolean is_mp4; // filename could either be mp4 or csv
  String filename;

  public MotionVectorParser(String filename)
  {
    this.filename = filename;
    this.is_mp4 = filename.endsWith("mp4") ? true : false;
    this.ffmpeg_extract_mvs = new ProcessBuilder("./extract_mvs", filename);

  }

  public List<MotionVector> parse() throws IOException
  {
    List<MotionVector> l = new ArrayList<MotionVector>();
    String line;

    //if an mp4 file was used to store the motion vectors
    if(is_mp4) {
      Process ffmpeg = this.ffmpeg_extract_mvs.start();
      InputStream ffmpeg_is = ffmpeg.getInputStream();
      InputStream ffmpeg_es = ffmpeg.getErrorStream();
      InputStreamReader ffmpeg_isr = new InputStreamReader(ffmpeg_is);
      InputStreamReader ffmpeg_esr = new InputStreamReader(ffmpeg_es);
      BufferedReader ffmpeg_ibr = new BufferedReader(ffmpeg_isr);
      BufferedReader ffmpeg_ebr = new BufferedReader(ffmpeg_esr);

      while ((line = ffmpeg_ibr.readLine()) != null) {
        addMotionVectors(line, l);
      }
      while ((line = ffmpeg_ebr.readLine()) != null) {
        System.err.println(line);
      }
    }
    // if the motion vectors come in csv format
    else
    {
      BufferedReader csvReader = new BufferedReader(new FileReader(filename));
      while ((line = csvReader.readLine()) != null)
      {
        addMotionVectors(line, l);
      }
      csvReader.close();

    }

    return l;
  }

  public void addMotionVectors(String line, List<MotionVector> l)
  {
    int src_x, src_y, dst_x, dst_y;
    String[] coordsAsStrings = line.split(",");
    src_x = Integer.parseInt(coordsAsStrings[0]);
    src_y = Integer.parseInt(coordsAsStrings[1]);
    dst_x = Integer.parseInt(coordsAsStrings[2]);
    dst_y = Integer.parseInt(coordsAsStrings[3]);

    l.add(new MotionVector(src_x, src_y, dst_x, dst_y));
  }
}
