package de.webis.webarchive;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import de.webis.webarchive.ElementTree.Element;
import de.webis.webarchive.ElementTree.ElementTreeNode;
import de.webis.webarchive.ElementTree.ElementTreeParser;
import javafx.util.Pair;

// TODO: Top-down, suitable threshold: <10% 0-Vectors, <10% variance in X and Y
public class ImageReconstructor
{
  /**
   * @param args args[0]: elements.txt
   *             args[1]: original screenshot : needed for width and height of output
   *             args[2]: path to CSV or MP4 containing motion vectors (src.x,src.x,dst.x,dst.y)
   *             args[3]: archived screenshot : needed for images of elements
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public static void main(String[] args)
  throws IllegalArgumentException, IOException
  {
    // will contain shifted elements sorted by their depth in tree
    Map<Integer, ArrayList> elementsByDepth = new TreeMap<>();

    BufferedImage originalImage = ImageIO.read(new File(args[1]));
    BufferedImage out = new BufferedImage(originalImage.getWidth(),
                                          originalImage.getHeight(),
                                          BufferedImage.TYPE_INT_ARGB);
    ElementTreeParser etp = null;
    ElementTreeNode tree = null;

    BufferedImage archivedImage = ImageIO.read(new File(args[3]));
    BufferedImage resizedArchived = null;
    if(archivedImage.getHeight() < originalImage.getHeight())
    {
      /*BufferedImage*/ resizedArchived = new BufferedImage(originalImage.getWidth(),
              originalImage.getHeight(),
              BufferedImage.TYPE_INT_ARGB);
      WritableRaster raster = archivedImage.getRaster();
      resizedArchived.setData(raster);
      // etp wird aus dem vergrößerten archived screenshot konstruiert (vergrößerung durch anfügen von pixeln, kein scale)
      etp = new ElementTreeParser(args[0], true, resizedArchived,  true, args[2], Integer.MAX_VALUE);

      // use archived image as background, because: "what did not cause an error
      // in raw archived, should not be causing one in reconstructed image"
      WritableRaster outRaster = out.getRaster();
      outRaster.setRect(resizedArchived.getRaster());
      out.setData(outRaster);
    }
    else
    {
      // etp wird aus dem normalen archived screenshot konstruiert
      etp = new ElementTreeParser(args[0], true, args[3], true, args[2], originalImage.getHeight());

      // archived as background image. if archived > original, background will contain archived cut at last line of originals height
      WritableRaster outRaster = out.getRaster();
      outRaster.setRect(archivedImage.getRaster());
      out.setData(outRaster);
    }
    tree = etp.getParsedTree();

    ArrayList<Element> elements = tree.findAll();
    for(Element e : elements)
    {
      if(e.isShifted()) {
        if (!elementsByDepth.containsKey(e.getDepth())) {
          elementsByDepth.put(e.getDepth(), new ArrayList());
        }
        elementsByDepth.get(e.getDepth()).add(e);
      }
    }

    // IDEA 1: draw only shifted elements in front of background of raw archived
    //writeImageWithDuplicates(elementsByDepth, out);

    // IDEA 2: draw only shifted elements in front of background of raw archived
    // but cut out areas where it was placed before shift to avoid duplicates
    int color = computeCommonColorArchived(archivedImage);

    /*option==0 for most common color in archived image*/
    //BufferedImage archived = resizedArchived != null ? resizedArchived : archivedImage;
    //writeImageCutDuplicates(elementsByDepth, color, out, archived, 0);

    /*option==1 for area of original image of cut element (upper bound)*/
    //writeImageCutDuplicates(elementsByDepth, color, out, originalImage, 1);

    /*option==2 for most common color of pixels bordering gap in archived image*/
    //BufferedImage archived = resizedArchived != null ? resizedArchived : archivedImage;
    //writeImageCutDuplicates(elementsByDepth, color, out, archived, 2);

    /*option==3 for area of original image of cut element (lower bound)*/
    writeImageCutDuplicates(elementsByDepth, color, out, originalImage, 3);

  }

  // IDEA 1
  public static void writeImageWithDuplicates(Map<Integer, ArrayList> elementsByDepth, BufferedImage out) throws IOException {
    for(ArrayList list : elementsByDepth.values())
    {
      for(Object e : list)
      {
        Rectangle[] outAreas = writeElement((Element) e, out);
      }
    }
    ImageIO.write(out, "png", new File("out_original_sized.png"));
  }

  // IDEA 2
  public static void writeImageCutDuplicates(Map<Integer, ArrayList> elementsByDepth, int color, BufferedImage out, BufferedImage image, int option) throws IOException {
    Map<Integer, ArrayList> gapsByDepth = new TreeMap<>();
    Map<Integer, ArrayList> rastersByDepth = new TreeMap<>();
    for(ArrayList list : elementsByDepth.values())
    {
      for(Object e : list)
      {
        Raster[] outAreas = computeGapsAndRasters((Element) e, color, image, option);
        if(!gapsByDepth.containsKey(((Element) e).getDepth()))
        {
          gapsByDepth.put(((Element) e).getDepth(), new ArrayList());
        }
        gapsByDepth.get(((Element) e).getDepth()).add(outAreas[1]);
        if(!rastersByDepth.containsKey(((Element) e).getDepth()))
        {
          rastersByDepth.put(((Element) e).getDepth(), new ArrayList());
        }
        rastersByDepth.get(((Element) e).getDepth()).add(outAreas[0]);
      }
    }

    // write all gaps deepest last to output image
    for(ArrayList list : gapsByDepth.values())
    {
      for(Object gap : list)
      {
        if (gap != null)
        {
          out.setData((Raster)gap);
        }
      }
    }

    // write all rasters (= e.getImage()) to their shifted positions deepest last
    for(ArrayList list : rastersByDepth.values())
    {
      for(Object raster : list)
      {
        if(raster != null)
        {
          out.setData((Raster) raster);
        }
      }
    }
    ImageIO.write(out, "png", new File("out_original_sized.png"));
  }

  public static Raster[] computeGapsAndRasters(Element e, int color, BufferedImage image, int option)
  {
    Raster[] areas = new Raster[2];

    int x_distance_avg = e.getX_shift();
    int y_distance_avg = e.getY_shift();
    // just for safety.
    if(e.getImage() == null)
    {
      return areas;
    }

    int[] colors = new int[e.getImage().getWidth()*e.getImage().getHeight()];
    DataBufferInt dataBuffer = new DataBufferInt(colors, colors.length);
    int[] bandMasks = {0xFF0000, 0xFF00, 0xFF, 0xFF000000}; // ARGB order
    WritableRaster gap = Raster.createPackedRaster(dataBuffer, e.getImage().getWidth(),
            e.getImage().getHeight(), e.getImage().getWidth(), bandMasks,
            new Point(e.getTopLeft().getKey(),e.getTopLeft().getValue()));

    switch(option){
      case 0:
        Arrays.fill(colors, color);
        break;
      case 1:
        int[] pixels = computeColorsAtGapInOriginal(image, e);
        colors = pixels;
        break;
      case 2:
        int clr = computeColorsAroundGapInArchived(image, e);
        Arrays.fill(colors, clr);
        break;
      case 3:
        int[] offsetPixels = computeColorsAtGapInOriginalWithOffset(image, e);
        colors = offsetPixels;
        break;
      // case compute white?
      // case most common color of pixels in direction of shift? -> find reason why
    }


    gap.setDataElements(gap.getMinX(), gap.getMinY(),gap.getWidth(), gap.getHeight(), colors);

    WritableRaster raster = e.getImage().getRaster()
            .createWritableTranslatedChild
                    (
                            e.getTopLeft().getKey() - x_distance_avg,
                            e.getTopLeft().getValue() - y_distance_avg
                    );

    areas[0] = raster;
    areas[1] = gap;

    return areas;
  }
  
  public static Rectangle[] writeElement(Element e, BufferedImage img)
  {
    Rectangle[] areas = new Rectangle[1];

    int x_distance_avg = e.getX_shift();
    int y_distance_avg = e.getY_shift();
    // just for safety.
    if(e.getImage() == null)
    {
      return areas;
    }
    WritableRaster raster = e.getImage().getRaster()
                            .createWritableTranslatedChild
                            (
                                e.getTopLeft().getKey() - x_distance_avg,
                                e.getTopLeft().getValue() - y_distance_avg
                            );
    img.setData(raster);
    areas[0] = raster.getBounds();

    return areas;
  }

  public static int computeCommonColorArchived(BufferedImage archivedImage)
  {
    // compute most common color of archived image
    Map<Integer, Integer> colorCounts = new HashMap<>();
    for (int y = 0; y < archivedImage.getHeight(); y++)
    {
      for (int x = 0; x < archivedImage.getWidth(); x++)
      {
        //get rgb color values at pixel
        int colorArchived = archivedImage.getRGB(x,y);
        if(!colorCounts.containsKey(colorArchived))
        {
          colorCounts.put(colorArchived, 0);
        }
        colorCounts.put(colorArchived, colorCounts.get(colorArchived)+1);
      }
    }
    Map<Integer,Integer> sortedByCounts = sortByValue(colorCounts);
    int clr = sortedByCounts.keySet().iterator().next();
    return clr;
  }
  // compute original image at gap (upper bound)
  public static int[] computeColorsAtGapInOriginal(BufferedImage originalImage, Element e)
  {
    int[] pixels = new int[e.getImage().getWidth()*e.getImage().getHeight()];
    pixels = originalImage.getRGB(e.getTopLeft().getKey(),e.getTopLeft().getValue(),e.getImage().getWidth(),e.getImage().getHeight(),pixels,0,e.getImage().getWidth());
    return pixels;
  }

  // compute original image at gap (lower bound)
  public static int[] computeColorsAtGapInOriginalWithOffset(BufferedImage originalImage, Element e)
  {
    int[] pixels = new int[e.getImage().getWidth()*e.getImage().getHeight()];
    pixels = originalImage.getRGB(e.getTopLeft().getKey(),e.getTopLeft().getValue(),e.getImage().getWidth(),e.getImage().getHeight(),pixels,0,e.getImage().getWidth());
    for(int i = 0; i < pixels.length; i++)
    {
      int pink = (255<<24) | (200<<16) | (0<<8) | 100;
      int otherPink = (255<<24) | (50<<16) | (0<<8) | 100;
      pixels[i] = pixels[i] != pink ? pink : otherPink;

      /*int alpha = (pixels[i] >> 24) & 0xff;
      alpha = alpha != 255 ? alpha + 1 : alpha-1;

      int red = (pixels[i] >> 16) & 0xff;
      red = red != 255 ? red + 1 : red-1;

      int green = (pixels[i] >> 8) & 0xff;
      green = green != 255 ? green + 1 : green-1;

      int blue = pixels[i] & 0xff;
      blue = blue != 255 ? blue + 1 : blue-1;

      int newColor = (alpha<<24) | (red<<16) | (green<<8) | blue;
      if(pixels[i] == newColor)
      {
        System.out.println("WARNING: INT COLOR DID NOT CHANGE");
      }
      pixels[i] = newColor;
      */
    }
    return pixels;
  }

  // most common color of pixels around gap
  // input resized archived image or out of bounds for archived < original
  public static Integer computeColorsAroundGapInArchived(BufferedImage archivedImage, Element e)
  {
    // if gap == border of image, sample border pixels instead of around e.getImage
    // because else samples would lie ouside of the image
    int topx = Math.max(0, e.getTopLeft().getKey()-1);
    int topy = Math.max(0, e.getTopLeft().getValue()-1);
    int btmx = Math.min(archivedImage.getWidth()-1, e.getBottomRight().getKey()+1);
    int btmy = Math.min(archivedImage.getHeight()-1, e.getBottomRight().getValue()+1);

    Pair<Integer,Integer> outlineTopLeft = new Pair(topx, topy);
    Pair<Integer,Integer> outlineBottomRight = new Pair(btmx,btmy);
    Map<Integer, Integer> colorCounts = new HashMap<>();

    for(int vertical = outlineTopLeft.getValue(); vertical <= outlineBottomRight.getValue(); vertical++)
    {
      int pixelColorLeft = archivedImage.getRGB(outlineTopLeft.getKey(),vertical);
      int pixelColorRight = archivedImage.getRGB(outlineBottomRight.getKey(),vertical);
      if(!colorCounts.containsKey(pixelColorLeft))
      {
        colorCounts.put(pixelColorLeft, 0);
      }
      colorCounts.put(pixelColorLeft, colorCounts.get(pixelColorLeft)+1);
      if(!colorCounts.containsKey(pixelColorRight))
      {
        colorCounts.put(pixelColorRight, 0);
      }
      colorCounts.put(pixelColorRight, colorCounts.get(pixelColorRight)+1);
    }
    // +1 and < to not sample corner pixels double
    for(int horizontal = outlineTopLeft.getKey()+1; horizontal < outlineBottomRight.getKey(); horizontal++)
    {
      int pixelColorTop = archivedImage.getRGB(horizontal, outlineTopLeft.getValue());
      int pixelColorBottom = archivedImage.getRGB(horizontal, outlineBottomRight.getValue());
      if(!colorCounts.containsKey(pixelColorTop))
      {
        colorCounts.put(pixelColorTop, 0);
      }
      colorCounts.put(pixelColorTop, colorCounts.get(pixelColorTop)+1);
      if(!colorCounts.containsKey(pixelColorBottom))
      {
        colorCounts.put(pixelColorBottom, 0);
      }
      colorCounts.put(pixelColorBottom, colorCounts.get(pixelColorBottom)+1);
    }

    Map<Integer,Integer> sortedByCounts = sortByValue(colorCounts);
    int clr = sortedByCounts.keySet().iterator().next();
    return clr;
  }

  public static Map<Integer,Integer> sortByValue(Map<Integer,Integer> keyValue)
  {
    return keyValue
            .entrySet()
            .stream()
            .sorted((Map.Entry.<Integer, Integer>comparingByValue().reversed()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}