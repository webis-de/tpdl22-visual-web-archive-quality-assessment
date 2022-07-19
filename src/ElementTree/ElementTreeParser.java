package de.webis.webarchive.ElementTree;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.tree.TreeNode;

import de.webis.webarchive.ShiftDetector;
import de.webis.webarchive.MotionVectors.MotionVector;
import de.webis.webarchive.MotionVectors.MotionVectorParser;
import javafx.util.Pair;

public class ElementTreeParser {
  private ElementTreeNode parsedTree;
  private final BufferedReader reader;
  private BufferedImage img;
  private int maxDrawableHeight;
  private final MotionVectorParser mvp;

  /**
   * Parses an {@link ElementTreeNode} from an elements.txt file.
   * 
   * @param filename path to elements.txt
   * @param parseImages whether leaf nodes should be assigned their corresponding subimages
   * @param imageFile path to archived screenshot or BufferedImage containing the archived Screenshot
   * @param parseMvs whether motion vectors should be parsed for each element
   * @param mvFile path to csv or MP4 containing motion vectors
   * @param maxDrawableHeight contains height in px up to which subimages will be drawn
   * @throws IOException when elements.txt cannot be opened
   * @throws IllegalArgumentException when image parsing requested, but no file
   * provided
   */
  public ElementTreeParser(String filename, boolean parseImages,
                           Object imageFile, boolean parseMvs,
                           String mvFile, int maxDrawableHeight)
  throws IOException, IllegalArgumentException
  {
    if (parseImages && imageFile != null) {
      if (imageFile instanceof String)
      {
        this.img = ImageIO.read(new File((String)imageFile));
      }
      else if (imageFile instanceof BufferedImage)
      {
        this.img = (BufferedImage) imageFile;
      }
    } else if (parseImages && imageFile == null) {
      throw new IllegalArgumentException("No image file provided");
    } else {
      this.img = null;
    }

    if (parseMvs && mvFile != null) {
      this.mvp = new MotionVectorParser(mvFile);
    } else if (parseMvs && mvFile == null) {
      throw new IllegalArgumentException("No MV file provided");
    } else {
      this.mvp = null;
    }

    // set threshold for drawing subimages to imageHeight for calls from methods
    // that intent to draw all subimages, set it to max height of drawable plane
    // for method calls that draw images on smaller planes (e.g. image reconstructor
    // if original < archived)
    if(parseImages) {
      this.maxDrawableHeight = maxDrawableHeight < Integer.MAX_VALUE ? maxDrawableHeight : this.img.getHeight();
    }
    this.reader = openFile(filename);
    parsedTree = (ElementTreeNode) parse(this.reader).getRoot();
  }

  public ElementTreeNode getParsedTree()
  {
    return parsedTree;
  }

  private BufferedReader openFile(String filename) throws FileNotFoundException
  {
    return new BufferedReader(new FileReader(filename));
  }

  private ElementTreeNode parse(BufferedReader reader) throws IOException
  {
    String line;
    ElementTreeNode tree = null;

    while ((line = reader.readLine()) != null) {
      String[] pathCoordsContent = line.split(" ", 4);

      if (pathCoordsContent.length != 4) {
        throw new IOError(new Throwable("Malformed elements.txt"));
      }

      String path = pathCoordsContent[0];
      String coordsTopLeft = pathCoordsContent[1];
      String coordsBottomRight = pathCoordsContent[2];

      String[] pathComponents = path.split("/");
      if (pathComponents.length < 2) {
        throw new IOError(new Throwable("Malformed elements.txt"));
      } else if (pathComponents.length == 2) {
        tree = new ElementTreeNode(
            new Element(pathComponents[1], pathCoordsContent[3], coordsTopLeft,
                coordsBottomRight)
            );
        continue;
      }
      
      Element exists = null;
      if (tree != null) {
        try {
          exists = (Element) tree.getElementByPath(path).getUserObject();
        } catch (NullPointerException e) {
          // do nothing for now
        }
      }
      
      if (exists != null)
      {
        if (!exists.getContent().equals(pathCoordsContent[3])) {
          exists.setContent(pathCoordsContent[3]);
        }
        if (!exists.getTopLeftAsString().equals(coordsTopLeft)) {
          exists.setTopLeftFromString(coordsTopLeft);
        }
        if (!exists.getBottomRightAsString().equals(coordsBottomRight)) {
          exists.setBottomRightFromString(coordsBottomRight);
        }
        continue;
      }
      
      for (int i = pathComponents.length - 2; i > 0; i--) {
        String parentPath = "/";
        for (int j = 1; j <= i; j++) {
          parentPath += pathComponents[j] + "/";
        }
        parentPath = parentPath.substring(0, parentPath.length() - 1);
        
        ElementTreeNode parent;
        if ((parent = tree.getElementByPath(parentPath)) != null) {
          String content = "";
          String coordsTopLeftTmp = "0,0";
          String coordsBottomRightTmp = "0,0";
          if (i == pathComponents.length - 2) {
            content = pathCoordsContent[3];
            coordsTopLeftTmp = coordsTopLeft;
            coordsBottomRightTmp = coordsBottomRight;
          }
          
          parent.add(new ElementTreeNode(new Element(
              pathComponents[pathComponents.length - 1], content,
              coordsTopLeftTmp, coordsBottomRightTmp)));
          
          break;
        }
      }
    }
    
    if (this.img != null) {
      sanitizeElementDimensions(tree);
      getSubimages(tree);
    }
    
    if (this.mvp != null) {
      List<MotionVector> mvs = mvp.parse();
      Iterator<TreeNode> it = tree.breadthFirstEnumeration().asIterator();
      while (it.hasNext()) {
        ElementTreeNode etn = (ElementTreeNode) it.next();
        Element e = (Element) etn.getUserObject();
        e.setMvs(mvs.stream().filter(m->e.contains(m.getSrc_x(), m.getSrc_y()))
                             .collect(Collectors.toList()));
        List<MotionVector> containedMajorityMVs = ShiftDetector.majorityDecider(e.getMvs());
        if (containedMajorityMVs != null) {
          int x_distance_avg = containedMajorityMVs.stream().mapToInt
                                           (
                                               m -> m.getDst_x() - m.getSrc_x()
                                           ).sum() / containedMajorityMVs.size();
          int y_distance_avg = containedMajorityMVs.stream().mapToInt
                                           (
                                               m -> m.getDst_y() - m.getSrc_y()
                                           ).sum() / containedMajorityMVs.size();
          
          e.setX_shift(x_distance_avg);
          e.setY_shift(y_distance_avg);
        }
      }
    }
    
    return tree;
  }

  private void assignSubimage(ElementTreeNode tree, Element e) {
    Pair<Integer, Integer> dim = tree.getImageDimensions();
      // do not consider elements that lie outside the image or have width or height of 0
    if(e.getTopLeft().getKey() < 0 || e.getTopLeft().getValue() < 0
    || e.getTopLeft().getKey() > dim.getKey() || e.getTopLeft().getValue() > dim.getValue()
    || e.getBottomRight().getKey()-e.getTopLeft().getKey() == 0
    || e.getBottomRight().getValue()-e.getTopLeft().getValue() == 0)
    {
      return;
    }
    // process elements only up to the height they can be drawn to target image
    // (width should always be the same 1366 px, so no need to fix it)
    if (maxDrawableHeight < e.getBottomRight().getValue() && e.getTopLeft().getValue() < maxDrawableHeight) {
      e.setBottomRight(new Pair<>(e.getBottomRight().getKey(), maxDrawableHeight));
    }
    try {
      BufferedImage subimage = this.img.getSubimage(e.getTopLeft().getKey(),
              e.getTopLeft().getValue(),
              Math.min(e.getBottomRight().getKey(), dim.getKey())
                      - e.getTopLeft().getKey(),
              Math.min(e.getBottomRight().getValue(), dim.getValue())
                      - e.getTopLeft().getValue());
      e.setImage(subimage);
    } catch (RasterFormatException ex) {
      System.err.println("Illegal image area at " + e.getTopLeftAsString()
              + "; size " + (e.getBottomRight().getKey() - e.getTopLeft().getKey())
              + "," + (e.getBottomRight().getValue() - e.getTopLeft().getValue())
              + "; total target site dimensions: " + tree.getImageDimensions().toString()
              + "; exception message: " + ex.getMessage());
    }
  }

  private void getSubimages(ElementTreeNode node) {
    assignSubimage(node, (Element) node.getUserObject());
    if (!node.isLeaf()) {
      Iterator<TreeNode> it = node.children().asIterator();
      while (it.hasNext()) {
        ElementTreeNode ee = (ElementTreeNode) it.next();
        assignSubimage(node, (Element) ee.getUserObject());
        getSubimages(ee);
      }
    }
  }

  private void sanitizeElementDimensions(ElementTreeNode node) {
    Pair<Integer, Integer> dim = node.getImageDimensions();
    Element e = (Element) node.getUserObject();

    Pair<Integer, Integer> newElemTopLeft = new Pair<>(
            e.getTopLeft().getKey() > dim.getKey() ?
                    dim.getKey() :
                    (e.getTopLeft().getKey() < 0 ? 0 : e.getTopLeft().getKey()),
            e.getTopLeft().getValue() > dim.getValue() ?
                    dim.getValue() :
                    (e.getTopLeft().getValue() < 0 ? 0 : e.getTopLeft().getValue())
    );
    Pair<Integer, Integer> newElemBottomRight = new Pair<>(
            e.getBottomRight().getKey() > dim.getKey() ?
                    dim.getKey() :
                    (e.getBottomRight().getKey() < 0 ? 0 : e.getBottomRight().getKey()),
            e.getBottomRight().getValue() > dim.getValue() ?
                    dim.getValue() :
                    (e.getBottomRight().getValue() < 0 ? 0 : e.getBottomRight().getValue())
    );

    e.setTopLeft(newElemTopLeft);
    e.setBottomRight(newElemBottomRight);

    if (!node.isLeaf()) {
      Iterator<TreeNode> it = node.children().asIterator();
      while (it.hasNext()) {
        ElementTreeNode ee = (ElementTreeNode) it.next();
        sanitizeElementDimensions(ee);
      }
    }
  }
}
