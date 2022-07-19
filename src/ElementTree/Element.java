package de.webis.webarchive.ElementTree;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import de.webis.webarchive.MotionVectors.MotionVector;

import javafx.util.Pair;

public class Element implements Comparable<Element> {
  private String name;
  private int index;
  private String content;

  private Pair<Integer, Integer> topLeft;
  private Pair<Integer, Integer> bottomRight;
  private BufferedImage image = null;
  private List<MotionVector> mvs = null;
  private int x_shift = 0;
  private int y_shift = 0;
  private int depth=-1;
  
  public Element(String name, String content, String coordsTopLeft,
                 String coordsBottomRight)
  {
    this.name = name;
    if (name.length() < 2) {
      throw new IllegalArgumentException("Invalid element name");
    }
    // This is going to be a problem at some point
    // There can be >9 child elements, then what?
    this.index = Character.getNumericValue(
                            this.name.charAt(this.name.length() - 2));
    // this.index = index;
    this.content = content;
    
    String[] coordsTopLeftXY = coordsTopLeft.split(",", 2);
    String[] coordsBottomRightXY = coordsBottomRight.split(",", 2);
    
    if (coordsTopLeftXY.length != 2) {
      throw new IllegalArgumentException("Incomplete top left coordinates");
    }
    if (coordsBottomRightXY.length != 2) {
      throw new IllegalArgumentException(
                                      "Incomplete bottom right coordinates");
    }
    
    this.topLeft = new Pair<Integer, Integer>(
                        Integer.parseInt(coordsTopLeftXY[0]),
                        Integer.parseInt(coordsTopLeftXY[1]));
    this.bottomRight = new Pair<Integer, Integer>(
                            Integer.parseInt(coordsBottomRightXY[0]),
                            Integer.parseInt(coordsBottomRightXY[1]));
  }
  
  public List<MotionVector> getMvs()
  {
    return mvs;
  }

  public void setMvs(List<MotionVector> mvs)
  {
    this.mvs = mvs;
  }

  public int getX_shift()
  {
    return x_shift;
  }

  public void setX_shift(int x_shift)
  {
    this.x_shift = x_shift;
  }

  public int getY_shift()
  {
    return y_shift;
  }

  public void setY_shift(int y_shift)
  {
    this.y_shift = y_shift;
  }

  public boolean isShifted()
  {
    return this.x_shift != 0 || this.y_shift != 0;
  }
  public boolean contains(int x, int y)
  {
    if (x >= topLeft.getKey() && x < bottomRight.getKey()
        && y >= topLeft.getValue() && y < bottomRight.getValue()
        /* && this.image != null */
        /* && (this.x_shift > 0 || this.y_shift > 0)*/) {
      return true;
    } else {
      return false;
    }
  }
  
  public String toString(DefaultMutableTreeNode parent) {
    String output = "/" + this.name + " "
        + this.getTopLeftAsString() + " "
        + this.getBottomRightAsString() + " ";
        // + this.content;
    while (parent != null) {
      output = "/" + ((Element) parent.getUserObject()).getName() + output;
      parent = (DefaultMutableTreeNode) parent.getParent();
    }
    
    return output;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public int getIndex()
  {
    return this.index;
  }
  
  public String getContent()
  {
    return this.content;
  }
  
  public void setContent(String content)
  {
    this.content = content;
  }
  
  public Pair<Integer, Integer> getTopLeft()
  {
    return this.topLeft;
  }

  public void setTopLeft(Pair<Integer, Integer> topLeft) {
    this.topLeft = topLeft;
  }
  
  public void setTopLeftFromString(String coordsTopLeft)
  {
    String[] coordsTopLeftXY = coordsTopLeft.split(",", 2);
    
    if (coordsTopLeftXY.length != 2) {
      throw new IllegalArgumentException("Incomplete top left coordinates");
    }
    
    this.topLeft = new Pair<Integer, Integer>(
                        Integer.parseInt(coordsTopLeftXY[0]),
                        Integer.parseInt(coordsTopLeftXY[1]));
  }
  
  public String getTopLeftAsString() {
    return this.topLeft.getKey() + "," + this.topLeft.getValue();
  }
  
  public Pair<Integer, Integer> getBottomRight()
  {
    return this.bottomRight;
  }

  public void setBottomRight(Pair<Integer, Integer> bottomRight) {
    this.bottomRight = bottomRight;
  }
  
  public void setBottomRightFromString(String coordsBottomRight)
  {
    String[] coordsBottomRightXY = coordsBottomRight.split(",", 2);
    
    if (coordsBottomRightXY.length != 2) {
      throw new IllegalArgumentException("Incomplete top left coordinates");
    }
    
    this.topLeft = new Pair<Integer, Integer>(
                        Integer.parseInt(coordsBottomRightXY[0]),
                        Integer.parseInt(coordsBottomRightXY[1]));
  }
  
  public String getBottomRightAsString() {
    return this.bottomRight.getKey() + "," + this.bottomRight.getValue();
  }

  public int getDepth()
  {
    return depth;
  }

  public BufferedImage getImage()
  {
    return image;
  }

  public void setImage(BufferedImage image)
  {
    this.image = image;
  }

  public void setDepth(int depth)
  {
    this.depth = depth;
  }

  public int getArea() {
    return (this.bottomRight.getKey() - this.topLeft.getKey())
            * (this.bottomRight.getValue() - this.topLeft.getValue());
  }

  /**
   * @param arg0 other {@link Element}
   * @return -1: if this above and/or left of and/or smaller than other
   *          0: if this has exactly the same position and size as other
   *          1: if this is below and/or right of and/or larger than other
   */
  @Override
  public int compareTo(Element arg0)
  {
    int ret = 0;
    
    // this above arg0
    if (this.topLeft.getValue() < arg0.topLeft.getValue()) {
      ret = -1;
    }
    // this and arg0 at same y
    else if (this.topLeft.getValue() == arg0.topLeft.getValue()) {
      // this left of arg0
      if (this.topLeft.getKey() < arg0.topLeft.getKey()) {
        ret = -1;
      }
      // this and arg0 at same x
      else if (this.topLeft.getKey() == arg0.topLeft.getKey()) {
        // this less tall than arg0
        if (this.bottomRight.getValue() < arg0.topLeft.getValue()) {
          ret = -1;
        }
        // this as tall as arg0
        else if (this.bottomRight.getValue() == arg0.bottomRight.getValue()) {
          // this as tall and less wide than arg0
          if (this.bottomRight.getKey() < arg0.bottomRight.getKey()) {
            ret = -1;
          }
          // this as tall and as wide as arg0
          else if (this.bottomRight.getKey() == arg0.bottomRight.getKey()) {
            ret = 0;
          }
          // this as tall but wider than arg0
          else if (this.bottomRight.getKey() > arg0.bottomRight.getKey()) {
            ret = 1;
          }  
        }
        // this taller than arg0
        else if (this.bottomRight.getValue() > arg0.bottomRight.getValue()) {
          ret = 1;
        }
      } else {
        // this right of arg0
        ret = 1;
      }
    }
    // this below arg0
    else if (this.topLeft.getValue() > arg0.topLeft.getValue()) {
      ret = 1;
    }
    
    return ret;
  }
}
