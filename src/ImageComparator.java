package de.webis.webarchive;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.lang.Math.abs;

public class ImageComparator{
    public static void main(String args[]){
        boolean drawMissingElement = false;
        BufferedImage imgOriginal = null;
        BufferedImage imgArchived = null;
        BufferedImage heatMap;
        File original;
        File archived;
        long paddedRows;
        long substitutionPenalty=0;
        int red = (255<<24) | (255<<16) | (0<<8) | 0; // paint padding red for deleted pixels in archived
        int blue = (255<<24) | (0<<16) | (0<<8) | 255; // paint padding blue for inserted pixels in archived

        //read images
        try {
            original = new File(args[0]);
            archived = new File(args[1]);
            imgOriginal = ImageIO.read(original);
            imgArchived = ImageIO.read(archived);
        } catch (IOException e) {
            System.out.println(e);
        }

        // if option to call drawMissingElement is passed
        if(args.length == 3) {
            drawMissingElement = Boolean.parseBoolean(args[2]);
        }

        // if inserted == 0 both are equally sized, >0 original is bigger, <0 archived is bigger
        // difference would be the numbers of lines added/deleted
        // if archived is bigger than original we pad deleted pixels red else blue
        int inserted = imgOriginal.getHeight() - imgArchived.getHeight();
        paddedRows = abs(inserted);

        // archived is smaller than original
        if(inserted > 0)
        {
            heatMap = new BufferedImage(imgOriginal.getWidth(), imgOriginal.getHeight(), BufferedImage.TYPE_INT_ARGB);
            pad(heatMap, paddedRows, blue);
        }
        // archived is bigger than original
        else if(inserted<0)
        {
            heatMap = new BufferedImage(imgArchived.getWidth(), imgArchived.getHeight(), BufferedImage.TYPE_INT_ARGB);
            pad(heatMap, paddedRows, red);
        }
        // archived and original are equally sized
        else
        {
            heatMap = new BufferedImage(imgArchived.getWidth(), imgArchived.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        if(drawMissingElement)
        {
            // hand over equally sized heatmaps without transparent padding
            BufferedImage originalHeatMap = new BufferedImage(heatMap.getWidth(), heatMap.getHeight(), BufferedImage.TYPE_INT_ARGB);
            BufferedImage archivedHeatMap = new BufferedImage(heatMap.getWidth(), heatMap.getHeight(), BufferedImage.TYPE_INT_ARGB);
            drawMissingElement(originalHeatMap, archivedHeatMap, paddedRows, imgOriginal, imgArchived, substitutionPenalty);
        }
        else
        {
            substitute(heatMap, paddedRows, imgOriginal, imgArchived, substitutionPenalty);
        }
        writeHeatMap(heatMap);
    }

    public static void pad(BufferedImage heatMap, long paddedRows, int color)
    {
        for(long y = heatMap.getHeight()-paddedRows+1; y < heatMap.getHeight(); y++)
        {
            for (int x = 0; x < heatMap.getWidth(); x++)
            {
                    heatMap.setRGB(x, (int) y,color); //padding red for inserted or blue for deleted rows
            }
        }
        return;
    }

    // color pixel positions that would be substituted black
    public static void substitute(BufferedImage heatMap, long paddedRows, BufferedImage imgOriginal,
                                  BufferedImage imgArchived, long substitutionPenalty)
    {
        int black = (255 << 24) | (0 << 16) | (0 << 8) | 0; // pixel is black if it was changed in archived
        int white = (255 << 24) | (255 << 16) | (255 << 8) | 255; // white for all unchanged pixels
        int red = (255 << 24) | (255 << 16) | (0 << 8) | 0; // white for all unchanged pixels

        int widthOriginal = imgOriginal.getWidth();
        int heightOriginal = imgOriginal.getHeight();
        int pxTotalOriginal = widthOriginal * heightOriginal;
        for (int y = 0; y < heatMap.getHeight() - paddedRows; y++)
        {
            for (int x = 0; x < widthOriginal; x++)
            {
                //get rgb color values at pixel
                int pOriginal = imgOriginal.getRGB(x, y);
                int pArchived = imgArchived.getRGB(x, y);
                if (pOriginal != pArchived) {
                    Color cOriginal = new Color(pOriginal);
                    Color cArchived = new Color(pArchived);

                    boolean isSimilar = abs(cOriginal.getAlpha() - cArchived.getAlpha()) <= 1;
                    isSimilar = isSimilar && abs(cOriginal.getRed() - cArchived.getRed()) <= 1;
                    isSimilar = isSimilar && abs(cOriginal.getGreen() - cArchived.getGreen()) <= 1;
                    isSimilar = isSimilar && abs(cOriginal.getBlue() - cArchived.getBlue()) <= 1;

                    if(!isSimilar)
                    {
                        substitutionPenalty += 1;
                        heatMap.setRGB(x, y, black);
                    }
                    else
                    {
                        heatMap.setRGB(x, y, white);
                    }
                }
                else
                {
                    heatMap.setRGB(x, y, white);
                }

            }
        }
        long paddingPenalty = paddedRows * heatMap.getWidth();
        long totalPenalty = paddingPenalty+substitutionPenalty;
        System.out.println(paddingPenalty + "," + totalPenalty + "," + pxTotalOriginal + ","
                + (float) totalPenalty*100/pxTotalOriginal + "," + (long) heatMap.getWidth() * heatMap.getHeight()
                + "," + (float) totalPenalty*100/(heatMap.getWidth() * heatMap.getHeight()));
        /*
        System.out.println("pixels added through insertion/deletion = "
                + paddingPenalty);
        System.out.println("total number of changed pixels = "
                + totalPenalty);
        System.out.println("pixels total in original = "
                + pxTotalOriginal);
        System.out.println("changed pixels as percent of pixels in original = "
                + totalPenalty*100/pxTotalOriginal + "%");
        System.out.println("number of pixels in bigger image = "
                + heatMap.getWidth() * heatMap.getHeight());
        System.out.println("changed pixels as percent of pixels in bigger image = "
                + totalPenalty*100/(heatMap.getWidth() * heatMap.getHeight()) + "%");

         */
    }

    //write heatmap
    public static void writeHeatMap(BufferedImage heatMap)
    {
        try{
            File file = new File("heatmap.png");
            ImageIO.write(heatMap, "png", file);
        }catch(IOException e){
            System.out.println(e);
        }
    }

    /*
    * For this method, the archived image handed over as input MUST be the reconstructed version of the raw archived
    * after shift detection has been done and all shifted elements have been moved back to their original position
    * */
    public static void drawMissingElement(BufferedImage originalHeatMap, BufferedImage archivedHeatMap, long paddedRows, BufferedImage imgOriginal,
                                          BufferedImage imgArchived, long substitutionPenalty)
    {
        int white = (255 << 24) | (255 << 16) | (255 << 8) | 255; // white for all unchanged pixels
        int transparent = (0 << 24) | (255 << 16) | (255 << 8) | 255; // white for all unchanged pixels

        int widthOriginal = imgOriginal.getWidth();
        int heightOriginal = imgOriginal.getHeight();
        // paddedrows should always be 0 for reconstructed archived and originial
        // left in to be sure to avoid index out of bounds
        for (int y = 0; y < heightOriginal-paddedRows; y++)
        {
            for (int x = 0; x < widthOriginal; x++)
            {
                //get rgb color values at pixel
                int pOriginal = imgOriginal.getRGB(x, y);
                int pArchived = imgArchived.getRGB(x, y);
                if (pOriginal != pArchived) {
                    substitutionPenalty += 1;
                    originalHeatMap.setRGB(x, y, pOriginal);
                    //archivedHeatMap.setRGB(x,y, pArchived);
                }
                else
                {
                    originalHeatMap.setRGB(x, y, transparent);
                    //archivedHeatMap.setRGB(x,y, transparent);
                }

            }
        }

        try{
            File originalFile = new File("heatmap.png");
            //File archivedFile = new File("archivedHeatmap.png");
            ImageIO.write(originalHeatMap, "png", originalFile);
            //ImageIO.write(archivedHeatMap, "png", archivedFile);
        }catch(IOException e){
            System.out.println(e);
        }

        //long paddingPenalty = paddedRows * heatMap.getWidth();
        //long totalPenalty = paddingPenalty+substitutionPenalty;
        //System.out.println(paddingPenalty + "," + totalPenalty + "," + pxTotalOriginal + ","
        //        + (float) totalPenalty*100/pxTotalOriginal + "," + (long) heatMap.getWidth() * heatMap.getHeight()
        //        + "," + (float) totalPenalty*100/(heatMap.getWidth() * heatMap.getHeight()));

    }
}
