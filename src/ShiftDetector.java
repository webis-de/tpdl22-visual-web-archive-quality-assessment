package de.webis.webarchive;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.tree.TreeNode;

import de.webis.webarchive.ElementTree.Element;
import de.webis.webarchive.ElementTree.ElementTreeNode;
import de.webis.webarchive.ElementTree.ElementTreeParser;
import de.webis.webarchive.MotionVectors.MotionVector;
import de.webis.webarchive.MotionVectors.MotionVectorParser;
import javafx.util.Pair;

public class ShiftDetector
{
  /**
   * Function that takes a list of Motion Vectors and returns only the majority
   * that points in the same direction.
   * @param mvs
   * @return a list of Motion Vectors pointing in the same direction, or null
   * if no 2/3 majority of such vectors exists
   */
  public static List<MotionVector> majorityDecider(List<MotionVector> mvs)
  {
    if (mvs.size() >= 5) {
      // Group Motion Vectors by their direction (normalized vectors)
      Map<Pair<BigDecimal, BigDecimal>, List<MotionVector>> mvsByDirection =
          mvs.stream().collect(Collectors.groupingBy(MotionVector::normalize));
      
      // Find 2/3 majority of Motion Vectors pointing in the same direction
      return mvsByDirection.values().stream()
                           .filter(l -> l.size() >= 0.67 * mvs.size())
                           .findFirst().orElse(null);
    } else {
      return null;
    }
  }

  /**
   * Prints element's position in archived and their corresponding position in original image.
   * ( "<position_archived>, <position_original>")
   * @param args [0] elements.txt
   *             [1]: path to CSV or MP4 containing motion vectors (src.x,src.x,dst.x,dst.y)
   * @throws IOException
   */
  public static void main(String[] args) throws IOException
  {    
    ElementTreeParser etp = new ElementTreeParser(args[0], false, null, false, null, Integer.MAX_VALUE);
    
    MotionVectorParser mvp = new MotionVectorParser(args[1]);
    List<MotionVector> mvs = mvp.parse();
        
    List<TreeNode> leaves = etp.getParsedTree().getLeaves();
    
    for (TreeNode n: leaves) {
      ElementTreeNode etn = (ElementTreeNode) n;
      Element e = (Element) etn.getUserObject();
      List<MotionVector> containedMajorityMVs =
          majorityDecider(mvs.stream()
                             .filter(m->e.contains(m.getDst_x(), m.getDst_y()))
                             .collect(Collectors.toList())
                         );
      
      if (containedMajorityMVs != null) {
        int x_distance_avg = containedMajorityMVs.stream().mapToInt
                                         (
                                             m -> m.getDst_x() - m.getSrc_x()
                                         ).sum() / containedMajorityMVs.size();
        int y_distance_avg = containedMajorityMVs.stream().mapToInt
                                         (
                                             m -> m.getDst_y() - m.getSrc_y()
                                         ).sum() / containedMajorityMVs.size();

        // print <archived_position>,<original_position>
        if(x_distance_avg != 0 || y_distance_avg != 0) {
          System.out.println("/" + etn.getPathAsString() + "," + e.getTopLeft().getKey() +
                  "," + e.getTopLeft().getValue() +
                  "," + e.getBottomRight().getKey() +
                  ","+ e.getBottomRight().getValue() + "," +
                  (e.getTopLeft().getKey() - x_distance_avg) +
                  "," + (e.getTopLeft().getValue() - y_distance_avg) +
                  "," + (e.getBottomRight().getKey() - x_distance_avg) +
                  "," + (e.getBottomRight().getValue() - y_distance_avg));
        }
      }
    }
  }

}
