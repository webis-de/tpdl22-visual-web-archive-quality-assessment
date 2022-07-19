package de.webis.webarchive.ElementTree;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import de.webis.webarchive.ElementTree.Element;
import de.webis.webarchive.MotionVectors.MotionVector;
import javafx.util.Pair;

public class ElementTreeNode extends DefaultMutableTreeNode
{
  /**
   * 
   */
  private static final long serialVersionUID = 7966491464025753145L;
  
  public ElementTreeNode() {
    super();
  }
  
  public ElementTreeNode(Element e) {
    super((Object) e);
  }

  public ElementTreeNode getElementByPath(String path)
  {
    String[] pathComponents = path.split("/");
    if (pathComponents.length < 2) {
      throw new IllegalArgumentException("Invalid path");
    }
    
    ElementTreeNode currentNode = (ElementTreeNode) this.getRoot();
    if (pathComponents.length == 2) {
      if (currentNode.getName().equals(pathComponents[1])) {
        return currentNode;
      } else {
        return null;
      }
    }
    
    int i = 2;
    while ((i < pathComponents.length) 
        && ((currentNode = currentNode.getChildByName(pathComponents[i]))
            != null)) {
      i++;
    }
    
    return currentNode;
  }
  
  /**
   * Returns the dimensions of this tree's visual representation.
   * @return bottom right coordinate of tree root
   */
  public Pair<Integer, Integer> getImageDimensions()
  {
    Element root = (Element) ((ElementTreeNode) this.getRoot()).getUserObject();
    
    int width = (root.getBottomRight().getKey() <= 1366)
                  ? root.getBottomRight().getKey() : 1366;
    int height = (root.getBottomRight().getValue() <= 16384)
                  ? root.getBottomRight().getValue() : 16384;
    
    return new Pair<Integer, Integer>(width, height);
  }
  
  /**
   * @return Vector of paths of the tree with the called node as root
   */
  public Vector<String> treeToString() {
    Vector<String> output = new Vector<String>();
    
    output.add(this.toString());
    Iterator<TreeNode> it = this.children().asIterator();
    while (it.hasNext()) {
      ElementTreeNode e = (ElementTreeNode) it.next();
      output.addAll(e.treeToString());
    }
    
    return output;
  }

  @Override
  public String toString() {
    return ((Element) this.userObject).toString((DefaultMutableTreeNode)
                                                this.parent);
  }
  
  public String getName() {
    return ((Element) this.userObject).getName();
  }
  
  public ElementTreeNode getChildByName(String name) {
    Iterator<TreeNode> it = this.children().asIterator();
    while (it.hasNext()) {
      ElementTreeNode e = (ElementTreeNode) it.next();
      if (e.getName().equals(name)) {
        return e;
      }
    }
    
    return null;
  }
  
  public List<TreeNode> getLeaves() {
    // https://stackoverflow.com/a/24511534
    Iterator<TreeNode> sourceIterator = this.depthFirstEnumeration().asIterator();
    Iterable<TreeNode> iterable = () -> sourceIterator;
    Stream<TreeNode> stream = StreamSupport.stream(iterable.spliterator(), false);
    
    return stream.filter(node -> node.isLeaf()).collect(Collectors.toList());
  }
  
  public String getPathAsString() {
    ElementTreeNode parent = (ElementTreeNode) this.getParent();
    String path = this.getName();
    
    while (parent != null) {
      path = parent.getName() + "/" + path;
      parent = (ElementTreeNode) parent.getParent();
    }
    
    return path;
  }

  public ArrayList<Element> findAll()
  {
    ArrayList<Element> elements = new ArrayList<>();
    Iterator<TreeNode> sourceIterator = this.depthFirstEnumeration().asIterator();

    while (sourceIterator.hasNext()) {
      ElementTreeNode etn = (ElementTreeNode) sourceIterator.next();
      Element e = (Element) etn.getUserObject();
      e.setDepth(etn.getLevel());
      elements.add(e);
    }
    return elements;
  }
  
  public Element findDeepestAt(int x, int y) {
    Iterator<TreeNode> sourceIterator = this.depthFirstEnumeration().asIterator();
    int treeDepth = this.getDepth();
    int maxDepth = 0;
    Element deepest = null;
    
    while (sourceIterator.hasNext()) {
      if ((maxDepth == treeDepth) && (deepest != null)) {
        deepest.setDepth(maxDepth);
        return deepest;
      }
      
      ElementTreeNode etn = (ElementTreeNode) sourceIterator.next();
      Element e = (Element) etn.getUserObject();
      if (e.contains(x, y)) {
        if (etn.getLevel() >= maxDepth) {
          maxDepth = etn.getLevel();
          deepest = e;
        }
      }
    }
    deepest.setDepth(maxDepth);
    return deepest;
  }

  public Element findSmallestAt(int x, int y) {
    Iterator<TreeNode> sourceIterator = this.breadthFirstEnumeration().asIterator();
    Element smallest = null;

    while (sourceIterator.hasNext()) {
      ElementTreeNode etn = (ElementTreeNode) sourceIterator.next();
      Element e = (Element) etn.getUserObject();

      if (e.contains(x, y)) {
        smallest = smallest == null ? e : (e.getArea() < smallest.getArea() ? e : smallest);
      }
    }

    return smallest;
  }
  
  /**
   * Finds the first element with unidirectional motion vectors in the tree
   * containing the given coordinates.
   * 
   * @param x coordinate
   * @param y coordinate
   * @return Element if found, null otherwise
   */
  public Element findFirstAt(int x, int y) {
    Iterator<TreeNode> it = this.breadthFirstEnumeration().asIterator();
    
    while (it.hasNext()) {
      ElementTreeNode etn = (ElementTreeNode) it.next();
      Element e = (Element) etn.getUserObject();
      
      List<MotionVector> mvs = e.getMvs();
      long num_zero_vecs = mvs.stream().filter(mv -> mv.getLength() < 1).count();
      
      // if (num_zero_vecs == mvs.size())
    }
    
    return null;
  }
}