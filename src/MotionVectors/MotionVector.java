package de.webis.webarchive.MotionVectors;

import java.math.BigDecimal;
import java.math.MathContext;

import javafx.util.Pair;

/**
 * Class that represents a motion vector extracted from H.264-compressed video.
 * 
 * @author Lars Meyer
 */
public class MotionVector
{
  /*
   * Motion vectors are extracted from H.264-compressed frames, so they are
   * inherently final.
   */
  private final int src_x;
  private final int src_y;
  private final int dst_x;
  private final int dst_y;
  private final double length;
  private final Pair<Integer, Integer> v;
  
  private final MathContext mc = new MathContext(2);
  
  public MotionVector(int src_x, int src_y, int dst_x, int dst_y)
  {
    this.src_x = src_x;
    this.src_y = src_y;
    this.dst_x = dst_x;
    this.dst_y = dst_y;
        
    this.v = new Pair<Integer, Integer>((this.dst_x - this.src_x),
    									(this.dst_y - this.src_y));
    this.length = Math.sqrt(Math.pow(this.v.getKey(), 2)
                            + Math.pow(this.v.getValue(), 2));
  }
  
  /**
   * Function that returns a normalized vector representation.
   * 
   * @return the normalized vector, rounded to two decimal places
   */
  public Pair<BigDecimal, BigDecimal> normalize()
  {
	  return new Pair<BigDecimal, BigDecimal>
	  (
	      this.length > 0 ? new BigDecimal(this.v.getKey() / this.length, mc) : new BigDecimal(0),
	      this.length > 0 ? new BigDecimal(this.v.getValue() / this.length, mc) : new BigDecimal(0)
	  );
  }

  public double getLength()
  {
    return length;
  }

  public int getSrc_x()
  {
    return src_x;
  }

  public int getSrc_y()
  {
    return src_y;
  }

  public int getDst_x()
  {
    return dst_x;
  }

  public int getDst_y()
  {
    return dst_y;
  }
}
